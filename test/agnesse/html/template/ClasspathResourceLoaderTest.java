package agnesse.html.template;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.util.ExtProperties;
import org.junit.Test;

import java.io.Reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClasspathResourceLoaderTest {
  @Test
  public void loadsATemplateAndReportsItUnchanged() throws Exception {
    ClasspathResourceLoader loader = loader("/agnesse/resources/templates");
    try (Reader reader = loader.getResourceReader("skeleton.vm", "UTF-8")) {
      char[] buffer = new char[256];
      int count = reader.read(buffer);
      assertTrue(count > 0);
      assertTrue(new String(buffer, 0, count).contains("html"));
    }
    assertEquals(0, loader.getLastModified(null));
    assertFalse(loader.isSourceModified(null));
  }

  @Test
  public void aBaseThatAlreadyEndsWithASlashIsLeftAlone() throws Exception {
    ClasspathResourceLoader loader = loader("/agnesse/resources/templates/");
    try (Reader reader = loader.getResourceReader("skeleton.vm", "UTF-8")) {
      assertTrue(reader.read() >= 0);
    }
  }

  @Test
  public void anEmptyNameIsRejected() {
    ClasspathResourceLoader loader = loader("/agnesse/resources/templates");
    try {
      loader.getResourceReader("", "UTF-8");
      fail("empty name");
    } catch (ResourceNotFoundException e) {
      assertEquals("No template name provided", e.getMessage());
    }
    try {
      loader.getResourceReader(null, "UTF-8");
      fail("null name");
    } catch (ResourceNotFoundException e) {
      assertEquals("No template name provided", e.getMessage());
    }
  }

  @Test
  public void aMissingTemplateIsRejected() {
    ClasspathResourceLoader loader = loader("/agnesse/resources/templates");
    try {
      loader.getResourceReader("missing-template.vm", "UTF-8");
      fail("missing");
    } catch (ResourceNotFoundException e) {
      assertTrue(e.getMessage().contains("missing-template.vm"));
    }
  }

  private static ClasspathResourceLoader loader(String base) {
    ClasspathResourceLoader loader = new ClasspathResourceLoader();
    ExtProperties properties = new ExtProperties();
    properties.setProperty("base", base);
    loader.init(properties);
    return loader;
  }
}
