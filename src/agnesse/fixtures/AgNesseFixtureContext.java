// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.fixtures;

import agnesse.AgNesseContext;
import agnesse.http.MockResponseSender;
import agnesse.http.Response;
import agnesse.wiki.WikiPage;

public class AgNesseFixtureContext {
  static WikiPage page;
  static Response response;
  static MockResponseSender sender;
  static AgNesseContext context;
}
