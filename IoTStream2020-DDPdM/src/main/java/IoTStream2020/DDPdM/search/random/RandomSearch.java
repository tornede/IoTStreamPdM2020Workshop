package IoTStream2020.DDPdM.search.random;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import IoTStream2020.DDPdM.experiment.ClassifierEvaluationEventListener;
import IoTStream2020.DDPdM.search.ASearch;
import IoTStream2020.DDPdM.search.SearchConfiguration;
import IoTStream2020.DDPdM.search.SearchConfigurationWithAnaconda;
import IoTStream2020.DDPdM.search.SearchResult;
import ai.libs.hasco.exceptions.ComponentInstantiationFailedException;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.jaicore.db.IDatabaseAdapter;
import ai.libs.jaicore.ml.core.evaluation.evaluator.MonteCarloCrossValidationEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.MonteCarloCrossValidationEvaluatorFactory;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.mlplan.multiclass.sklearn.SKLearnClassifierFactory;

public class RandomSearch extends ASearch {

	private final int numberOfCrossValidations = 5;

	public RandomSearch(final SearchConfiguration searchConfiguration, final IDatabaseAdapter adapter, final String subtaskId) throws SQLException {
		super(searchConfiguration, new ClassifierEvaluationEventListener(adapter, searchConfiguration.getExperimentId(), subtaskId));
	}

	@Override
	public SearchResult call() throws Exception {
		long start = System.currentTimeMillis();

		SKLearnClassifierFactory<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> factory = new SKLearnClassifierFactory<>();
		factory.setProblemType(this.getConfiguration().getProblemType().getBasicProblemType());
		factory.setSeed(this.getConfiguration().getSeed());
		factory.setTimeout(this.getConfiguration().getMaxCandidateEvaluationTime());
		if (this.getConfiguration() instanceof SearchConfigurationWithAnaconda) {
			SearchConfigurationWithAnaconda searchConfigWithAnaconda = (SearchConfigurationWithAnaconda) this.getConfiguration();
			factory.setAnacondaEnvironment(searchConfigWithAnaconda.getAnacondaEnvironment());
			factory.setPathVariable(searchConfigWithAnaconda.getPathVariable());
		}

		Random random = new Random(this.getConfiguration().getSeed());
		MonteCarloCrossValidationEvaluator evaluator = new MonteCarloCrossValidationEvaluatorFactory().withData(this.getConfiguration().getTrainingData()).withNumMCIterations(this.numberOfCrossValidations).withTrainFoldSize(0.7)
				.withMeasure(this.getConfiguration().getPerformanceMeasure()).withRandom(random).withCacheSplitSets(true).getLearnerEvaluator();

		SearchResult bestResult = this.execute(factory, evaluator, random);

		long trainTime = (int) ((System.currentTimeMillis() - start) / 1000.0);
		LOGGER.info("Finished build of the model.");
		LOGGER.info("Training time was {}s.", trainTime);
		LOGGER.info("Chosen model is: {}", bestResult.getModel());

		return bestResult;
	}

	private SearchResult execute(final SKLearnClassifierFactory<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> factory, final MonteCarloCrossValidationEvaluator evaluator, final Random random)
			throws ObjectEvaluationFailedException, InterruptedException, ComponentInstantiationFailedException, IOException {
		LOGGER.info("Start RandomSearch");
		long start = System.currentTimeMillis();
		List<ComponentInstance> allComponentInstances = new ArrayList<>(
				ComponentUtil.getAllAlgorithmSelectionInstances(this.getConfiguration().getProblemType().getRequestedInterface(), new ComponentLoader(new File(this.getConfiguration().getSearchSpaceConfigFile())).getComponents()));

		SearchResult bestResult = new SearchResult();
		LOGGER.info("Starting {} parallel processes", this.getConfiguration().getNumCpus() - 2);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.getConfiguration().getNumCpus() - 2);
		while ((System.currentTimeMillis() - start) < (this.getConfiguration().getMaxSearchTime().milliseconds())) {
			if (executor.getQueue().size() < this.getConfiguration().getNumCpus()) {
				RandomComponentEvaluator componentEvaluator = new RandomComponentEvaluator(allComponentInstances, factory, evaluator, bestResult, random, this.getConfiguration().getMaxCandidateEvaluationTime().milliseconds());
				componentEvaluator.registerListener(this.getEventListener());
				executor.execute(componentEvaluator);
			}
		}

		LOGGER.info("End RandomSearch. Maximum threads inside pool: " + executor.getMaximumPoolSize());
		executor.shutdownNow();

		return bestResult;
	}

}
