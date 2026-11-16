package agnesse.slim.protocol;

import agnesse.slim.SlimError;

public class SyntaxError extends SlimError {
  private static final long serialVersionUID = 1L;

  public SyntaxError(String s) {
    super(s);
  }

  public SyntaxError(Throwable e) {
    super(e);
  }
}
