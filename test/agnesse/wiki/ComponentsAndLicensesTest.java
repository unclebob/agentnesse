package agnesse.wiki;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ComponentsAndLicensesTest {

  @Test
  public void thePageNamesEveryRuntimeLibrary() throws Exception {
    String text = Files.readString(Path.of("AgNesseRoot/AgNesse/ComponentsAndLicenses.md"));
    for (String library : List.of(
      "velocity-engine-core",
      "commons-lang3",
      "slf4j-api",
      "slf4j-jdk14",
      "org.json:json",
      "commonmark-ext-gfm-tables",
      "commonmark",
      "diffutils",
      "commons-text",
      "groovy-xml",
      "CodeMirror"
    )) {
      assertTrue(library, text.contains(library));
    }
    assertTrue(text.contains("AgNesse"));
  }
}
