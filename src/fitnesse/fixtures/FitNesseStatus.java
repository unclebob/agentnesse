package fitnesse.fixtures;

public class FitNesseStatus {
  public boolean isRunning() {
    return FitnesseFixtureContext.context.fitNesse.isRunning();
  }
}
