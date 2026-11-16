// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse;

import agnesse.components.Logger;
import agnesse.html.template.PageFactory;
import agnesse.reporting.FormatterFactory;
import agnesse.responders.ResponderFactory;
import agnesse.testrunner.run.TestRunFactoryRegistry;
import agnesse.testsystems.TestSystemFactory;
import agnesse.testsystems.TestSystemListener;
import agnesse.util.StringUtils;
import agnesse.wiki.PathParser;
import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.UrlPathVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageFactory;
import agnesse.wiki.fs.WikiFiles;
import agnesse.wiki.VariableSource;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class FitNesseContext {
  public static final String rfcCompliantDateFormat = "EEE, d MMM yyyy HH:mm:ss Z";

  public final FitNesseVersion version;
  public final FitNesse fitNesse;

  public final TestSystemFactory testSystemFactory;
  public final TestSystemListener testSystemListener;
  public final TestRunFactoryRegistry testRunFactoryRegistry;

  public final FormatterFactory formatterFactory;

  public final int port;
  private final WikiPageFactory wikiPageFactory;
  public final String rootPath;
  private final String rootDirectoryName;
  public final Integer maximumWorkers;
  public final ResponderFactory responderFactory;
  public final PageFactory pageFactory;

  public final SystemVariableSource variableSource;
  public final WikiFiles versionsController;
  public final Logger logger;
  private final Properties properties;

  protected FitNesseContext(FitNesseVersion version, WikiPageFactory wikiPageFactory, String rootPath,
                            String rootDirectoryName, int maximumWorkers,
                            WikiFiles versionsController, int port,
                            Logger logger,
                            TestSystemFactory testSystemFactory, TestSystemListener testSystemListener,
                            FormatterFactory formatterFactory,
                            Properties properties,
                            SystemVariableSource variableSource) {
    super();
    this.version = version;
    this.wikiPageFactory = wikiPageFactory;
    this.rootPath = rootPath;
    this.rootDirectoryName = rootDirectoryName;
    this.maximumWorkers = maximumWorkers;
    this.versionsController = versionsController;
    this.port = port;
    this.logger = logger;
    this.testSystemFactory = testSystemFactory;
    this.testSystemListener = testSystemListener;
    this.formatterFactory = formatterFactory;
    this.properties = properties;
    responderFactory = new ResponderFactory(getRootPagePath());
    this.variableSource = variableSource;
    fitNesse = new FitNesse(this);
    pageFactory = new PageFactory(this);
    testRunFactoryRegistry = new TestRunFactoryRegistry(this);
  }

  public WikiPage getRootPage() {
    return getRootPage(variableSource);
  }

  public WikiPage getRootPage(Map<String, String> customProperties) {
    return getRootPage(new UrlPathVariableSource(variableSource, customProperties));
  }

  public WikiPage getPage(String pageName, Map<String, String> customProperties) {
    return getRootPage(customProperties).getPageCrawler().getPage(PathParser.parse(pageName));
  }

  private WikiPage getRootPage(VariableSource variableSource) {
    return wikiPageFactory.makePage(new File(rootPath, rootDirectoryName), rootDirectoryName, null, variableSource);

  }
  public String getTestProgressPath() {
    return String.format(unifiedPathPattern("%s/files/testProgress"), getRootPagePath());
  }

  public String getRootPagePath() {
    return String.format(unifiedPathPattern("%s/%s"), rootPath, rootDirectoryName);
  }

  public Properties getProperties() {
    return properties;
  }

  public String getProperty(String name) {
    return variableSource.getProperty(name);
  }

  private String unifiedPathPattern(String s) {
    return StringUtils.replace(s, "/", File.separator);
  }
}
