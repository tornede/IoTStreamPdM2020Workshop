package IoTStream2020.DDPdM.experiment;

import java.io.File;

import org.aeonbits.owner.ConfigFactory;

import ai.libs.jaicore.db.IDatabaseAdapter;
import ai.libs.jaicore.db.IDatabaseConfig;
import ai.libs.jaicore.db.sql.SQLAdapter;
import ai.libs.jaicore.experiments.IExperimentDatabaseHandle;
import ai.libs.jaicore.experiments.databasehandle.ExperimenterMySQLHandle;

public class ConfigurationContainer {

	private final IExperimentConfiguration config;
	private final IExperimentDatabaseHandle databaseHandle;
	private final IDatabaseAdapter adapter;

	public ConfigurationContainer(final String databaseConfigFile, String configurationFile) throws Exception {

		/* get experiment configuration */
		this.config = ConfigFactory.create(IExperimentConfiguration.class);
		this.config.loadPropertiesFromFile(new File(configurationFile));

		/* setup database connection */
		IDatabaseConfig dbConfig = ConfigFactory.create(IDatabaseConfig.class);
		dbConfig.loadPropertiesFromFile(new File(databaseConfigFile));
		this.adapter = new SQLAdapter(dbConfig);
		this.databaseHandle = new ExperimenterMySQLHandle(this.adapter, dbConfig.getDBTableName());
	}
	

	public IExperimentConfiguration getConfig() {
		return this.config;
	}

	public IExperimentDatabaseHandle getDatabaseHandle() {
		return this.databaseHandle;
	}

	public IDatabaseAdapter getAdapter() {
		return this.adapter;
	}
}
