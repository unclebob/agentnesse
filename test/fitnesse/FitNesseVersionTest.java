package fitnesse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FitNesseVersionTest {

  @Test
  public void doesLoadVersionResourceFile() {
    FitNesseVersion version = new FitNesseVersion();
    assertTrue(version.toString().startsWith("v"));
    assertFalse(version.toString().endsWith("\n"));
  }
}
