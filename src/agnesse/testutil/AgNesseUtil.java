// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.testutil;

import agnesse.ContextConfigurator;
import agnesse.AgNesse;
import agnesse.AgNesseContext;
import agnesse.socketservice.PlainServerSocketFactory;
import agnesse.wiki.WikiPageFactory;
import agnesse.wiki.fs.FileSystem;
import agnesse.wiki.fs.WikiFiles;
import agnesse.wiki.fs.InMemoryPage;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class AgNesseUtil {
  public static final String base = "RooT";
  public static final int PORT = 1999;
  public static final String URL = "http://localhost:" + PORT + "/";

  private static AgNesse instance = null;

  public static void startAgNesseWithContext(AgNesseContext context) throws IOException {
    instance = context.agNesse;
    instance.start(new PlainServerSocketFactory().createServerSocket(context.port));
  }

  public static void stopAgNesse() throws IOException {
    instance.stop();
    FileUtil.deleteFileSystemDirectory(base);
  }

  public static AgNesseContext makeTestContext() {
    Properties properties = new Properties();
    properties.setProperty("AGNESSE_PORT", String.valueOf(PORT));
    return makeTestContext(InMemoryPage.newInstance(), properties);
  }

  public static AgNesseContext makeTestContext(Properties properties) {
    return makeTestContext(InMemoryPage.newInstance(), properties);
  }

  public static AgNesseContext makeTestContext(WikiPageFactory wikiPageFactory, Properties properties) {
    File temporaryFolder = createTemporaryFolder();
    return makeTestContext(wikiPageFactory, temporaryFolder.getPath(), AgNesseUtil.base, PORT, properties);
  }

  public static AgNesseContext makeTestContext(int port) {
    return makeTestContext(InMemoryPage.newInstance(), createTemporaryFolder().getPath(), AgNesseUtil.base, port, new Properties());
  }

  public static AgNesseContext makeTestContext(int port, Properties properties) {
    return makeTestContext(InMemoryPage.newInstance(), createTemporaryFolder().getPath(), AgNesseUtil.base, port, properties);
  }

  public static AgNesseContext makeTestContext(FileSystem fileSystem) {
    return makeTestContext(InMemoryPage.newInstance(fileSystem), createTemporaryFolder().getPath(), AgNesseUtil.base, PORT, new Properties());
  }

  public static AgNesseContext makeTestContext(WikiPageFactory wikiPageFactory, String rootPath, String name, int port) {
    return makeTestContext(wikiPageFactory, rootPath, name, port, new Properties());
  }

  public static AgNesseContext makeTestContext(WikiPageFactory wikiPageFactory, String rootPath,
                                                String rootDirectoryName, int port, Properties properties) {
    AgNesseContext context;

    try {
      context = ContextConfigurator.systemDefaults()
              .withWikiPageFactory(wikiPageFactory)
              .withRootPath(rootPath)
              .withRootDirectoryName(rootDirectoryName)
              .withPort(port)
              .withVersionsController(new WikiFiles())
              .updatedWith(properties)
              .makeAgNesseContext();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    // Ensure Velocity is configured with the default root directory name (AgNesseRoot)
    context.pageFactory.getVelocityEngine();
    return context;
  }

  public static File createTemporaryFolder() {
    File createdFolder;
    try {
      createdFolder = File.createTempFile("agnesse", "");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create temporary folder for test execution", e);
    }
    createdFolder.delete();
    createdFolder.mkdir();
    return createdFolder;
  }

  public static void destroyTestContext(AgNesseContext context) throws IOException {
    FileUtil.deleteFileSystemDirectory(context.rootPath);
  }

  public static void destroyTestContext() throws IOException {
    FileUtil.deleteFileSystemDirectory(AgNesseUtil.base);
  }
}
