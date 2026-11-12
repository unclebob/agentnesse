package fitnesse.wiki.refactoring;

import fitnesse.components.TraversalListener;
import fitnesse.wiki.*;
import java.util.Optional;
import java.util.Set;

public class ReferenceRenamingTraverser implements TraversalListener<WikiPage> {

  public static void renameReferences(WikiPage root, ChangeReference changeReference) {
    root.getPageCrawler().traverse(new ReferenceRenamingTraverser(changeReference), new NoPruningStrategy());
  }

  private final ChangeReference changeReference;

  private ReferenceRenamingTraverser(ChangeReference changeReference) {
    this.changeReference = changeReference;
  }

  @Override
  public void process(WikiPage currentPage) {
    PageData data = currentPage.getData();
    boolean pageHasChanged = updatePageContent(currentPage, data);

    if (pageHasChanged) {
      currentPage.commit(data);
    }
  }

  private boolean updatePageContent(WikiPage currentPage, PageData data) {
    String content = data.getContent();

    String newContent = getUpdatedPageContent(currentPage);

    boolean pageHasChanged = !newContent.equals(content);
    if (pageHasChanged) {
      data.setContent(newContent);
    }
    return pageHasChanged;
  }

  private String getUpdatedPageContent(WikiPage currentPage) {
    return MarkdownReferences.replace(currentPage, currentPage.getData().getContent(),
      reference -> changeReference.changeReference(currentPage, reference));
  }
}
