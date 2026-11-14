package fitnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** JUnit XML built from a results document, for CI. */
public final class JunitXmlWriter {
  private JunitXmlWriter() {
  }

  public static String xml(JSONObject document) {
    JSONObject run = document.getJSONObject("run");
    JSONArray pages = run.getJSONArray("pages");
    int failures = 0;
    int errors = 0;
    StringBuilder cases = new StringBuilder();
    for (int i = 0; i < pages.length(); i++) {
      JSONObject page = pages.getJSONObject(i);
      String status = page.getString("status");
      if ("fail".equals(status) || "empty".equals(status)) {
        failures++;
      } else if ("error".equals(status)) {
        errors++;
      }
      cases.append(testCase(page));
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<testsuite name=\"" + escape(run.optString("name", run.optString("id")))
      + "\" tests=\"" + pages.length()
      + "\" failures=\"" + failures + "\" errors=\"" + errors
      + "\" time=\"" + seconds(run.optLong("durationMillis")) + "\">\n"
      + cases + "</testsuite>\n";
  }

  public static void write(JSONObject document, Path file) throws IOException {
    if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    Files.writeString(file, xml(document), StandardCharsets.UTF_8);
  }

  private static String testCase(JSONObject page) {
    String name = escape(page.getString("path"));
    String status = page.getString("status");
    String open = "  <testcase name=\"" + name + "\" classname=\"" + name
      + "\" time=\"" + seconds(page.optLong("durationMillis")) + "\"";
    if (!RunDocument.failed(status)) {
      return open + "/>\n";
    }
    String detail = FailureText.of(page);
    String message = detail.contains("\n") ? detail.substring(0, detail.indexOf('\n')) : detail;
    String tag = "error".equals(status) ? "error" : "failure";
    return open + ">\n    <" + tag + " message=\"" + escape(message) + "\">"
      + escape(detail) + "</" + tag + ">\n  </testcase>\n";
  }

  private static String seconds(long millis) {
    return String.format(Locale.US, "%.3f", millis / 1000.0);
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
