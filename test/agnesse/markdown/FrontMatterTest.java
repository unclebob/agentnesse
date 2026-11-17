package agnesse.markdown;

import agnesse.wiki.PageData;
import agnesse.wiki.WikiPageProperty;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FrontMatterTest {

  @Test
  public void readsTypeTagsHelpAndPrune() {
    FrontMatter matter = FrontMatter.read("""
      {
        "type": "suite",
        "tags": ["smoke", "slow"],
        "help": "the suite",
        "prune": true
      }
      # Suite
      """);

    assertTrue(matter.present());
    assertEquals("suite", matter.type());
    assertEquals(List.of("smoke", "slow"), matter.tags());
    assertEquals("the suite", matter.help());
    assertTrue(matter.prune());
    assertEquals("# Suite\n", matter.body());
  }

  @Test
  public void missingFrontMatterIsTheWholeBody() {
    FrontMatter matter = FrontMatter.read("# Just markdown\n");

    assertFalse(matter.present());
    assertNull(matter.type());
    assertTrue(matter.tags().isEmpty());
    assertNull(matter.help());
    assertFalse(matter.prune());
    assertEquals("# Just markdown\n", matter.body());
  }

  @Test
  public void emptyTextIsTheWholeBody() {
    FrontMatter matter = FrontMatter.read("");

    assertFalse(matter.present());
    assertEquals("", matter.body());
  }

  @Test
  public void whitespaceOnlyIsTheWholeBody() {
    FrontMatter matter = FrontMatter.read("  \n\t");

    assertFalse(matter.present());
    assertEquals("  \n\t", matter.body());
  }

  @Test
  public void propertiesStandInWhenTheBodyHasNoFrontMatter() {
    WikiPageProperty properties = new WikiPageProperty();
    properties.set("Test");
    properties.set("Suites", "guide, slow");
    properties.set("Help", "how");
    properties.set("Prune");
    properties.set("LastModifyingUser", "robert");
    PageData data = new PageData("body\n", properties);

    FrontMatter matter = FrontMatter.from(data);

    assertTrue(matter.present());
    assertEquals("test", matter.type());
    assertEquals(List.of("guide", "slow"), matter.tags());
    assertEquals("how", matter.help());
    assertTrue(matter.prune());
    assertEquals("robert", matter.user());
    assertEquals("body\n", matter.body());
  }

  @Test
  public void aSuiteAttributeWinsOverATestAttribute() {
    WikiPageProperty properties = new WikiPageProperty();
    properties.set("Suite");
    properties.set("Test");
    FrontMatter matter = FrontMatter.from(new PageData("body\n", properties));

    assertEquals("suite", matter.type());
  }

  @Test
  public void frontMatterInTheBodyWinsOverProperties() {
    WikiPageProperty properties = new WikiPageProperty();
    properties.set("Suite");
    PageData data = new PageData(FrontMatter.write("test", List.of("ready"), "how", false, "body"), properties);

    FrontMatter matter = FrontMatter.from(data);

    assertEquals("test", matter.type());
    assertEquals(List.of("ready"), matter.tags());
    assertEquals("body", matter.body());
  }

  @Test
  public void writesTheFourKeysAndReadsThemBack() {
    String text = FrontMatter.write("test", List.of("one"), "helps", false, "body\n");
    FrontMatter matter = FrontMatter.read(text);

    assertEquals("test", matter.type());
    assertEquals(List.of("one"), matter.tags());
    assertEquals("helps", matter.help());
    assertFalse(matter.prune());
    assertEquals("body\n", matter.body());
  }

  @Test
  public void unknownKeyNamesTheKey() {
    assertFrontMatterRejected("""
      { "type": "test", "owner": "ada" }
      """, "owner");
  }

  @Test
  public void malformedJsonNamesTheProblem() {
    assertFrontMatterRejected("{ \"type\": ", "malformed JSON");
  }

  @Test
  public void aBadTypeNamesTheValue() {
    assertFrontMatterRejected("{ \"type\": \"wiki\" }\n", "wiki");
  }

  @Test
  public void aStringMayContainBracesAndQuotes() {
    FrontMatter matter = FrontMatter.read("{ \"help\": \"a } \\\"brace\\\"\", \"type\": \"static\" }\nbody\n");

    assertEquals("a } \"brace\"", matter.help());
    assertEquals("static", matter.type());
    assertEquals("body\n", matter.body());
  }

  @Test
  public void anEscapedQuoteDoesNotCloseTheFrontMatterString() {
    FrontMatter matter = FrontMatter.read("{ \"help\": \"a \\\"} b\", \"type\": \"static\" }\nbody\n");

    assertEquals("a \"} b", matter.help());
    assertEquals("static", matter.type());
    assertEquals("body\n", matter.body());
  }

  @Test
  public void leadingWhitespaceStillReadsFrontMatter() {
    FrontMatter matter = FrontMatter.read("\t  \n{ \"type\": \"test\" }\nbody");

    assertTrue(matter.present());
    assertEquals("test", matter.type());
    assertEquals("body", matter.body());
  }

  @Test
  public void aNullValueIsTheSameAsOmittingTheKey() {
    FrontMatter matter = FrontMatter.read(
      "{ \"type\": null, \"tags\": null, \"help\": null, \"prune\": null, \"user\": null }\nbody");

    assertTrue(matter.present());
    assertNull(matter.type());
    assertTrue(matter.tags().isEmpty());
    assertNull(matter.help());
    assertFalse(matter.prune());
    assertNull(matter.user());
    assertEquals("body", matter.body());
  }

  @Test
  public void theBodyStartsRightAfterTheClosingBrace() {
    assertEquals("", FrontMatter.read("{ \"type\": \"test\" }").body());
    assertEquals("# Hi", FrontMatter.read("{ \"type\": \"test\" }# Hi").body());
    assertEquals("\nHi", FrontMatter.read("{ \"type\": \"test\" }\n\nHi").body());
  }

  @Test
  public void tagsHelpAndPruneMustHaveTheRightShape() {
    assertFrontMatterRejected("{ \"tags\": \"smoke\" }\n", "tags");
    assertFrontMatterRejected("{ \"tags\": [1] }\n", "tags");
    assertFrontMatterRejected("{ \"help\": 1 }\n", "help");
    assertFrontMatterRejected("{ \"prune\": \"yes\" }\n", "prune");
  }

  @Test
  public void omittedKeysLeaveTheDefaults() {
    FrontMatter matter = FrontMatter.read("{ }\nrest\n");

    assertTrue(matter.present());
    assertNull(matter.type());
    assertTrue(matter.tags().isEmpty());
    assertNull(matter.help());
    assertFalse(matter.prune());
    assertEquals("rest\n", matter.body());
  }

  @Test
  public void prefixIsTheBytesBeforeTheBody() {
    FrontMatter matter = FrontMatter.read("{ \"type\": \"test\" }\nbody\n");

    assertEquals("{ \"type\": \"test\" }\n", matter.prefix());
    assertEquals("{ \"type\": \"test\" }", FrontMatter.objectText(matter.prefix()));
    assertEquals("body\n", matter.body());
  }

  @Test
  public void aBodyTightAgainstTheBraceKeepsThatPrefix() {
    FrontMatter matter = FrontMatter.read("{ \"type\": \"test\" }body");

    assertEquals("{ \"type\": \"test\" }", matter.prefix());
    assertEquals("{ \"type\": \"test\" }", FrontMatter.objectText(matter.prefix()));
    assertEquals("body", matter.body());
  }

  @Test
  public void objectTextDropsOnlyTheSeparatorNewline() {
    assertEquals("", FrontMatter.objectText(null));
    assertEquals("", FrontMatter.objectText(""));
    assertEquals("{\n}", FrontMatter.objectText("{\n}\n"));
    assertEquals("{}", FrontMatter.objectText("{}"));
  }

  @Test
  public void defaultFrontMatterMatchesTheSelectedType() {
    assertEquals("{\n  \"type\": \"test\",\n  \"tags\": [],\n  \"help\": null,\n  \"prune\": false\n}",
      FrontMatter.defaultJson("Test"));
    assertEquals("{\n  \"type\": \"suite\",\n  \"tags\": [],\n  \"help\": null,\n  \"prune\": false\n}",
      FrontMatter.defaultJson("Suite"));
    assertEquals("{\n  \"type\": \"static\",\n  \"tags\": [],\n  \"help\": null,\n  \"prune\": false\n}",
      FrontMatter.defaultJson("Static"));
    assertEquals("{\n  \"type\": null,\n  \"tags\": [],\n  \"help\": null,\n  \"prune\": false\n}",
      FrontMatter.defaultJson("Default"));
    assertEquals(FrontMatter.defaultJson("Default"), FrontMatter.defaultJson(null));
  }

  @Test
  public void submittedBlankClearsAndAnythingElseMustBeAnObject() {
    assertFalse(FrontMatter.readSubmitted(null).present());
    assertFalse(FrontMatter.readSubmitted("").present());
    assertFalse(FrontMatter.readSubmitted("  \n").present());
    assertEquals("test", FrontMatter.readSubmitted("{ \"type\": \"test\" }").type());
    assertFrontMatterRejectedSubmitted("hello", "malformed JSON");
    assertFrontMatterRejectedSubmitted("{ \"type\": \"wiki\" }", "wiki");
  }

  @Test
  public void applyStoresTypeTagsHelpPruneAndUser() {
    PageData data = new PageData("body", new WikiPageProperty());
    data.setAttribute("Suite");
    data.setAttribute(WikiPageProperty.SUITES, "old");
    data.setAttribute(WikiPageProperty.HELP, "old help");
    data.setAttribute(WikiPageProperty.PRUNE);
    data.setAttribute(WikiPageProperty.LAST_MODIFYING_USER, "old");
    data.setAttribute(WikiPageProperty.EDIT);
    data.setFrontMatterPrefix(null);

    FrontMatter.apply(data, FrontMatter.read("{ \"type\": \"test\", \"tags\": [\"a\", \"b\"], \"help\": \"how\", \"prune\": true, \"user\": \"ada\" }"), "Notes");

    assertEquals("", data.getFrontMatterPrefix());
    assertTrue(data.hasAttribute("Test"));
    assertFalse(data.hasAttribute("Suite"));
    assertEquals("a, b", data.getAttribute(WikiPageProperty.SUITES));
    assertEquals("how", data.getAttribute(WikiPageProperty.HELP));
    assertTrue(data.hasAttribute(WikiPageProperty.PRUNE));
    assertEquals("ada", data.getAttribute(WikiPageProperty.LAST_MODIFYING_USER));
    assertTrue(data.hasAttribute(WikiPageProperty.EDIT));
  }

  @Test
  public void applyClearsFieldsAndInfersAMissingTypeFromTheName() {
    PageData suite = new PageData("body", new WikiPageProperty());
    suite.setAttribute("Test");
    suite.setAttribute(WikiPageProperty.SUITES, "old");
    suite.setAttribute(WikiPageProperty.HELP, "old");
    suite.setAttribute(WikiPageProperty.PRUNE);
    suite.setAttribute(WikiPageProperty.LAST_MODIFYING_USER, "old");
    FrontMatter.apply(suite, FrontMatter.read("{ \"type\": \"suite\" }"), "TestChild");
    assertTrue(suite.hasAttribute("Suite"));
    assertFalse(suite.hasAttribute("Test"));
    assertFalse(suite.hasAttribute(WikiPageProperty.SUITES));
    assertFalse(suite.hasAttribute(WikiPageProperty.HELP));
    assertFalse(suite.hasAttribute(WikiPageProperty.PRUNE));
    assertFalse(suite.hasAttribute(WikiPageProperty.LAST_MODIFYING_USER));

    PageData inferred = new PageData("body", new WikiPageProperty());
    FrontMatter.apply(inferred, FrontMatter.read("{ \"type\": null }"), "TestChild");
    assertTrue(inferred.hasAttribute("Test"));

    PageData namedSuite = new PageData("body", new WikiPageProperty());
    FrontMatter.apply(namedSuite, FrontMatter.read("{ }"), "SuiteChild");
    assertTrue(namedSuite.hasAttribute("Suite"));

    PageData notes = new PageData("body", new WikiPageProperty());
    notes.setAttribute("Test");
    FrontMatter.apply(notes, FrontMatter.read("{ \"type\": \"static\" }"), "TestChild");
    assertFalse(notes.hasAttribute("Test"));
    assertFalse(notes.hasAttribute("Suite"));
  }

  @Test
  public void anUnchangedSubmissionKeepsThePrefixAndAChangeReplacesIt() {
    PageData same = new PageData("old", new WikiPageProperty());
    same.setFrontMatterPrefix("{ \"type\" : \"test\" }\n");
    same.setAttribute("Suite");
    String json = "{ \"type\" : \"test\" }";
    FrontMatter.keepOrApply(same, FrontMatter.readSubmitted(json), json, "Notes");
    assertTrue(same.preserveFrontMatter());
    assertEquals("{ \"type\" : \"test\" }\n", same.getFrontMatterPrefix());
    assertTrue(same.hasAttribute("Suite"));

    PageData changed = new PageData("old", new WikiPageProperty());
    changed.setFrontMatterPrefix("{ \"type\": \"static\" }\n");
    changed.setAttribute(WikiPageProperty.EDIT);
    FrontMatter.keepOrApply(changed, FrontMatter.readSubmitted("{ \"type\": \"test\", \"help\": \"how\" }"),
      "{ \"type\": \"test\", \"help\": \"how\" }", "Notes");
    assertFalse(changed.preserveFrontMatter());
    assertTrue(changed.hasAttribute("Test"));
    assertEquals("how", changed.getAttribute(WikiPageProperty.HELP));

    PageData cleared = new PageData("old", new WikiPageProperty());
    cleared.setFrontMatterPrefix("{ \"type\": \"test\" }\n");
    FrontMatter.keepOrApply(cleared, FrontMatter.readSubmitted("  "), "  ", "TestChild");
    assertTrue(cleared.preserveFrontMatter());
    assertEquals("", cleared.getFrontMatterPrefix());
  }

  private void assertFrontMatterRejectedSubmitted(String text, String expected) {
    try {
      FrontMatter.readSubmitted(text);
      fail("expected the front matter to be rejected");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }
  }

  private void assertFrontMatterRejected(String text, String expected) {
    try {
      FrontMatter.read(text);
      fail("expected the front matter to be rejected");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }
  }
}
