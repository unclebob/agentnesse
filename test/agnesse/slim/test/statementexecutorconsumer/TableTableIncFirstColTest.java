package agnesse.slim.test.statementexecutorconsumer;

import agnesse.slim.SlimException;
import agnesse.slim.StatementExecutor;
import agnesse.slim.StatementExecutorInterface;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TableTableIncFirstColTest {
  @Test
  public void theFixtureIncrementsSymbolsItManages() {
    Symbols symbols = new Symbols();
    symbols.assign("X", "99");
    TableTableIncFirstCol fixture = new TableTableIncFirstCol();
    fixture.setStatementExecutor(symbols);
    assertEquals("tableTable.*\\.doTable", symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));

    List<List<String>> table = List.of(
      List.of("$X", "$X="),
      List.of("$X", "$X="),
      List.of("$X", "$X="),
      List.of("$X", "$X="),
      List.of("$X", "$X="),
      List.of("$X", "$Y=")
    );
    List<List<String>> result = fixture.doTable(table);

    assertEquals(List.of("no change", "pass:100"), result.get(0));
    assertEquals(List.of("no change", "pass:104"), result.get(4));
    assertEquals(List.of("no change", "pass:105"), result.get(5));
    assertEquals(104, symbols.getSymbolObject("X"));
    assertEquals(105, symbols.getSymbolObject("Y"));
    assertNull(symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));
  }

  @Test
  public void aPlainNumberIncrementsWithoutAssigningASymbol() {
    Symbols symbols = new Symbols();
    TableTableIncFirstCol fixture = new TableTableIncFirstCol();
    fixture.setStatementExecutor(symbols);

    List<List<String>> result = fixture.doTable(List.of(List.of("3", "plain")));
    assertEquals(List.of("no change", "pass:4"), result.get(0));
    assertNull(symbols.getSymbolObject("plain"));

    symbols.assign("A", "1");
    symbols.assign("B", "2");
    result = fixture.doTable(List.of(List.of("$A$B", "out")));
    assertEquals(List.of("no change", "pass:13"), result.get(0));
    assertNull(symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));
  }

  @Test
  public void aMissingOrEmptyValueStillReleasesSymbolHandling() {
    Symbols symbols = new Symbols();
    TableTableIncFirstCol fixture = new TableTableIncFirstCol();
    fixture.setStatementExecutor(symbols);
    try {
      fixture.doTable(List.of(List.of("$Missing", "$Z=")));
      fail("missing symbol");
    } catch (NumberFormatException e) {
      assertNull(symbols.getSymbolObject("Z"));
    }
    assertNull(symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));

    fixture.setStatementExecutor(symbols);
    try {
      fixture.doTable(List.of(List.of("", "out")));
      fail("empty cell");
    } catch (NumberFormatException e) {
      assertNull(symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));
    }

    fixture.setStatementExecutor(symbols);
    symbols.assign("Gone", null);
    try {
      fixture.doTable(List.of(Arrays.asList(null, "out")));
      fail("null cell");
    } catch (NumberFormatException e) {
      assertNull(symbols.getSymbolObject(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS));
    }
  }

  private static final class Symbols implements StatementExecutorInterface {
    private final Map<String, Object> symbols = new HashMap<>();

    @Override
    public void assign(String name, Object value) {
      symbols.put(name, value);
    }

    @Override
    public Object getSymbol(String symbolName) {
      return getSymbolObject(symbolName);
    }

    @Override
    public Object getSymbolObject(String symbolName) {
      return symbols.get(symbolName);
    }

    @Override
    public Object getInstance(String instanceName) {
      throw new UnsupportedOperationException(instanceName);
    }

    @Override
    public boolean stopHasBeenRequested() {
      return false;
    }

    @Override
    public void reset() {
      symbols.clear();
    }

    @Override
    public void setInstance(String actorInstanceName, Object actor) {
    }

    @Override
    public void addPath(String path) throws SlimException {
      throw new UnsupportedOperationException(path);
    }

    @Override
    public void create(String instanceName, String className, Object... constructorArgs) throws SlimException {
      throw new UnsupportedOperationException(className);
    }

    @Override
    public Object callAndAssign(String symbolName, String instanceName, String methodsName, Object... arguments) {
      throw new UnsupportedOperationException(methodsName);
    }

    @Override
    public Object call(String instanceName, String methodName, Object... arguments) {
      throw new UnsupportedOperationException(methodName);
    }
  }
}
