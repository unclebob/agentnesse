package agnesse.wiki;

public class WikiPageLoadException extends RuntimeException {
  public WikiPageLoadException(final String message) {
    super(message);
  }

  public WikiPageLoadException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
