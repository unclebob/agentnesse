package agnesse.testsystems.slim.property;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueTest {
  @Test
  public void intsBooleansAndTextArePlainCells() {
    assertEquals("0", new TextValue("0").cell());
    assertEquals("-12", new TextValue("-12").cell());
    assertEquals("true", new TextValue("true").cell());
    assertEquals("false", new TextValue("false").cell());
    assertEquals("", new TextValue("").cell());
  }

  @Test
  public void listsUseTheSlimListText() {
    assertEquals("[]", new ListValue(List.of()).cell());
    Value items = new ListValue(List.of(new TextValue("1"), new TextValue("-2"), new TextValue("true")));
    assertEquals("[1, -2, true]", items.cell());
  }

  @Test
  public void nestedListsKeepTheirElements() {
    Value inner = new ListValue(List.of(new TextValue("1"), new TextValue("2")));
    Value outer = new ListValue(List.of(inner, new ListValue(List.of(new TextValue("a")))));
    assertEquals("[[1, 2], [a]]", outer.cell());
  }

  @Test
  public void replacingAnElementDoesNotChangeTheOriginalList() {
    ListValue items = new ListValue(List.of(new TextValue("1"), new TextValue("2")));
    assertEquals("[1, 9]", items.replace(1, new TextValue("9")).cell());
    assertEquals("[1, 2]", items.cell());
  }

  @Test
  public void aPrefixDropsTheTail() {
    ListValue items = new ListValue(List.of(new TextValue("4"), new TextValue("5"), new TextValue("6")));
    assertEquals("[4]", items.prefix(1).cell());
    assertEquals("[]", items.prefix(0).cell());
  }
}
