package IoTStream2020.DDPdM.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.db.IDatabaseAdapter;
import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.experiments.ExperimentDatabasePreparer;

public class DatabaseTableSetup {

	public static void main(final String[] args) throws Exception {
		/* prepare database for this combination */
		String dbConfigFile = "";
		String experimentConfigFile = "";
		if (args.length > 0 && args[0].contentEquals("mac")) {
			dbConfigFile = "conf/experiment/mac_db.conf";
			experimentConfigFile = "conf/experiment/mac_experiments.conf";
		} else {
			dbConfigFile = "conf/experiment/db.conf";
			experimentConfigFile = "conf/experiment/experiments.conf";
		}

		ConfigurationContainer container = new ConfigurationContainer(dbConfigFile, experimentConfigFile);
		ExperimentDatabasePreparer preparer = new ExperimentDatabasePreparer(container.getConfig(), container.getDatabaseHandle());
		preparer.setLoggerName("DatabaseSetup");
		preparer.synchronizeExperiments();

		/* prepare database for eventlogs */
		IDatabaseAdapter adapter = container.getAdapter();
		IDatabaseConfig dbConfig = ConfigFactory.create(IDatabaseConfig.class);
		dbConfig.loadPropertiesFromFile(new File(dbConfigFile));

		String dbName = dbConfig.getDBDatabaseName();
		String tableName = dbConfig.getDBTableName() + "_eventlogs";

		List<IKVStore> resultSet = adapter.getResultsOfQuery("SHOW TABLES");
		boolean resultTableAlreadyExists = resultSet.stream().anyMatch(kvStore -> kvStore.getAsString("Tables_in_" + dbName).equals(tableName));
		if (!resultTableAlreadyExists) {
			adapter.update("CREATE TABLE " + tableName + " (\n" + //
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
