package IoTStream2020.DDPdM.experiment;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.evaluation.execution.LearnerExecutionFailedException;
import org.api4.java.ai.ml.core.evaluation.execution.LearnerExecutionInterruptedException;
import org.api4.java.datastructure.kvstore.IKVStore;

import com.google.common.eventbus.Subscribe;

import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.db.IDatabaseAdapter;
import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.ml.core.evaluation.evaluator.events.TrainTestSplitEvaluationFailedEvent;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPrediction;
import ai.libs.jaicore.ml.regression.singlelabel.SingleTargetRegressionPredictionBatch;
import ai.libs.jaicore.ml.scikitwrapper.ScikitLearnWrapper;
import ai.libs.jaicore.processes.OS;
import ai.libs.jaicore.processes.ProcessUtil;
import ai.libs.mlplan.core.events.ClassifierFoundEvent;

public class ClassifierEvaluationEventListener {

	private final IDatabaseAdapter adapter;
	private final String dbName;
	private final String tableName;
	private final int experimentId;
	private final String subtaskId;
	private final SimpleDateFormat format;

	public ClassifierEvaluationEventListener(final IDatabaseAdapter adapter, final int experimentId, final String subtaskId) throws SQLException {
		this.adapter = adapter;
		this.experimentId = experimentId;
		this.subtaskId = subtaskId;
		IDatabaseConfig dbConfig = ConfigFactory.create(IDatabaseConfig.class);
		if (ProcessUtil.getOS() == OS.MAC) {
			dbConfig.loadPropertiesFromFile(new File("conf/experiment/mac_db.conf"));
		} else {
			dbConfig.loadPropertiesFromFile(new File("conf/experiment/db.conf"));
		}
		this.dbName = dbConfig.getDBDatabaseName();
		this.tableName = dbConfig.getDBTableName() + "_eventlogs";
		this.format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

		this.createResultsTableIfNecessary();
	}

	@SuppressWarnings("unchecked")
	@Subscribe
	public void rcvClassifierFoundEvent(final ClassifierFoundEvent event) {
		ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> learner = null;
		if (event.getSolutionCandidate() instanceof ScikitLearnWrapper) {
			learner = (ScikitLearnWrapper) event.getSolutionCandidate();
			this.logCandidateEvaluation("success", learner.toString(), event.getInSampleError() + "", event.getTimeToEvaluate() + "ms");
		}
	}

	@Subscribe
	public void rcvTrainTestSplitEvaluationFailedEvent(final TrainTestSplitEvaluationFailedEvent<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> event) {
		ScikitLearnWrapper<SingleTargetRegressionPrediction, SingleTargetRegressionPredictionBatch> learner = null;
		if (event.getLearner() instanceof ScikitLearnWrapper) {
			learner = (ScikitLearnWrapper) event.getLearner();
		}
		if (learner != null) {
			ILearnerRunReport report = event.getReport();
			String status = "unknown";
			if (event.getReport().getException() instanceof LearnerExecutionInterruptedException) {
				status = "timeout";
			} else if (event.getReport().getException() instanceof LearnerExecutionFailedException) {
				if (ExceptionUtils.getStackTrace(event.getReport().getException()).contains("NoSuchFileException")) {
					status = "timeout (python)";
				} else if (ExceptionUtils.getStackTrace(event.getReport().getException()).contains("MemoryError: Unable to allocate")) {
					status = "memory full";
				} else {
					status = "crashed";
				}
			}
			String exceptionStackTrace = ExceptionUtils.getStackTrace(report.getException());
			long candidateRuntime = (report.getTrainEndTime() - report.getTrainStartTime()) + (report.getTestEndTime() - report.getTestStartTime());
			this.logCandidateEvaluation(status, learner.toString(), exceptionStackTrace, candidateRuntime + "ms");

		}
	}

	private void logCandidateEvaluation(final String status, final String pipeline, final String inSampleError, final String candidateRuntime) {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("experiment_id", this.experimentId);
			map.put("subtask_id", this.subtaskId);
			map.put("thread", Thread.currentThread().getName());
			map.put("status", status);
			map.put("pipeline", pipeline);
			map.put("result", inSampleError);
			map.put("timestamp_found", this.format.format(new Date(System.currentTimeMillis())));
			map.put("candidate_runtime", candidateRuntime);
			new KVStore(map);
			this.adapter.insert(this.tableName, map);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void createResultsTableIfNecessary() throws SQLException {
		List<IKVStore> resultSet = this.adapter.getResultsOfQuery("SHOW TABLES");
		boolean resultTableAlreadyExists = resultSet.stream().anyMatch(kvStore -> kvStore.getAsString("Tables_in_" + this.dbName).equals(this.tableName));
		if (!resultTableAlreadyExists) {
			this.adapter.update("CREATE TABLE " + this.tableName + " (\n" + //
					"`experiment_id` VARCHAR(255) NOT NULL,\n" + //
					"`subtask_id` VARCHAR(255) NOT NULL,\n" + //
					" `thread` varchar(255),\n" + //
					" `status` VARCHAR(255) NOT NULL,\n" + //
					" `pipeline` LONGTEXT NOT NULL,\n" + //
					" `result` LONGTEXT NOT NULL,\n" + //
					" `timestamp_found` VARCHAR(255) NOT NULL,\n" + //
					" `candidate_runtime` VARCHAR(255) NOT NULL\n) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin", new ArrayList<>());
		}
	}
}