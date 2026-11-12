// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders;

import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.wiki.WikiPage;

public class PageDataWikiPageResponder extends BasicResponder {
  @Override
  protected String contentFrom(FitNesseContext context, Request request, WikiPage requestedPage) {
    return requestedPage.getData().getContent();
  }

}
