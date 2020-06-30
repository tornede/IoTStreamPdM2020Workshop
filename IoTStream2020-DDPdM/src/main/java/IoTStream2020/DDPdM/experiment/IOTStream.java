package IoTStream2020.DDPdM.experiment;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.api4.java.datastructure.kvstore.IKVStore;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.ESignificanceTestResult;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.kvstore.KVStoreSequentialComparator;
import ai.libs.jaicore.basic.kvstore.KVStoreStatisticsUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;
import ai.libs.jaicore.db.sql.SQLAdapter;

public class IOTStream {

	private static final List<String> MEASURES_TO_CONSIDER = Arrays.asList("performance_asymmetric_loss", "performance_mean_percentage_error", "performance_mean_absolute_percentage_error", "performance_root_mean_squared_error");

	public static void main(final String[] args) throws SQLException {
		SQLAdapter adapter = new SQLAdapter("isys-otfml.cs.upb.de", "results", "Hallo333!", "automl_pdm");

		KVStoreCollection col = KVStoreUtil.readFromMySQLTable(adapter, "experimentsLossGuidance", new HashMap<>());
		col.projectRemove("exception", "max_search_time", "subtask_id", "time_started", "test_end", "train_end", "performance_weighted_absolute_error", "performance_mean_squared_logarithmic_mean_squared_error",
				"performance_quadratic_quadratic_error", "host", "train_start", "time_created", "end", "time_end", "internal_performance", "performance_mean_squared_percentage_error", "test_start", "max_candidate_evaluation_time",
				"experiment_id", "cpus", "performance_weighted_asymmetric_absolute_error", "max_node_evaluation_time", "memory_max", "performance_mean_squared_error", "performance_mean_absolute_error",
				"performance_linear_mean_squared_error", "internal_performance_measure", "finalpipeline");

		List<String> replaceValuesByOne = Arrays.asList("null", "Infinity", "-Infinity");
		col.stream().forEach(x -> {
			MEASURES_TO_CONSIDER.stream().forEach(y -> {
				if (replaceValuesByOne.contains(x.getAsString(y) + "")) {
					x.put(y, 1.0);
				}
			});
		});

		Map<String, EGroupMethod> grouping = new HashMap<>();
		MEASURES_TO_CONSIDER.stream().forEach(x -> grouping.put(x, EGroupMethod.AVG));

		String[] groupKeys = { "internal_performance", "search_algorithms", "dataset" };
		KVStoreCollection grouped = col.group(groupKeys, grouping);

		KVStoreCollection finalCol = new KVStoreCollection();
		for (IKVStore store : grouped) {
			for (String measure : MEASURES_TO_CONSIDER) {
				IKVStore cStore = new KVStore(store.toString());
				cStore.put("measure", measure);
				cStore.put("setting", cStore.getAsString("dataset") + "#" + measure);
				cStore.put("approach", MEASURES_TO_CONSIDER.indexOf(measure) + "_" + cStore.getAsString("search_algorithms") + "#" + measure);
				cStore.put("valueList", cStore.get(measure + "_list"));
				cStore.put("meanValue", cStore.get(measure));
				cStore.put("stdValue", cStore.get(measure + "_stdDev"));
				finalCol.add(cStore);
			}
		}
		finalCol.project(new String[] { "measure", "setting", "valueList", "approach", "meanValue", "stdValue", "search_algorithms", "seed", "dataset" });
		KVStoreStatisticsUtil.wilcoxonSignedRankTest(finalCol, "setting", "search_algorithms", "seed", "valueList", "BestFirstSearch", "sig");
		KVStoreStatisticsUtil.best(finalCol, "setting", "search_algorithms", "valueList", "best");
		finalCol.sort(new KVStoreSequentialComparator("measure", "search_algorithms", "dataset"));

		for (IKVStore store : finalCol) {
			store.put("entry", ValueUtil.valueToString(store.getAsDouble("meanValue"), 4) + "$\\pm$" + ValueUtil.valueToString(store.getAsDouble("stdValue"), 2));

			if (store.getAsBoolean("best")) {
				store.put("entry", "\\textbf{" + store.getAsString("entry") + "}");
			}

			if (store.containsKey("sig")) {
				switch (ESignificanceTestResult.valueOf(store.getAsString("sig"))) {
				case INFERIOR:
					store.put("entry", store.getAsString("entry") + " $\\bullet$");
					break;
				case SUPERIOR:
					store.put("entry", store.getAsString("entry") + " $\\circ$");
					break;
				default:
					store.put("entry", store.getAsString("entry") + " $\\phantom{\\bullet}$");
					break;
				}
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("0_BestFirstSearch#performance_asymmetric_loss", "0 BF AL");
		replacements.put("0_RandomSearch#performance_asymmetric_loss", "0 RS AL");
		replacements.put("1_BestFirstSearch#performance_mean_absolute_percentage_error", "1 BF MAPE");
		replacements.put("1_RandomSearch#performance_mean_absolute_percentage_error", "1 RS MAPE");
		replacements.put("1_BestFirstSearch#performance_mean_percentage_error", "2 BF MAE");
		replacements.put("1_RandomSearch#performance_mean_percentage_error", "2 RS MAE");
		replacements.put("1_BestFirstSearch#performance_root_mean_squared_error", "3 BF RMSE");
		replacements.put("1_RandomSearch#performance_root_mean_squared_error", "3 RS RMSE");

		System.out.println(finalCol);

		System.out.println("######");
		finalCol.stream().forEach(x -> x.put("approach", replacements.get(x.getAsString("approach"))));

		System.out.println(finalCol);

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(finalCol, "dataset", "approach", "entry");
		for (Entry<String, String> entry : replacements.entrySet()) {
			latexTable.replaceAll(entry.getKey(), entry.getValue());
		}

		System.out.println(latexTable);
	}
}
