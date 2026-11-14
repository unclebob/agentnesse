package fitnesse.results;

import fitnesse.testsystems.ExecutionResult;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Page counts are the assertion cells, plus table failures.
 * A table failure is an error: the page is not a pass.
 * Assertion wrongs still outrank that and make the page a fail.
 * A non-meta page with neither assertions nor table failures is empty.
 */
public final class PageCounts {
  private PageCounts() {
  }

  public static JSONObject counts(JSONArray blocks) {
    int right = 0;
    int wrong = 0;
    int ignored = 0;
    int exceptions = 0;
    int tableFailures = 0;
    for (int i = 0; i < blocks.length(); i++) {
      JSONObject block = blocks.getJSONObject(i);
      JSONObject blockCounts = block.getJSONObject("counts");
      right += blockCounts.getInt("right");
      wrong += blockCounts.getInt("wrong");
      ignored += blockCounts.getInt("ignored");
      exceptions += blockCounts.getInt("exceptions");
      if ("table-failure".equals(block.optString("status", ""))) {
        tableFailures++;
      }
    }
    return counts(right, wrong, ignored, exceptions, tableFailures);
  }

  public static JSONObject counts(int right, int wrong, int ignored, int exceptions, int tableFailures) {
    return new JSONObject()
      .put("right", right)
      .put("wrong", wrong)
      .put("ignored", ignored)
      .put("exceptions", exceptions)
      .put("tableFailures", tableFailures);
  }

  public static String status(String pageName, JSONObject counts) {
    if (counts.getInt("wrong") > 0) {
      return "fail";
    }
    if (counts.getInt("exceptions") > 0 || counts.getInt("tableFailures") > 0) {
      return "error";
    }
    if (!ExecutionResult.isSuiteMetaPage(pageName) && none(counts)) {
      return "empty";
    }
    if (counts.getInt("ignored") > 0 && counts.getInt("right") == 0) {
      return "ignore";
    }
    return "pass";
  }

  public static JSONObject add(JSONObject total, JSONObject page) {
    return counts(
      total.getInt("right") + page.getInt("right"),
      total.getInt("wrong") + page.getInt("wrong"),
      total.getInt("ignored") + page.getInt("ignored"),
      total.getInt("exceptions") + page.getInt("exceptions"),
      total.getInt("tableFailures") + page.getInt("tableFailures"));
  }

  private static boolean none(JSONObject counts) {
    return counts.getInt("right") == 0 && counts.getInt("wrong") == 0
      && counts.getInt("ignored") == 0 && counts.getInt("exceptions") == 0
      && counts.getInt("tableFailures") == 0;
  }
}
