package agnesse.testsystems.slim.property;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueTest {
  @Test
  public void intsBooleansAndTextArePlainCells() {
    assertEquals("0", new IntValue(0).cell());
    assertEquals("-12", new IntValue(-12).cell());
    assertEquals("true", new BoolValue(true).cell());
    assertEquals("false", new BoolValue(false).cell());
    assertEquals("red", new TextValue("red").cell());
    assertEquals("", new TextValue("").cell());
  }

  @Test
  public void listsUseTheSlimListText() {
    assertEquals("[]", new ListValue(List.of()).cell());
    Value items = new ListValue(List.of(new IntValue(1), new IntValue(-2), new BoolValue(true)));
    assertEquals("[1, -2, true]", items.cell());
  }

  @Test
  public void nestedListsKeepTheirElements() {
    Value inner = new ListValue(List.of(new IntValue(1), new IntValue(2)));
    Value outer = new ListValue(List.of(inner, new ListValue(List.of(new TextValue("a")))));
    assertEquals("[[1, 2], [a]]", outer.cell());
  }

  @Test
  public void replacingAnElementDoesNotChangeTheOriginalList() {
    ListValue items = new ListValue(List.of(new IntValue(1), new IntValue(2)));
    assertEquals("[1, 9]", items.replace(1, new IntValue(9)).cell());
    assertEquals("[1, 2]", items.cell());
  }

  @Test
  public void aPrefixDropsTheTail() {
    ListValue items = new ListValue(List.of(new IntValue(4), new IntValue(5), new IntValue(6)));
    assertEquals("[4]", items.prefix(1).cell());
    assertEquals("[]", items.prefix(0).cell());
  }
}
