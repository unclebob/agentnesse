// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.fixtures;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import agnesse.testutil.AgNesseUtil;
import util.FileUtil;

import static agnesse.fixtures.AgNesseFixtureContext.context;

public class SetUp {
  public SetUp() throws Exception {
    this(new Properties());
  }

  public SetUp(String configuration) throws Exception {
    this(asProperties(configuration));
  }

  private SetUp(Properties properties) throws Exception {
    final int port = 9123;
    properties.setProperty("AGNESSE_PORT", String.valueOf(port));
    context = AgNesseUtil.makeTestContext(port, properties);
    AgNesseUtil.startAgNesseWithContext(context);
  }

  private static Properties asProperties(String configuration) throws Exception {
    Properties properties = new Properties();
    properties.load(new ByteArrayInputStream(configuration.getBytes(FileUtil.CHARENCODING)));
    return properties;
  }
}
