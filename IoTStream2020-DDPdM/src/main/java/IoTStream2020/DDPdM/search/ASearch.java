package IoTStream2020.DDPdM.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import IoTStream2020.DDPdM.experiment.ClassifierEvaluationEventListener;

public abstract class ASearch implements ISearch {

	protected static final Logger LOGGER = LoggerFactory.getLogger("mlplan");

	private final ClassifierEvaluationEventListener eventListener;
	private final SearchConfiguration configuration;

	public ASearch(final SearchConfiguration configuration, final ClassifierEvaluationEventListener eventListener) {
		super();
		this.eventListener = eventListener;
		this.configuration = configuration;
	}

	public SearchConfiguration getConfiguration() {
		return this.configuration;
	}

	public ClassifierEvaluationEventListener getEventListener() {
		return this.eventListener;
	}

}
