package fitnesse.markdown;

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

  private void assertFrontMatterRejected(String text, String expected) {
    try {
      FrontMatter.read(text);
      fail("expected the front matter to be rejected");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }
  }
}
