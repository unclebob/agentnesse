package fitnesse.markdown;

public final class ContentsLink {
  private final String name;
  private final String href;

  public ContentsLink(String name, String href) {
    this.name = name;
    this.href = href;
  }

  public String name() {
    return name;
  }

  public String href() {
    return href;
  }
}
