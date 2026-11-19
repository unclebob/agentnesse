package agnesse.slim.test;

/** execute throws, so a property can record a failure that is not a wrong cell. */
public class PropertyProbe {
  public void setN(int n) {
  }

  public void execute() {
    throw new IllegalStateException("boom");
  }

  public boolean ok() {
    return true;
  }
}
