package fitnesse.markdown;

import java.util.ArrayList;
import java.util.List;

final class PathLines {
  private boolean open;
  private boolean seen;
  private boolean path;

  private PathLines() {
  }

  static List<String> from(String content) {
    PathLines scan = new PathLines();
    List<String> paths = new ArrayList<>();
    for (String line : content.split("\n", -1)) {
      scan.read(line, paths);
    }
    return paths;
  }

  private void read(String line, List<String> paths) {
    if (line.startsWith("```")) {
      open = !open;
      seen = false;
      path = false;
      return;
    }
    if (open && !seen) {
      seen = true;
      path = line.trim().equals("path") || line.trim().startsWith("path ");
      if (path) {
        add(paths, line.trim().substring("path".length()));
      }
      return;
    }
    if (open && path) {
      add(paths, line);
    } else if (!open) {
      addBang(paths, line);
    }
  }

  private static void add(List<String> paths, String line) {
    if (!line.isBlank()) {
      paths.add(line.trim());
    }
  }

  private static void addBang(List<String> paths, String line) {
    String trimmed = line.trim();
    if (trimmed.startsWith("!path")) {
      add(paths, trimmed.substring("!path".length()));
    }
  }
}
