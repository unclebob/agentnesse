package agnesse.wiki.search;

import agnesse.components.TraversalListener;
import agnesse.wiki.PageData;
import agnesse.wiki.WikiPage;

import static agnesse.util.WikiPageLineProcessingUtil.getMethodNameFromLine;

public class MethodWikiPageFinder extends WikiPageFinder {

  private String methodToFind;

  public MethodWikiPageFinder(String methodToFind, TraversalListener<? super WikiPage> observer) {
    super(observer);
    this.methodToFind = methodToFind;
  }

  @Override
  protected boolean pageMatches(WikiPage page) {
    String pageContent = page.getData().getContent();
    String targetMethod = getMethodNameFromLine(this.methodToFind);
    String[] contentLines = pageContent.split(PageData.PAGE_LINE_SEPARATOR);

    //Both the inputs should follow the correct format
    if (!methodToFind.startsWith("|") || !methodToFind.endsWith("|"))
      return false;

    for (String eachLine : contentLines) {
      if (getMethodNameFromLine(eachLine).equals(targetMethod))
        return true;
    }
    return false;
  }

}
