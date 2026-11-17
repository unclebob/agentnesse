package agnesse.responders.editing;

import agnesse.html.HtmlUtil;
import agnesse.html.template.HtmlPage;

final class EditForm {
  private EditForm() {
  }

  static void putFields(HtmlPage html, String content, String frontMatter, String error) {
    html.put(EditResponder.CONTENT_INPUT_NAME, HtmlUtil.escapeHTML(empty(content)));
    html.put("frontMatter", HtmlUtil.escapeHTML(empty(frontMatter)));
    if (error != null) {
      html.put("error", error);
      html.put("frontMatterOpen", Boolean.TRUE);
    }
  }

  private static String empty(String value) {
    return value == null ? "" : value;
  }
}
