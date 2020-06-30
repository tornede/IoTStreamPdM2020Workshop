package IoTStream2020.DDPdM.experiment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.api4.java.ai.ml.core.evaluation.IPredictionBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import IoTStream2020.DDPdM.search.ISearch;
import IoTStream2020.DDPdM.search.SearchConfiguration;
import IoTStream2020.DDPdM.search.SearchResult;
import IoTStream2020.DDPdM.search.bestfirst.BestFirstSearch;
import IoTStream2020.DDPdM.search.random.RandomSearch;
import ai.libs.jaicore.db.IDatabaseAdapter;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.IExperimentSetEvaluator;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.ml.regression.loss.ERulPerformanceMeasure;

public class ExperimentEvaluator implements IExperimentSetEvaluator {

	private static final Logger LOGGER = LoggerFactory.getLogger("Experiment");

	private final IDatabaseAdapter adapter;
	private final String subtaskId;
	private final IExperimentConfiguration experimentConfig;

	public ExperimentEvaluator(final IDatabaseAdapter adapter, final String subtaskId, final IExperimentConfiguration experimentConfig) {
		super();
		this.adapter = adapter;
		this.subtaskId = subtaskId;
		this.experimentConfig = experimentConfig;
	}

	@Override
	public void evaluate(final ExperimentDBEntry experimentEntry, final IExperimentIntermediateResultProcessor processor) throws ExperimentEvaluationFailedException, InterruptedException {
		LOGGER.info("Reading in experiment.");

		SearchConfiguration searchConfig = SearchConfiguration.of(experimentEntry, this.experimentConfig);

		// EXperimentcode start
		LOGGER.info("Using {}", searchConfig);
		Map<String, Object> map = new HashMap<>();
		map.put("subtask_id", this.subtaskId);
		SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

		try {
			/* create model */
			ISearch searchAlgorithm;
			switch (searchConfig.getSearchAlgorithm()) {
			case "RandomSearch":
				searchAlgorithm = new RandomSearch(searchConfig, this.adapter, this.subtaskId);
				break;
			case "BestFirstSearch":
				searchAlgorithm = new BestFirstSearch(searchConfig, new ClassifierEvaluationEventListener(this.adapter, searchConfig.getExperimentId(), this.subtaskId));
				break;
			default:
				map.put("end", format.format(new Date(System.currentTimeMillis())));
				processor.processResults(map);
				throw new RuntimeException("Unknown search algorithm: " + searchConfig.getSearchAlgorithm());
			}

			LOGGER.info("Training started ... ");
			map.put("train_start", format.format(new Date(System.currentTimeMillis())));
			processor.processResults(map);
			map.clear();

			SearchResult searchResult = searchAlgorithm.call();

			map.put("train_end", format.format(new Date(System.currentTimeMillis())));
			processor.processResults(map);
			map.clear();
			LOGGER.info("Training finished.");

			if (searchResult.getModel() != null) {
				map.put("finalpipeline", searchResult.getModel().toString());
				map.put("internal_performance", searchResult.getError());

				LOGGER.info("Testing started ...");
				map.put("test_start", format.format(new Date(System.currentTimeMillis())));
				processor.processResults(map);
				map.clear();

				searchResult.getModel().setTimeout(null);
				searchResult.getModel().setSeed(searchConfig.getSeed());
				IPredictionBatch batch = searchResult.getModel().fitAndPredict(searchConfig.getTrainingData(), searchConfig.getEvaluationData());
				map.put("test_end", format.format(new Date(System.currentTimeMillis())));

				List<Double> expected = searchConfig.getEvaluationData().stream().map(i -> (double) i.getLabel()).collect(Collectors.toList());
				List<Double> actual = batch.getPredictions().stream().map(i -> Math.max(0, (double) i.getPrediction())).collect(Collectors.toList());
				for (ERulPerformanceMeasure performanceMeasure : ERulPerformanceMeasure.values()) {
					map.put("performance_" + performanceMeasure.name().toLowerCase(), performanceMeasure.loss(expected, actual));
				}

				LOGGER.info("Testing finished.");

			} else {
				map.put("finalpipeline", "None found.");
				LOGGER.info("No pipeline found.");
			}
			LOGGER.info("{}", map);
			processor.processResults(map);
			map.clear();
		} catch (Throwable e) {
			map.put("end", format.format(new Date(System.currentTimeMillis())));
			processor.processResults(map);
			throw new ExperimentEvaluationFailedException(e);
		}

		map.put("end", format.format(new Date(System.currentTimeMillis())));
		processor.processResults(map);
		LOGGER.info("Finished Experiment {}. Results: {}", experimentEntry.getExperiment().getValuesOfKeyFields(), map);
	}

}
