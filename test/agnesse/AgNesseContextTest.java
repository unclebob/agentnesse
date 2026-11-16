package agnesse;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import agnesse.testutil.AgNesseUtil;

public class AgNesseContextTest {

  @Test
  public void testGetRootPagePath() {
    AgNesseContext context = AgNesseUtil.makeTestContext();
    assertEquals(context.rootPath + File.separator + AgNesseUtil.base, context.getRootPagePath());
  }

  @Test
  public void testProgressIsStoredUnderTheRoot() {
    AgNesseContext context = AgNesseUtil.makeTestContext();
    assertEquals(context.getRootPagePath() + File.separator + "files" + File.separator + "testProgress",
      context.getTestProgressPath());
  }

}
