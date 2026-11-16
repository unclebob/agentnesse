package agnesse.wiki;

import java.util.Optional;

public interface VariableSource {
  Optional<String> findVariable(String name);
}
