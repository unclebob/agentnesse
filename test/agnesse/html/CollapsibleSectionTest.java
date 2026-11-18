package agnesse.html;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CollapsibleSectionTest {
  @Test
  public void anIncludedPageLinksItsNameToTheFullPath() {
    assertEquals(
      "<details class=\"agnesse-section agnesse-include\">"
        + "<summary><button type=\"button\" class=\"disclosure\" aria-expanded=\"false\">&gt;</button>"
        + "<a href=\"Parent.SetUp\">SetUp</a></summary><table></table></details>\n",
      CollapsibleSection.html("SetUp", "Parent.SetUp", "<table></table>", false, true));
  }

  @Test
  public void aSectionWithNoPageKeepsAPlainTitle() {
    assertEquals(
      "<details class=\"agnesse-section\" open>"
        + "<summary><button type=\"button\" class=\"disclosure\" aria-expanded=\"true\">&gt;</button>Notes</summary>body</details>\n",
      CollapsibleSection.html("Notes", null, "body", true, false));
  }

  @Test
  public void aBlankPathIsNotALinkAndTheTitleIsEscaped() {
    assertEquals(
      "<details class=\"agnesse-section\">"
        + "<summary><button type=\"button\" class=\"disclosure\" aria-expanded=\"false\">&gt;</button>A&lt;B &amp; C</summary></details>\n",
      CollapsibleSection.html("A<B & C", "  ", null, false, false));
  }

  @Test
  public void aNullTitleIsEmpty() {
    assertEquals(
      "<details class=\"agnesse-section agnesse-include\" open>"
        + "<summary><button type=\"button\" class=\"disclosure\" aria-expanded=\"true\">&gt;</button>"
        + "<a href=\"Page&amp;\"></a></summary></details>\n",
      CollapsibleSection.html(null, "Page&", "", true, true));
  }
}
