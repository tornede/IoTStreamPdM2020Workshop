package IoTStream2020.DDPdM.search;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.algorithm.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.exceptions.ComponentInstantiationFailedException;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.ComponentUtil;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.jaicore.ml.core.dataset.serialization.ArffDatasetAdapter;
import ai.libs.jaicore.ml.core.evaluation.evaluator.MonteCarloCrossValidationEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.MonteCarloCrossValidationEvaluatorFactory;
import ai.libs.jaicore.ml.regression.loss.ERulPerformanceMeasure;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.jaicore.ml.scikitwrapper.ScikitLearnWrapper;
import ai.libs.jaicore.timing.TimedComputation;
import ai.libs.mlplan.multiclass.sklearn.EMLPlanSkLearnProblemType;
import ai.libs.mlplan.multiclass.sklearn.SKLearnClassifierFactory;

public class SearchSpaceConfigurationChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger("mlplan");

	private static EMLPlanSkLearnProblemType problemType = EMLPlanSkLearnProblemType.RUL;
	private static ERulPerformanceMeasure performanceMeasure = ERulPerformanceMeasure.ASYMMETRIC_LOSS;

	private static List<ComponentInstance> allComponentInstances;
	private static SKLearnClassifierFactory<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> factory;
	private static MonteCarloCrossValidationEvaluator evaluator;
	private static int numberOfPipelinesFound;
	private static int numberOfErrorsFound;

	public static void main(final String[] args) throws Exception {
		String searchConfigFile = args[0];
		String dataPath = args[1];

		allComponentInstances = new ArrayList<>(ComponentUtil.getAllAlgorithmSelectionInstances(problemType.getRequestedInterface(), new ComponentLoader(new File(searchConfigFile)).getComponents()));

		factory = new SKLearnClassifierFactory<>();
		factory.setProblemType(problemType.getBasicProblemType());
		// factory.setAnacondaEnvironment(anacondaEnvironment);
		// factory.setPathVariable(pathVariable);

		ILabeledDataset<ILabeledInstance> data = ArffDatasetAdapter.readDataset(new File(dataPath));
		evaluator = new MonteCarloCrossValidationEvaluatorFactory().withData(data).withNumMCIterations(1).withTrainFoldSize(0.7).withMeasure(performanceMeasure).withRandom(new Random(42)).getLearnerEvaluator();

		LOGGER.info("Testing search space config file: {}", searchConfigFile);
		testDefaultConfigs();
		testMinConfigs();
		testMaxConfigs();
		testCatConfigs();
		LOGGER.info("DONE. {} pipelines testes, {} errors found.", numberOfPipelinesFound, numberOfErrorsFound);
	}

	private static void testDefaultConfigs() throws Exception {
		LOGGER.info("Testing default configurations...");
		for (ComponentInstance ciToInstantiate : allComponentInstances) {
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(ciToInstantiate);

			LOGGER.trace("Sample parameters for contained components.");
			while (!queue.isEmpty()) {
				ComponentInstance currentCI = queue.remove(0);
				if (!currentCI.getComponent().getParameters().isEmpty()) {
					ComponentInstance parametrization = null;
					while (parametrization == null) {
						try {
							parametrization = ComponentUtil.defaultParameterizationOfComponent(currentCI.getComponent());
						} catch (Exception e) {
							LOGGER.warn("Could not instantiate component instance {} with max parameters", ciToInstantiate, e);
						}
					}
					currentCI.getParameterValues().putAll(parametrization.getParameterValues());
				}
				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}
			if (!ciToInstantiate.getSatisfactionOfRequiredInterfaces().isEmpty()) {
				tryExecution(ciToInstantiate);
			}
		}
		LOGGER.info("Testing default configurations done.");
	}

	private static void testMinConfigs() throws Exception {
		LOGGER.info("Testing minimum configurations...");
		for (ComponentInstance ciToInstantiate : allComponentInstances) {
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(ciToInstantiate);

			LOGGER.trace("Sample parameters for contained components.");
			while (!queue.isEmpty()) {
				ComponentInstance currentCI = queue.remove(0);
				if (!currentCI.getComponent().getParameters().isEmpty()) {
					ComponentInstance parametrization = null;
					while (parametrization == null) {
						try {
							parametrization = ComponentUtil.minParameterizationOfComponent(currentCI.getComponent());
						} catch (Exception e) {
							LOGGER.warn("Could not instantiate component instance {} with max parameters", ciToInstantiate, e);
						}
					}
					currentCI.getParameterValues().putAll(parametrization.getParameterValues());
				}
				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}
			if (!ciToInstantiate.getSatisfactionOfRequiredInterfaces().isEmpty()) {
				tryExecution(ciToInstantiate);
			}
		}
		LOGGER.info("Testing minimum configurations done.");
	}

	private static void testMaxConfigs() throws Exception {
		LOGGER.info("Testing maximum configurations...");
		for (ComponentInstance ciToInstantiate : allComponentInstances) {
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(ciToInstantiate);

			LOGGER.trace("Sample parameters for contained components.");
			while (!queue.isEmpty()) {
				ComponentInstance currentCI = queue.remove(0);
				if (!currentCI.getComponent().getParameters().isEmpty()) {
					ComponentInstance parametrization = null;
					while (parametrization == null) {
						try {
							parametrization = ComponentUtil.maxParameterizationOfComponent(currentCI.getComponent());
						} catch (Exception e) {
							LOGGER.warn("Could not instantiate component instance {} with max parameters", ciToInstantiate, e);
						}
					}
					currentCI.getParameterValues().putAll(parametrization.getParameterValues());
				}
				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}
			if (!ciToInstantiate.getSatisfactionOfRequiredInterfaces().isEmpty()) {
				tryExecution(ciToInstantiate);
			}
		}
		LOGGER.info("Testing maximum configurations done.");
	}

	private static void testCatConfigs() throws ComponentInstantiationFailedException, InterruptedException {
		LOGGER.info("Testing categorical configurations...");
		for (ComponentInstance componentInstance : allComponentInstances) {
			List<ComponentInstance> componentInstanceClonesWithAllPosibleCategoricalParameters = new ArrayList<>();
			List<ComponentInstance> queue = new LinkedList<>();
			queue.add(componentInstance);
			while (!queue.isEmpty()) {
				ComponentInstance currentCI = queue.remove(0);
				if (!currentCI.getComponent().getParameters().isEmpty()) {
					List<ComponentInstance> parameterizedComponentInstances = new ArrayList<>();
					String currentComponentName = currentCI.getComponent().getName();
					try {
						parameterizedComponentInstances.addAll(ComponentUtil.categoricalParameterizationsOfComponent(currentCI.getComponent()));
					} catch (Exception e) {
						LOGGER.warn("Could not instantiate component instance {} with categorical parameters", componentInstance, e);
					}
					for (ComponentInstance parameterization : parameterizedComponentInstances) {
						ComponentInstance option = new ComponentInstance(componentInstance);
						List<ComponentInstance> optionQueue = new LinkedList<>();
						optionQueue.add(option);
						while (!optionQueue.isEmpty()) {
							ComponentInstance currentOption = optionQueue.remove(0);
							if (!currentOption.getComponent().getParameters().isEmpty() && currentOption.getComponent().getName().equals(currentComponentName)) {
								currentOption.getParameterValues().putAll(parameterization.getParameterValues());
							}
							if (!currentOption.getSatisfactionOfRequiredInterfaces().isEmpty()) {
								optionQueue.addAll(currentOption.getSatisfactionOfRequiredInterfaces().values());
							}

						}
						componentInstanceClonesWithAllPosibleCategoricalParameters.add(option);
					}
				}
				if (!currentCI.getSatisfactionOfRequiredInterfaces().isEmpty()) {
					queue.addAll(currentCI.getSatisfactionOfRequiredInterfaces().values());
				}
			}
			for (ComponentInstance instance : componentInstanceClonesWithAllPosibleCategoricalParameters.stream().distinct().collect(Collectors.toList())) {
				tryExecution(instance);
			}
		}
		LOGGER.info("Testing categorical configurations done.");
	}

	private static void tryExecution(final ComponentInstance componentInstance) throws ComponentInstantiationFailedException, InterruptedException {
		ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> model = factory.getComponentInstantiation(componentInstance);
		try {
			numberOfPipelinesFound++;
			LOGGER.info("{}", model);
			TimedComputation.compute(new Callable<Double>() {
				@Override
				public Double call() throws Exception {
					return evaluator.evaluate(model);
				}
			}, new Timeout(30, TimeUnit.SECONDS).milliseconds(), "Evaluation timed out.");

		} catch (Exception e) {
			numberOfErrorsFound++;
			if (e.getMessage().contains("PredictionException")) {
				LOGGER.error("Invalid parameters for pipeline\n\t{}\n\t{}\n", model, e.getMessage().substring(e.getMessage().indexOf("PredictionException") + 21));
			} else {
				LOGGER.error("Exception for pipeline {}", model, e);
			}
		}
	}

}
