package fitnesse.markdown.convert;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikiTreeTest {

  @Test
  public void aDirectoryPageBecomesMarkdownBesideItsChildren() throws Exception {
    Path root = Files.createTempDirectory("wiki");
    Files.writeString(root.resolve("content.txt"), "!contents\n");
    Files.writeString(root.resolve("properties.xml"), "<properties><Suite/></properties>");
    Path child = Files.createDirectory(root.resolve("Child"));
    Files.writeString(child.resolve("content.txt"), "| script |\n| check | x |\n");
    Files.writeString(child.resolve("properties.xml"), "<properties><Test/></properties>");
    Files.writeString(child.resolve("Leaf.wiki"), """
      ---
      Test
      Help: the leaf
      ---
      !2 Hi
      """);

    WikiTree.convert(root, root);

    assertFalse(Files.exists(root.resolve("content.txt")));
    assertFalse(Files.exists(root.resolve("properties.xml")));
    assertFalse(Files.exists(child.resolve("content.txt")));
    assertFalse(Files.exists(child.resolve("Leaf.wiki")));
    String rootPage = Files.readString(root.resolve("_root.md"));
    assertTrue(rootPage.contains("\"type\": \"suite\""));
    assertTrue(rootPage.contains("contents"));
    String childPage = Files.readString(root.resolve("Child.md"));
    assertTrue(childPage.contains("\"type\": \"test\""));
    assertTrue(childPage.contains("| script |"));
    String leaf = Files.readString(child.resolve("Leaf.md"));
    assertTrue(leaf.contains("## Hi"));
    assertTrue(leaf.contains("\"type\": \"test\""));
    assertTrue(leaf.contains("\"help\": \"the leaf\""));
  }
}
