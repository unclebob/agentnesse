package agnesse.wiki;


import java.util.Optional;

public class ApplicationVariableSource implements VariableSource {

  private final VariableSource variableSource;

  public ApplicationVariableSource(VariableSource variableSource) {
    this.variableSource = variableSource;
  }

  @Override
  public Optional<String> findVariable(String name) {
    if (variableSource != null) {
      switch (name) {
        case "AGNESSE_PORT":
          return Optional.of(variableSource.findVariable("AGNESSE_PORT").orElse("-1"));
        case "AGNESSE_ROOTPATH":
          return Optional.of(variableSource.findVariable("AGNESSE_ROOTPATH").orElse(""));
        case "AGNESSE_VERSION":
          return Optional.of(variableSource.findVariable("AGNESSE_VERSION").orElse(""));
        default:
          return Optional.empty();
      }
    }
    return Optional.empty();
  }
}
