package agnesse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AgNesseVersionTest {

  @Test
  public void doesLoadVersionResourceFile() {
    AgNesseVersion version = new AgNesseVersion();
    assertTrue(version.toString().startsWith("v"));
    assertFalse(version.toString().endsWith("\n"));
  }
}
