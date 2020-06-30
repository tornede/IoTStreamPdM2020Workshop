package IoTStream2020.DDPdM.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.exceptions.ComponentInstantiationFailedException;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.jaicore.ml.scikitwrapper.ScikitLearnWrapper;

public class SearchResult {

	private static final Logger LOGGER = LoggerFactory.getLogger("mlplan");

	private ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> model;
	private double error;

	public SearchResult() {
		this.error = Double.MAX_VALUE;
	}

	public synchronized void update(final ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> candidateModel, final Double candidateError) throws ComponentInstantiationFailedException {
		if (candidateError != null && candidateError.doubleValue() < this.error) {
			LOGGER.info("New incumbent found: " + candidateError + " (current best known: " + this.error + ")");
			this.model = candidateModel;
			this.error = candidateError;
		}
	}

	public ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> getModel() {
		return this.model;
	}

	public Double getError() {
		return this.error;
	}

	public static SearchResult of(final ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> candidateModel, final Double candidateError) throws ComponentInstantiationFailedException {
		SearchResult result = new SearchResult();
		result.update(candidateModel, candidateError);
		return result;
	}

}
