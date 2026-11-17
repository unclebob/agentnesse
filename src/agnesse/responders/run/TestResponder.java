package agnesse.responders.run;

import agnesse.AgNesseContext;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.responders.ErrorResponder;
import agnesse.wiki.TestAdjunct;
import agnesse.wiki.WikiPage;

import java.util.List;

import static agnesse.wiki.WikiPageUtil.isTestPage;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TestResponder extends SuiteResponder {
  static final String DOES_NOT_RUN = "A test-adjunct page does not run on its own.";

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    Response result = super.makeResponse(context, request);
    if (result == response && TestAdjunct.is(page)) {
      return new ErrorResponder(DOES_NOT_RUN).makeResponse(context, request);
    }
    return result;
  }

  @Override
  protected List<WikiPage> getPagesToRun() {
    if (isTestPage(page)) {
      return singletonList(page);
    } else {
      return emptyList();
    }
  }

}
