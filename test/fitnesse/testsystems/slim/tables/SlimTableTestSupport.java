package fitnesse.testsystems.slim.tables;

import java.util.ArrayList;
import java.util.List;

import fitnesse.slim.instructions.Instruction;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.SlimTestContextImpl;
import fitnesse.testsystems.slim.Table;
import fitnesse.wiki.WikiPageDummy;

import org.junit.Before;

/**
 * Test support class for testing slim tables. Class provides
 * the List of {@link #instructions} created and the {@link #tableUnderTest}
 * as protected fields.
 *
 * @param <T> The type of the table under test.
 */
public abstract class SlimTableTestSupport<T extends SlimTable> {

  protected List<Instruction> instructions;
  protected List<SlimAssertion> assertions;
  protected T tableUnderTest;

  public T createSlimTable(String tableText) {
    Table t = MarkdownTable.parse(tableText);
    SlimTestContextImpl testContext = new SlimTestContextImpl(new WikiTestPage(new WikiPageDummy()));
    SlimTableFactory tableFactory = new SlimTableFactory();
    return (T) tableFactory.makeSlimTable(t, "id", testContext);
  }

  @Before
  public void setUp() throws Exception {
    instructions = new ArrayList<>();
    assertions = new ArrayList<>();
  }

  protected T makeSlimTableAndBuildInstructions(String pageContents) throws Exception {
    tableUnderTest = createSlimTable(pageContents);
    assertions.addAll(tableUnderTest.getAssertions());
    instructions.addAll(SlimAssertion.getInstructions(assertions));
    return tableUnderTest;
  }
}
