package IoTStream2020.DDPdM.search;

import java.util.StringJoiner;

import org.api4.java.algorithm.Timeout;

import IoTStream2020.DDPdM.experiment.IExperimentConfiguration;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.ml.regression.loss.ERulPerformanceMeasure;
import ai.libs.mlplan.multiclass.sklearn.EMLPlanSkLearnProblemType;

public class SearchConfigurationWithAnaconda extends SearchConfiguration {

	private String anacondaEnvironment;
	private String pathVariable;

	public SearchConfigurationWithAnaconda(final EMLPlanSkLearnProblemType problemType, final String searchSpaceConfigFile, final String searchAlgorithm, final String trainingDataPath, final Timeout maxSearchTime,
			final Timeout maxNodeEvaluationTime, final Timeout maxCandidateEvaluationTime, final ERulPerformanceMeasure performanceMeasure, final long seed, final int numCpus, final String anacondaEnvironment, final String pathVariable) {
		super(problemType, searchSpaceConfigFile, searchAlgorithm, trainingDataPath, maxSearchTime, maxNodeEvaluationTime, maxCandidateEvaluationTime, performanceMeasure, seed, numCpus);
		this.anacondaEnvironment = anacondaEnvironment;
		this.pathVariable = pathVariable;
	}

	public SearchConfigurationWithAnaconda(final ExperimentDBEntry experimentEntry, final IExperimentConfiguration experimentConfig) throws ExperimentEvaluationFailedException {
		super(experimentEntry, experimentConfig);
		this.anacondaEnvironment = experimentConfig.anacondaEnvironment();
		this.pathVariable = experimentConfig.pathVariable();
	}

	public String getAnacondaEnvironment() {
		return this.anacondaEnvironment;
	}

	public String getPathVariable() {
		return this.pathVariable;
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner("\n\t");
		sj.add(super.toString());
		sj.add("anacondaEnvironment=" + this.anacondaEnvironment);
		sj.add("pathVariable=" + this.pathVariable);
		return sj.toString();
	}

}
