package IoTStream2020.DDPdM.search.bestfirst;

import java.io.File;
import java.util.Arrays;

import IoTStream2020.DDPdM.experiment.ClassifierEvaluationEventListener;
import IoTStream2020.DDPdM.search.ASearch;
import IoTStream2020.DDPdM.search.SearchConfiguration;
import IoTStream2020.DDPdM.search.SearchConfigurationWithAnaconda;
import IoTStream2020.DDPdM.search.SearchResult;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.MonteCarloCrossValidationEvaluatorFactory;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.jaicore.ml.scikitwrapper.ScikitLearnWrapper;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.core.TasksAlreadyResolvedPathEvaluator;
import ai.libs.mlplan.multiclass.sklearn.MLPlanSKLearnBuilder;

public class BestFirstSearch extends ASearch {

	public BestFirstSearch(final SearchConfiguration searchConfiguration, final ClassifierEvaluationEventListener eventListener) {
		super(searchConfiguration, eventListener);
	}

	@Override
	public SearchResult call() throws Exception {
		long start = System.currentTimeMillis();

		/* initialize mlplan with a tiny search space, and let it run for 30 seconds */
		MLPlanSKLearnBuilder<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> builder = new MLPlanSKLearnBuilder<>(true);
		builder.withProblemType(this.getConfiguration().getProblemType());
		builder.withNumCpus(this.getConfiguration().getNumCpus());
		builder.withSearchSpaceConfigFile(new File(this.getConfiguration().getSearchSpaceConfigFile()));
		builder.withOnePreferredNodeEvaluator(new TasksAlreadyResolvedPathEvaluator(Arrays.asList("AbstractRegressor", "BasicRegressor")));
		if (this.getConfiguration() instanceof SearchConfigurationWithAnaconda) {
			SearchConfigurationWithAnaconda searchConfigWithAnaconda = (SearchConfigurationWithAnaconda) this.getConfiguration();
			builder.withAnacondaEnvironment(searchConfigWithAnaconda.getAnacondaEnvironment());
			builder.withPathVariable(searchConfigWithAnaconda.getPathVariable());
		}

		((MonteCarloCrossValidationEvaluatorFactory) builder.getLearnerEvaluationFactoryForSearchPhase()).withMeasure(this.getConfiguration().getPerformanceMeasure()).withNumMCIterations(5).withCacheSplitSets(true);
		((MonteCarloCrossValidationEvaluatorFactory) builder.getLearnerEvaluationFactoryForSelectionPhase()).withMeasure(this.getConfiguration().getPerformanceMeasure()).withNumMCIterations(5);

		builder.withTimeOut(this.getConfiguration().getMaxSearchTime());
		builder.withNodeEvaluationTimeOut(this.getConfiguration().getMaxNodeEvaluationTime());
		builder.withCandidateEvaluationTimeOut(this.getConfiguration().getMaxCandidateEvaluationTime());
		builder.withSeed(this.getConfiguration().getSeed());

		MLPlan<ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch>> mlplan = builder.withDataset(this.getConfiguration().getTrainingData()).build();
		mlplan.setPortionOfDataForPhase2(0f);
		mlplan.setLoggerName("mlplan");
		mlplan.registerListener(this.getEventListener());

		SearchResult searchResult = new SearchResult();
		ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> optimizedRegressor = null;
		try {
			start = System.currentTimeMillis();
			optimizedRegressor = mlplan.call();
			searchResult.update(optimizedRegressor, mlplan.getInternalValidationErrorOfSelectedClassifier());

			long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
			LOGGER.info("Finished build of the classifier.");
			LOGGER.info("Training time was {}s.", trainTime);
			LOGGER.info("Chosen model is: {}", (searchResult.getModel()));

		} catch (Exception e) {
			LOGGER.error("Building the classifier failed for pipeline {}", optimizedRegressor, e);
		}

		return searchResult;

	}

}
