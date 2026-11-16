package agnesse.fixtures;

public class AgNesseStatus {
  public boolean isRunning() {
    return AgNesseFixtureContext.context.agNesse.isRunning();
  }
}
