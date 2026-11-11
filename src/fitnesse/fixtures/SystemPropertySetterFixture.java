package fitnesse.fixtures;

public class SystemPropertySetterFixture {
  private String key;
  private String value;

  public void execute() {
    System.getProperties().setProperty(key, value);
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
