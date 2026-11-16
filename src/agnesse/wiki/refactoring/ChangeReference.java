package agnesse.wiki.refactoring;

import agnesse.wiki.WikiPage;

import java.util.Optional;

public interface ChangeReference {
  Optional<String> changeReference(WikiPage page, String reference);
}
