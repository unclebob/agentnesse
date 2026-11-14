package fitnesse;

import fitnesseMain.Arguments;
import fitnesseMain.FitNesseMain;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * A suite run returns its results and must not leave files under FitNesseRoot.
 */
public class FitNesseRootUnchangedBySuiteRunTest {
  @Test
  public void suiteRunLeavesFitNesseRootUnchanged() throws Exception {
    Path root = Path.of("FitNesseRoot");
    Path suite = root.resolve("NoFilesLeftBehind");
    try {
      writePassingSuite(suite);
      Map<String, String> before = snapshot(root);
      PrintStream out = System.out;
      PrintStream err = System.err;
      ByteArrayOutputStream captured = new ByteArrayOutputStream();
      PrintStream capture = new PrintStream(captured, true, StandardCharsets.UTF_8);
      System.setOut(capture);
      System.setErr(capture);
      int exitCode;
      try {
        exitCode = new FitNesseMain().launchFitNesse(new Arguments(
          "-c", "NoFilesLeftBehind?suite"));
      } finally {
        System.setOut(out);
        System.setErr(err);
      }
      assertEquals(captured.toString(StandardCharsets.UTF_8), 0, exitCode);
      String changes = diff(before, snapshot(root));
      assertEquals(changes, "", changes);
    } finally {
      deleteTree(suite);
      Files.deleteIfExists(suite.getParent().resolve(suite.getFileName() + ".md"));
    }
  }

  private static void writePassingSuite(Path suite) throws IOException {
    Files.createDirectories(suite);
    Files.writeString(suite.getParent().resolve(suite.getFileName() + ".md"), """
      { "type": "suite" }
      A suite that must not leave files behind
      """);
    Files.writeString(suite.resolve("SampleTest.md"), """
      { "type": "test" }
      ```agentnesse
      define TEST_SYSTEM
      slim
      ```

      | script | fitnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | hello | hello |
      """);
  }

  private static Map<String, String> snapshot(Path root) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    Map<String, String> files = new TreeMap<>();
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        digest.reset();
        files.put(root.relativize(file).toString(), hex(digest.digest(Files.readAllBytes(file))));
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  private static String diff(Map<String, String> before, Map<String, String> after) {
    StringBuilder changes = new StringBuilder();
    for (Map.Entry<String, String> entry : before.entrySet()) {
      String now = after.get(entry.getKey());
      if (now == null) {
        changes.append("removed ").append(entry.getKey()).append('\n');
      } else if (!now.equals(entry.getValue())) {
        changes.append("changed ").append(entry.getKey()).append('\n');
      }
    }
    for (String path : after.keySet()) {
      if (!before.containsKey(path)) {
        changes.append("added ").append(path).append('\n');
      }
    }
    return changes.toString();
  }

  private static String hex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      builder.append(Character.forDigit((b >> 4) & 0xF, 16));
      builder.append(Character.forDigit(b & 0xF, 16));
    }
    return builder.toString();
  }

  private static void deleteTree(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
