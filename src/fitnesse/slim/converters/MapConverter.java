package fitnesse.slim.converters;

import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlUtil;
import fitnesse.slim.Converter;
import fitnesse.testsystems.slim.HtmlTable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MapConverter implements Converter<Map> {

  @Override
  public String toString(Map hash) {
    if (hash == null) {
      return NULL_VALUE;
    }
    return createTag(hash).html().trim();
  }

  protected HtmlTag createTag(Map<?, ?> hash) {
    HtmlTag table = new HtmlTag("table");
    table.addAttribute("class", "hash_table");
    for (Map.Entry<?, ?> entry : hash.entrySet()) {
      HtmlTag row = new HtmlTag("tr");
      row.addAttribute("class", "hash_row");
      table.add(row);

      HtmlTag keyCell = new HtmlTag("td");
      addCellContent(keyCell, entry.getKey());
      keyCell.addAttribute("class", "hash_key");
      row.add(keyCell);

      HtmlTag valueCell = new HtmlTag("td");
      addCellContent(valueCell, entry.getValue());
      valueCell.addAttribute("class", "hash_value");
      row.add(valueCell);
    }
    return table;
  }

  protected void addCellContent(HtmlTag valueCell, Object cellValue) {
    String valueToAdd = ElementConverterHelper.elementToString(cellValue);
    if (!HtmlUtil.isValidTableCellContent(valueToAdd) && !HtmlTable.qualifiesAsConvertedList(valueToAdd))
      valueToAdd = HtmlUtil.escapeHTML(valueToAdd);

    valueCell.add(valueToAdd.trim());
  }

  @Override
  public Map<String, String> fromString(String source) {
    Map<String, String> map = new HashMap<>();
    if (source == null) {
      return map;
    }
    String text = source.trim();
    if (!text.startsWith("{") || !text.endsWith("}")) {
      return map;
    }
    try {
      JSONObject object = new JSONObject(text);
      for (String key : object.keySet()) {
        Object value = object.get(key);
        map.put(key, value == null || JSONObject.NULL.equals(value) ? null : String.valueOf(value));
      }
    } catch (RuntimeException ignored) {
      return new HashMap<>();
    }
    return map;
  }
}
