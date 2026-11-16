package agnesse;

import agnesseMain.Arguments;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class AgNesseConfigTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void appliesEveryKey() throws Exception {
    File file = write("{\"port\": 8123, \"rootPath\": \"work\", \"pageRoot\": \"Pages\", " +
      "\"maximumWorkers\": 12, " +
      "\"refactorStartPath\": \"FrontPage\", \"listen\": \"0.0.0.0\"}");

    ContextConfigurator configurator = ContextConfigurator.empty();
    AgNesseConfig.apply(configurator, file, true);

    assertThat(configurator.get(ConfigurationParameter.PORT), is("8123"));
    assertThat(configurator.get(ConfigurationParameter.ROOT_PATH), is("work"));
    assertThat(configurator.get(ConfigurationParameter.ROOT_DIRECTORY), is("Pages"));
    assertThat(configurator.get(ConfigurationParameter.MAXIMUM_WORKERS), is("12"));
    assertThat(configurator.get(ConfigurationParameter.REFACTOR_START_PATH), is("FrontPage"));
    assertThat(configurator.get(ConfigurationParameter.LISTEN), is("0.0.0.0"));
  }

  @Test
  public void commandLineOverridesTheFile() throws Exception {
    File file = write("{\"port\": 8123, \"listen\": \"0.0.0.0\"}");
    ContextConfigurator configurator = ContextConfigurator.systemDefaults();
    AgNesseConfig.apply(configurator, file, true);
    configurator = new Arguments("-p", "81", "--listen", "127.0.0.2").update(configurator);

    assertThat(configurator.get(ConfigurationParameter.PORT), is("81"));
    assertThat(configurator.get(ConfigurationParameter.LISTEN), is("127.0.0.2"));
  }

  @Test
  public void missingDefaultFileKeepsDefaults() throws Exception {
    ContextConfigurator configurator = ContextConfigurator.systemDefaults();
    AgNesseConfig.apply(configurator, new File(folder.getRoot(), "absent.json"), false);

    assertThat(configurator.makeAgNesseContext().port, is(80));
    assertThat(configurator.get(ConfigurationParameter.LISTEN), is("127.0.0.1"));
    assertThat(configurator.get(ConfigurationParameter.ROOT_DIRECTORY), is("AgNesseRoot"));
  }

  @Test
  public void missingExplicitFileIsAnError() throws Exception {
    File missing = new File(folder.getRoot(), "missing.json");
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), missing, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("missing.json"));
      assertThat(e.getMessage(), containsString("not found"));
    }
  }

  @Test
  public void unknownKeyNamesTheFileAndKey() throws Exception {
    File file = write("{\"port\": 80, \"nope\": true}");
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), file, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString(file.getName()));
      assertThat(e.getMessage(), containsString("nope"));
    }
    File nullValue = folder.newFile("null-key.json");
    Files.writeString(nullValue.toPath(), "{\"nope\": null}", StandardCharsets.UTF_8);
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), nullValue, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("nope"));
    }
  }

  @Test
  public void themeIsAnUnknownKey() throws Exception {
    File file = folder.newFile("theme.json");
    Files.writeString(file.toPath(), "{\"theme\": \"bootstrap\"}", StandardCharsets.UTF_8);
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), file, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString(file.getName()));
      assertThat(e.getMessage(), containsString("theme"));
      assertThat(e.getMessage(), containsString("unknown key"));
    }
  }

  @Test
  public void aDoubleIsNotAnInteger() throws Exception {
    File file = folder.newFile("double.json");
    Files.writeString(file.toPath(), "{\"port\": 1.5}", StandardCharsets.UTF_8);
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), file, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("integer"));
    }
  }

  @Test
  public void malformedJsonNamesTheFile() throws Exception {
    File file = write("{");
    try {
      AgNesseConfig.apply(ContextConfigurator.empty(), file, true);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString(file.getName()));
      assertThat(e.getMessage(), containsString("malformed"));
    }
  }

  private File write(String json) throws Exception {
    File file = folder.newFile("agnesse.json");
    Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    return file;
  }
}
