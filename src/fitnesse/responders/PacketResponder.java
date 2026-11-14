package fitnesse.responders;

import fitnesse.Responder;
import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.testsystems.slim.MarkdownTableScanner;
import fitnesse.testsystems.slim.Table;
import fitnesse.testsystems.slim.TableScanner;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PacketResponder implements Responder {
  private SimpleResponse response;
  private WikiPage page;
  private JSONObject packet;
  List<JSONObject> tables = new ArrayList<>();

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    response = new SimpleResponse();
    response.setContentType(Response.Format.JSON);
    String pageName = request.getResource();
    PageCrawler pageCrawler = context.getRootPage().getPageCrawler();
    WikiPagePath resourcePath = PathParser.parse(pageName);
    page = pageCrawler.getPage(resourcePath);

    if (page == null)
      response.setStatus(404);
    else {
      buildPacket();
      response.setStatus(200);
    }
    return response;
  }

  private void buildPacket() throws UnsupportedEncodingException {
    packet = new JSONObject();
    TableScanner scanner = new MarkdownTableScanner(page.getData().getContent());

    addTablesToPacket(scanner);
    packet.put("page", page.getName());
    response.setContent(packet.toString(2));
  }

  private void addTablesToPacket(TableScanner scanner) throws JSONException {
    for (int i = 0; i < scanner.getTableCount(); i++) {
      Table t = scanner.getTable(i);
      addTableToPacket(t);
    }
    packet.put("tables", tables);
  }

  private void addTableToPacket(Table t) throws JSONException {
    JSONObject table = new JSONObject();
    JSONArray rows = new JSONArray();
    for (int row = 0; row < t.getRowCount(); row++) {
      rows.put(new JSONArray(getRowFromTable(t, row)));
    }
    table.put("rows", rows);
    tables.add(table);
  }

  private List<String> getRowFromTable(Table t, int row) {
    List<String> rowList = new ArrayList<>();
    for (int col = 0; col < t.getColumnCountInRow(row); col++)
      rowList.add(t.getCellContents(col, row));
    return rowList;
  }

}
