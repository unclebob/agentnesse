package fitnesse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static fitnesse.ConfigurationParameter.LISTEN;
import static fitnesse.ConfigurationParameter.LOG_DIRECTORY;
import static fitnesse.ConfigurationParameter.MAXIMUM_WORKERS;
import static fitnesse.ConfigurationParameter.REFACTOR_START_PATH;
import static fitnesse.ConfigurationParameter.THEME;

/**
 * Reads agentnesse.json. Command-line flags are applied after this, so they win.
 */
public final class AgentNesseConfig {
  private static final Set<String> KEYS = Set.of(
    "port", "listen", "rootPath", "pageRoot",
    "logDirectory", "maximumWorkers", "theme", "refactorStartPath");

  private AgentNesseConfig() {
  }

  public static void apply(ContextConfigurator configurator, File file, boolean required) {
    if (!file.isFile()) {
      if (required) {
        throw new IllegalArgumentException(file.getPath() + ": configuration file not found");
      }
      return;
    }
    JSONObject json = read(file);
    for (String key : json.keySet()) {
      if (json.isNull(key)) {
        if (!known(key)) {
          throw unknown(file, key);
        }
        continue;
      }
      if (!applyAddress(configurator, file, json, key) && !applyServer(configurator, file, json, key)) {
        throw unknown(file, key);
      }
    }
  }

  private static boolean known(String key) {
    return KEYS.contains(key);
  }

  private static JSONObject read(File file) {
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      Object value = new JSONTokener(reader).nextValue();
      if (!(value instanceof JSONObject)) {
        throw new IllegalArgumentException(file.getPath() + ": malformed JSON");
      }
      return (JSONObject) value;
    } catch (JSONException e) {
      throw new IllegalArgumentException(file.getPath() + ": malformed JSON");
    } catch (IOException e) {
      throw new IllegalArgumentException(file.getPath() + ": cannot read configuration (" + e.getMessage() + ")");
    }
  }

  private static boolean applyAddress(ContextConfigurator configurator, File file, JSONObject json, String key) {
    switch (key) {
      case "port":
        configurator.withPort(requireInt(file, key, json));
        return true;
      case "listen":
        configurator.withParameter(LISTEN, requireString(file, key, json));
        return true;
      case "rootPath":
        configurator.withRootPath(requireString(file, key, json));
        return true;
      case "pageRoot":
        configurator.withRootDirectoryName(requireString(file, key, json));
        return true;
      default:
        return false;
    }
  }

  private static boolean applyServer(ContextConfigurator configurator, File file, JSONObject json, String key) {
    switch (key) {
      case "logDirectory":
        configurator.withParameter(LOG_DIRECTORY, requireString(file, key, json));
        return true;
      case "maximumWorkers":
        configurator.withParameter(MAXIMUM_WORKERS, Integer.toString(requireInt(file, key, json)));
        return true;
      case "theme":
        configurator.withParameter(THEME, requireString(file, key, json));
        return true;
      case "refactorStartPath":
        configurator.withParameter(REFACTOR_START_PATH, requireString(file, key, json));
        return true;
      default:
        return false;
    }
  }

  private static IllegalArgumentException unknown(File file, String key) {
    return new IllegalArgumentException(file.getPath() + ": unknown key \"" + key + "\"");
  }

  private static int requireInt(File file, String key, JSONObject json) {
    Object value = json.get(key);
    if (!(value instanceof Number) || value instanceof Double) {
      throw new IllegalArgumentException(file.getPath() + ": key \"" + key + "\" must be an integer");
    }
    return ((Number) value).intValue();
  }

  private static String requireString(File file, String key, JSONObject json) {
    Object value = json.get(key);
    if (!(value instanceof String)) {
      throw new IllegalArgumentException(file.getPath() + ": key \"" + key + "\" must be a string");
    }
    return (String) value;
  }
}
