package IoTStream2020.DDPdM.search;

import java.util.concurrent.Callable;

public interface ISearch extends Callable<Object> {

	@Override
	public SearchResult call() throws Exception;
}
