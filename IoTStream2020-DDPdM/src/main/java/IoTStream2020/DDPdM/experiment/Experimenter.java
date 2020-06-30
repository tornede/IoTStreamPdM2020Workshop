package IoTStream2020.DDPdM.experiment;

import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.experiments.ExperimentRunner;
import ai.libs.jaicore.experiments.IExperimentDatabaseHandle;

public class Experimenter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Experimenter.class);

	public static void main(final String[] args) throws Exception {
		String subtaskId = args[0];
		Timeout overallTimeout = new Timeout(Long.parseLong(args[1]), TimeUnit.MINUTES);
		Timeout searchTimeout = new Timeout(Long.parseLong(args[2]), TimeUnit.MINUTES);

		ConfigurationContainer container;
		if (args.length > 2 && args[2].equals("mac")) {
			container = new ConfigurationContainer("conf/experiment/mac_db.conf", "conf/experiment/mac_experiments.conf");
		} else {
			container = new ConfigurationContainer("conf/experiment/db.conf", "conf/experiment/experiments.conf");
		}

		IExperimentDatabaseHandle databaseHandle = container.getDatabaseHandle();

		/* run an experiment */
		LOGGER.info("Creating the runner.");
		ExperimentRunner runner = new ExperimentRunner(container.getConfig(), new ExperimentEvaluator(container.getAdapter(), subtaskId, container.getConfig()), databaseHandle);
		runner.setLoggerName(LOGGER.getName());
		long start = System.currentTimeMillis();
		while ((overallTimeout.milliseconds() - (System.currentTimeMillis() - start)) >= (searchTimeout.milliseconds() + new Timeout(55, TimeUnit.MINUTES).milliseconds())) {
			LOGGER.info("Conducting next experiment.");
			try {
				runner.randomlyConductExperiments(1);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		LOGGER.info("No more time left to conduct more experiments. Stopping.");
		System.exit(0);
	}

}
