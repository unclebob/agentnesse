package agnesse.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class WikiPageLineProcessingUtilTest {

  @Test
  public void testColumnSpecialWiki() {
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("ensure"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("reject"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("check"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("check not"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("note"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("show"));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord(" "));
    assertTrue(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("$value="));
    assertFalse(WikiPageLineProcessingUtil.isColumnSpecialWikiKeyWord("not a keyword"));
  }

  @Test
  public void testLineNeedsExtraLastColumn() {
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|ensure|keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|reject|keyword|"));
    assertTrue(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|check|keyword|"));
    assertTrue(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|check not|keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|note|keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|show|keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("| |keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|$value=|keyword|"));
    assertFalse(WikiPageLineProcessingUtil.doesLineNeedExtraLastColumn("|not a |keyword|keyword"));
  }

  @Test
  public void testGetMethodNameFromLine() {
    assertEquals("", WikiPageLineProcessingUtil.getMethodNameFromLine("not a valid line"));
    assertEquals("noParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|no param|"));
    assertEquals("noParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|$value=|no param|"));
    assertEquals("noParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|show|no param|"));
    assertEquals("noParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|ensure|no param|"));
    assertEquals("noParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|reject|no           param      |"));
    assertEquals("itHasOneParam", WikiPageLineProcessingUtil.getMethodNameFromLine("|check|it has one param|1|check value|"));
    assertEquals("itHasAnd", WikiPageLineProcessingUtil.getMethodNameFromLine("|check not|it has|param1|and|param2|check not value|"));
    assertEquals("itHas3ParamAndAnd", WikiPageLineProcessingUtil.getMethodNameFromLine("|note|it has 3 param|param1|and|param2|and|param3|"));
    assertEquals("itHas3ParamAndAnd", WikiPageLineProcessingUtil.getMethodNameFromLine("||it has ||3 param and|param2|and|param3|"));
  }

}
