package agnesse.fixtures;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoginTest {
  @Test
  public void loginAcceptsANameAndRejectsABlank() {
    Login login = new Login();
    assertFalse(login.loggedIn());

    login.enter("bob");
    assertFalse(login.loggedIn());
    login.login();
    assertTrue(login.loggedIn());

    login.enter("");
    login.login();
    assertFalse(login.loggedIn());

    login.enter(null);
    login.login();
    assertFalse(login.loggedIn());

    login.enter("bob");
    login.login();
    assertTrue(login.loggedIn());
  }
}
