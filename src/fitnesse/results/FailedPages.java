package fitnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads a failed-page list from the small file or from a full results document. */
public final class FailedPages {
  private FailedPages() {
  }

  public static File defaultFile(String rootPagePath) {
    File root = new File(rootPagePath);
    File parent = root.getParentFile() == null ? root : root.getParentFile();
    return new File(parent, ".agentnesse/last-failures.json");
  }

  public static List<String> read(Path file) throws IOException {
    JSONObject json = new JSONObject(Files.readString(file, StandardCharsets.UTF_8));
    JSONArray pages = json.has("run") ? json.getJSONObject("run").getJSONArray("failedPages") : json.getJSONArray("failedPages");
    List<String> paths = new ArrayList<>();
    for (int i = 0; i < pages.length(); i++) {
      paths.add(pages.getString(i));
    }
    return paths;
  }
}
