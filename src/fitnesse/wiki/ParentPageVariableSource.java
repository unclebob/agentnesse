package fitnesse.wiki;

import java.util.Optional;

public class ParentPageVariableSource implements VariableSource {
  private final WikiPage page;

  public ParentPageVariableSource(WikiPage page) {
    this.page = page;
  }

  @Override
  public Optional<String> findVariable(String name) {
    if (page.isRoot()) {
      return Optional.empty();
    }
    return Optional.ofNullable(page.getParent().getVariable(name));
  }
}
