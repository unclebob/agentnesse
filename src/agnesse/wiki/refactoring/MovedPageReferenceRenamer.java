// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.wiki.refactoring;

import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiWordReference;

import java.util.Optional;

public class MovedPageReferenceRenamer implements ChangeReference {
  private final WikiPage pageToBeMoved;
  private final String newParentName;

  public MovedPageReferenceRenamer(WikiPage pageToBeMoved, String newParentName) {
    this.pageToBeMoved = pageToBeMoved;
    this.newParentName = newParentName;
  }

  @Override
  public Optional<String> changeReference(WikiPage currentPage, String reference) {
    if (staysRelative(currentPage, reference)) {
      return Optional.empty();
    }
    return new WikiWordReference(currentPage, reference).getMovedPageRenamedContent(reference, pageToBeMoved, newParentName);
  }

  /** A relative reference whose source and target both move with the subtree still resolves after the move. */
  private boolean staysRelative(WikiPage currentPage, String reference) {
    if (reference.startsWith(".") || !inside(currentPage.getFullPath())) {
      return false;
    }
    WikiPage target = ReferenceIndex.resolve(currentPage, reference);
    return target != null && inside(target.getFullPath());
  }

  private boolean inside(WikiPagePath path) {
    WikiPagePath moved = pageToBeMoved.getFullPath();
    return path.equals(moved) || path.startsWith(moved);
  }
}
