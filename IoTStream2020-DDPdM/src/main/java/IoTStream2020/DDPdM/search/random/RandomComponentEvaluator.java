package IoTStream2020.DDPdM.search.random;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import IoTStream2020.DDPdM.search.SearchResult;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.jaicore.ml.core.evaluation.evaluator.MonteCarloCrossValidationEvaluator;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.jaicore.ml.scikitwrapper.ScikitLearnWrapper;
import ai.libs.jaicore.timing.TimedComputation;
import ai.libs.mlplan.core.events.ClassifierFoundEvent;
import ai.libs.mlplan.multiclass.sklearn.SKLearnClassifierFactory;

public class RandomComponentEvaluator implements Runnable, Callable<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger("mlplan");

	private final EventBus eventBus;
	private final List<ComponentInstance> allComponentInstances;
	private final SKLearnClassifierFactory<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> factory;
	private final MonteCarloCrossValidationEvaluator evaluator;
	private final SearchResult bestResult;
	private final Random random;
	private final long timeout;

	private ComponentInstance randomComponentInstance;
	private ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> randomModel;

	public RandomComponentEvaluator(final List<ComponentInstance> allComponentInstances, final SKLearnClassifierFactory<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> factory,
			final MonteCarloCrossValidationEvaluator evaluator, final SearchResult bestResult, final Random random, final long timeout) {
		super();
		this.allComponentInstances = allComponentInstances;
		this.factory = factory;
		this.evaluator = evaluator;
		this.bestResult = bestResult;
		this.random = random;
		this.timeout = timeout;
		this.eventBus = new EventBus();

	}

	@Override
	public void run() {
		this.randomComponentInstance = sampleRandomComponent(this.allComponentInstances, this.random);
		try {
			this.randomModel = this.factory.getComponentInstantiation(this.randomComponentInstance);
			long start = System.currentTimeMillis();
			Double randomComponentInstanceError = (Double) TimedComputation.compute(this, this.timeout, "RandomComponentEvaluator interrupted");
			this.eventBus.post(new ClassifierFoundEvent(null, this.randomComponentInstance, this.randomModel, randomComponentInstanceError, (int) (System.currentTimeMillis() - start)));
			this.bestResult.update(this.randomModel, randomComponentInstanceError);
		} catch (Exception e) {
			LOGGER.error("An error occured during evaluation: {}", ExceptionUtils.getStackTrace(e));

		}
	}

	@Override
	public Object call() throws Exception {
		LOGGER.info("Evaluating pipeline: {}", this.randomModel);
		return this.evaluator.evaluate(this.randomModel);
	}

	/**
	 * Samples uniformly an unparametrized MLC classifier from the set of all configurable classifiers and then samples a random parameterization of this classifier and its nested components.
	 *
	 * @return The random component instantiation of an MLC Meka classifier.
	 */
	private static ComponentInstance sampleRandomComponent(final List<ComponentInstance> allComponentInstances, final Random random) {
		LOGGER.trace("Sample uniformly algorithm selection.");
		ComponentInstance ciToInstantiate = new ComponentInstance(allComponentInstances.get(random.nextInt(allComponentInstances.size())));
		List<ComponentInstance> queue = new LinkedList<>();
		queue.add(ciToInstantiate);
		LOGGER.trace("Sample parameters for contained components.");
		while (!queue.isEmpty()) {
			ComponentInstance currentCI = queue.remove(0);
			if (!currentCI.getComponent().getParameters().isEmpty()) {
				ComponentInstance parametrization = null;
				while (parametrization == null) {
					try {
						parametrization = ComponentUtil.randomParameterizationOfComponent(currentCI.getComponent(), random);
					} catch (Exception e) {
						LOGGER.warn("Could not instantiate component instance {} with random parameters", ciToInstantiate, e);
					}
				}
				currentCI.getParameterValues().putAll(parametrization.getParameterValues());
			}
			if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
				queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
			}
		}
		LOGGER.trace("Return randomly sampled component instance {}", ciToInstantiate);
		return ciToInstantiate;
	}

	public void registerListener(final Object listener) {
		this.eventBus.register(listener);
		this.evaluator.registerListener(listener);
	}

}
