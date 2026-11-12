package fitnesse.markdown.convert;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WikiTree {
  private WikiTree() {
  }

  public static List<String> convert(Path wikiRoot, Path page) throws IOException {
    List<String> notes = new ArrayList<>();
    convertPage(wikiRoot, page, notes);
    return notes;
  }

  private static void convertPage(Path wikiRoot, Path dir, List<String> notes) throws IOException {
    for (Path child : children(dir)) {
      if (Files.isDirectory(child)) {
        convertPage(wikiRoot, child, notes);
      } else if (child.getFileName().toString().endsWith(".wiki")) {
        convertWiki(child, notes);
      }
    }
    if (Files.exists(dir.resolve("content.txt"))) {
      convertContent(wikiRoot, dir, notes);
    }
  }

  private static void convertContent(Path wikiRoot, Path dir, List<String> notes) throws IOException {
    boolean root = dir.equals(wikiRoot);
    String name = root ? "_root" : dir.getFileName().toString();
    write(root ? dir.resolve("_root.md") : dir.getParent().resolve(name + ".md"),
      dir.resolve("content.txt"), dir.resolve("properties.xml"), name, notes);
    Files.delete(dir.resolve("content.txt"));
    Files.deleteIfExists(dir.resolve("properties.xml"));
    if (!root && directoryIsEmpty(dir)) {
      Files.delete(dir);
    }
  }

  private static void convertWiki(Path wiki, List<String> notes) throws IOException {
    String file = wiki.getFileName().toString();
    String name = file.substring(0, file.length() - ".wiki".length());
    write(wiki.resolveSibling(name + ".md"), wiki, null, name, notes);
    Files.delete(wiki);
  }

  private static void write(Path markdown, Path source, Path properties, String name, List<String> notes)
      throws IOException {
    List<String> pageNotes = new ArrayList<>();
    String props = properties != null && Files.exists(properties) ? Files.readString(properties) : "";
    Files.writeString(markdown, WikiToMarkdown.convert(name, Files.readString(source), props, pageNotes));
    for (String note : pageNotes) {
      notes.add(markdown.getFileName() + ": " + note);
    }
  }

  private static List<Path> children(Path dir) throws IOException {
    List<Path> children = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path child : stream) {
        children.add(child);
      }
    }
    return children;
  }

  private static boolean directoryIsEmpty(Path dir) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return !stream.iterator().hasNext();
    }
  }
}
