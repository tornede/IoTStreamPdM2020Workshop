package IoTStream2020.DDPdM.search;

import java.io.File;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.algorithm.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import IoTStream2020.DDPdM.experiment.IExperimentConfiguration;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.ml.core.dataset.serialization.ArffDatasetAdapter;
import ai.libs.jaicore.ml.regression.loss.ERulPerformanceMeasure;
import ai.libs.mlplan.multiclass.sklearn.EMLPlanSkLearnProblemType;

public class SearchConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchConfiguration.class);

	private int experimentId;
	private EMLPlanSkLearnProblemType problemType;
	private String searchSpaceConfigFile;

	private String searchAlgorithm;
	private String trainingDataPath;
	private ILabeledDataset<ILabeledInstance> trainingData;
	private ILabeledDataset<ILabeledInstance> evaluationData;
	private Timeout maxSearchTime;
	private Timeout maxNodeEvaluationTime;
	private Timeout maxCandidateEvaluationTime;
	private ERulPerformanceMeasure performanceMeasure;
	private long seed;

	private int numCpus;

	private SearchConfiguration(final EMLPlanSkLearnProblemType problemType, final String searchSpaceConfigFile, final int numCpus) {
		this.problemType = problemType;
		this.searchSpaceConfigFile = searchSpaceConfigFile;
		this.numCpus = numCpus;
	}

	protected SearchConfiguration(final EMLPlanSkLearnProblemType problemType, final String searchSpaceConfigFile, final String searchAlgorithm, final String trainingDataPath, final Timeout maxSearchTime,
			final Timeout maxNodeEvaluationTime, final Timeout maxCandidateEvaluationTime, final ERulPerformanceMeasure performanceMeasure, final long seed, final int numCpus) {
		this(problemType, searchSpaceConfigFile, numCpus);
		this.experimentId = 1;
		this.searchAlgorithm = searchAlgorithm;
		this.trainingDataPath = trainingDataPath;
		this.performanceMeasure = performanceMeasure;

		this.maxSearchTime = maxSearchTime;
		this.maxNodeEvaluationTime = maxNodeEvaluationTime;
		this.maxCandidateEvaluationTime = maxCandidateEvaluationTime;

		this.seed = seed;
	}

	protected SearchConfiguration(final ExperimentDBEntry experimentEntry, final IExperimentConfiguration experimentConfig) throws ExperimentEvaluationFailedException {
		this(experimentConfig.getParsedProblemType(), experimentConfig.searchSpace(), experimentConfig.getNumberOfCPUs());

		Map<String, String> keys = experimentEntry.getExperiment().getValuesOfKeyFields();
		this.experimentId = experimentEntry.getId();
		this.searchAlgorithm = keys.get(IExperimentConfiguration.KEY_SEARCH_ALGORITHM);
		this.trainingDataPath = experimentConfig.dataPath() + keys.get(IExperimentConfiguration.KEY_DATASET);
		this.performanceMeasure = ERulPerformanceMeasure.valueOf(keys.get(IExperimentConfiguration.KEY_PERFORMANCE_MEASURE));

		this.maxSearchTime = this.parseTimeout(keys.get(IExperimentConfiguration.KEY_MAX_SEARCH_TIME));
		this.maxNodeEvaluationTime = this.parseTimeout(keys.get(IExperimentConfiguration.KEY_MAX_NODE_EVALUATION_TIME));
		this.maxCandidateEvaluationTime = this.parseTimeout(keys.get(IExperimentConfiguration.KEY_MAX_CANDIDATE_EVALUATION_TIME));

		this.seed = Long.parseLong(keys.get(IExperimentConfiguration.KEY_SEED));
	}

	public static SearchConfiguration of(final EMLPlanSkLearnProblemType problemType, final String searchSpaceConfigFile, final String searchAlgorithm, final String trainingDataPath, final Timeout maxSearchTime,
			final Timeout maxNodeEvaluationTime, final Timeout maxCandidateEvaluationTime, final ERulPerformanceMeasure performanceMeasure, final long seed, final int numCpus) {
		return new SearchConfiguration(problemType, searchSpaceConfigFile, searchAlgorithm, trainingDataPath, maxSearchTime, maxNodeEvaluationTime, maxCandidateEvaluationTime, performanceMeasure, seed, numCpus);
	}

	public static SearchConfiguration of(final ExperimentDBEntry experimentEntry, final IExperimentConfiguration experimentConfig) throws ExperimentEvaluationFailedException {
		if (experimentConfig.anacondaEnvironment() != null) {
			return new SearchConfigurationWithAnaconda(experimentEntry, experimentConfig);
		}
		return new SearchConfiguration(experimentEntry, experimentConfig);
	}

	public int getExperimentId() {
		return this.experimentId;
	}

	public EMLPlanSkLearnProblemType getProblemType() {
		return this.problemType;
	}

	public String getSearchSpaceConfigFile() {
		return this.searchSpaceConfigFile;
	}

	public String getSearchAlgorithm() {
		return this.searchAlgorithm;
	}

	public String getTrainingDataPath() {
		return this.trainingDataPath;
	}

	public ILabeledDataset<ILabeledInstance> getTrainingData() throws ExperimentEvaluationFailedException {
		if (this.trainingData == null) {
			this.trainingData = this.readDataFile(this.trainingDataPath);
		}
		return this.trainingData;
	}

	public ILabeledDataset<ILabeledInstance> getEvaluationData() throws ExperimentEvaluationFailedException {
		if (this.evaluationData == null) {
			this.evaluationData = this.readDataFile(this.trainingDataPath.replaceAll("train", "test"));
		}
		return this.evaluationData;
	}

	private ILabeledDataset<ILabeledInstance> readDataFile(final String filePath) throws ExperimentEvaluationFailedException {
		long start = System.currentTimeMillis();
		ILabeledDataset<ILabeledInstance> dataset;
		try {
			dataset = ArffDatasetAdapter.readDataset(new File(filePath));
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException(e);
		}
		LOGGER.info("Data {} read. Time to create dataset object was {}ms", filePath, System.currentTimeMillis() - start);
		return dataset;

	}

	public ERulPerformanceMeasure getPerformanceMeasure() {
		return this.performanceMeasure;
	}

	private Timeout parseTimeout(final String string) {
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		switch (string.charAt(string.length() - 1)) {
		case 'h':
			timeUnit = TimeUnit.HOURS;
			break;
		case 'm':
			timeUnit = TimeUnit.MINUTES;
			break;
		case 's':
			timeUnit = TimeUnit.SECONDS;
			break;
		}
		return new Timeout(Long.parseLong(string.substring(0, string.length() - 1)), timeUnit);
	}

	public Timeout getMaxSearchTime() {
		return this.maxSearchTime;
	}

	public Timeout getMaxNodeEvaluationTime() {
		return this.maxNodeEvaluationTime;
	}

	public Timeout getMaxCandidateEvaluationTime() {
		return this.maxCandidateEvaluationTime;
	}

	public int getNumCpus() {
		return this.numCpus;
	}

	public long getSeed() {
		return this.seed;
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner("\n\t");
		sj.add("SearchConfiguration:");
		sj.add("experimentId=" + this.experimentId);
		sj.add("searchSpaceConfigFile=" + this.searchSpaceConfigFile);
		sj.add("searchAlgorithm=" + this.searchAlgorithm);
		sj.add("trainingData=" + this.trainingDataPath);
		sj.add("evaluationData=" + this.trainingDataPath.replaceAll("train", "test"));
		sj.add("performanceMeasure=" + this.performanceMeasure);
		sj.add("maxSearchTime=" + this.maxSearchTime);
		sj.add("maxNodeEvaluationTime=" + this.maxNodeEvaluationTime);
		sj.add("maxCandidateEvaluationTime=" + this.maxCandidateEvaluationTime);
		sj.add("numCpus=" + this.numCpus);
		sj.add("seed=" + this.seed);
		return sj.toString();
	}

}
