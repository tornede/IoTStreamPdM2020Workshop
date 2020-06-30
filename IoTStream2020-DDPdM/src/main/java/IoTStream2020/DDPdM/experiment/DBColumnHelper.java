package IoTStream2020.DDPdM.experiment;

import java.io.IOException;

import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;

import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.ml.regression.loss.ERulPerformanceMeasure;

public class DBColumnHelper {

	public static void main(final String[] args) throws IOException, ExperimentEvaluationFailedException, SplitFailedException, InterruptedException {
		System.out.print("subtask_id, train_start, train_end, finalpipeline, internal_performance, test_start, test_end, end, ");
		for (ERulPerformanceMeasure measure : ERulPerformanceMeasure.values()) {
			System.out.print(getERulPerformanceMeasureName(measure) + ", ");
		}
	}

	public static String getERulPerformanceMeasureName(final ERulPerformanceMeasure measure) {
		return "performance_" + measure.name().toLowerCase();
	}

}
