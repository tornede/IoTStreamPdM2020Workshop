package IoTStream2020.DDPdM.experiment;

import java.util.List;

import ai.libs.jaicore.experiments.IExperimentSetConfig;
import ai.libs.mlplan.multiclass.sklearn.EMLPlanSkLearnProblemType;

public interface IExperimentConfiguration extends IExperimentSetConfig {

	public static final String KEY_PROBLEM_TYPE = "problem_type";
	public static final String KEY_SEARCHSPACE = "search_space";
	public static final String KEY_SEARCH_ALGORITHM = "search_algorithms";
	public static final String KEY_DATAPATH = "data_path";
	public static final String KEY_DATASET = "dataset";

	public static final String KEY_MAX_SEARCH_TIME = "max_search_time";
	public static final String KEY_MAX_NODE_EVALUATION_TIME = "max_node_evaluation_time";
	public static final String KEY_MAX_CANDIDATE_EVALUATION_TIME = "max_candidate_evaluation_time";
	public static final String KEY_PERFORMANCE_MEASURE = "internal_performance_measure";

	public static final String KEY_ANACONDA_ENVIRONMENT = "anaconda_environment";
	public static final String KEY_PATH_VARIABLE = "path_variable";

	public static final String KEY_SEED = "seed";

	@Key(KEY_PROBLEM_TYPE)
	public String problemType();

	public default EMLPlanSkLearnProblemType getParsedProblemType() {
		return EMLPlanSkLearnProblemType.valueOf(this.problemType());
	}

	@Key(KEY_SEARCHSPACE)
	public String searchSpace();

	@Key(KEY_SEARCH_ALGORITHM)
	public List<String> searchAlgorithms();

	@Key(KEY_DATAPATH)
	public String dataPath();

	@Key(KEY_DATASET)
	public List<String> dataSet();

	@Key(KEY_MAX_SEARCH_TIME)
	public String maxSearchTime();

	@Key(KEY_MAX_NODE_EVALUATION_TIME)
	public String maxNodeEvaluationTime();

	@Key(KEY_MAX_CANDIDATE_EVALUATION_TIME)
	public String maxCandidateEvaluationTime();

	@Key(KEY_PERFORMANCE_MEASURE)
	public String performanceMeasure();

	@Key(KEY_ANACONDA_ENVIRONMENT)
	public String anacondaEnvironment();

	@Key(KEY_PATH_VARIABLE)
	public String pathVariable();

	@Key(KEY_SEED)
	public List<Integer> seeds();

}
