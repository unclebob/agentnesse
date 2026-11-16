package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;

public class BaselineDecisionTable extends DecisionTable {

	public BaselineDecisionTable(Table table, String id, SlimTestContext context) {
		super(table, id, context);
		setBaselineDecisionTable(true);
	}
}
