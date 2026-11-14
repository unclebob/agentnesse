package fitnesse;

import java.util.Properties;

/**
 * Parameters used to configure FitNesse.
 */
public enum ConfigurationParameter {

  CONFIG_FILE("ConfigFile"),
  LOG_LEVEL("LogLevel"),
  LOG_DIRECTORY("LogDirectory"),
  ROOT_PATH("RootPath"),
  ROOT_DIRECTORY("FitNesseRoot"),
  PORT("Port"),
  OUTPUT("RedirectOutput"),
  COMMAND("Command"),
  RERUN_FAILURES("RerunFailures"),
  LISTEN("listen"),
  MAXIMUM_WORKERS("MaximumWorkers"),
  THEME("Theme"),
  REFACTOR_START_PATH("refactorStartPath");

  private final String name;

  private ConfigurationParameter(String key) {
    this.name = key;
  }

  public String getKey() {
    return name;
  }

  public static Properties makeProperties(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Number of arguments should be even (name, value)");
    }

    Properties properties = new Properties();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      String key = keyValuePairs[i] instanceof ConfigurationParameter ? ((ConfigurationParameter) keyValuePairs[i]).getKey() : keyValuePairs[i].toString();
      String value = keyValuePairs[i+1].toString();
      properties.setProperty(key, value);
    }
    return properties;
  }

  public static ConfigurationParameter byKey(String key) {
    for (ConfigurationParameter parameter : ConfigurationParameter.values()) {
      if (parameter.getKey().equals(key)) {
        return parameter;
      }
    }
    return null;
  }
}
