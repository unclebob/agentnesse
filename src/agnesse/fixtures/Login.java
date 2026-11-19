package agnesse.fixtures;

/** The actor for the ScriptLogin sample. A blank name stays logged out. */
public class Login {
  private String user;
  private boolean in;

  public void enter(String name) {
    user = name;
  }

  public void login() {
    in = user != null && !user.isEmpty();
  }

  public boolean loggedIn() {
    return in;
  }
}
