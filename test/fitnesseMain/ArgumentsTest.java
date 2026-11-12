// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesseMain;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

import fitnesse.ContextConfigurator;
import org.junit.Test;

public class ArgumentsTest {

  @Test(expected = IllegalArgumentException.class)
  public void testBadArgument() throws Exception {
    new Arguments("-x");
  }

  @Test
  public void defaultConfigLocation() {
    Arguments args = new Arguments();
    assertEquals("./plugins.properties", args.getConfigFile(ContextConfigurator.systemDefaults()));
  }

  @Test
  public void configLocationWithDifferentRootPath() {
    Arguments args = new Arguments("-d", "customDir");
    assertEquals("customDir/plugins.properties", args.getConfigFile(ContextConfigurator.systemDefaults()));
  }

  @Test
  public void customConfigLocation() {
    Arguments args = new Arguments("-f", "custom.properties");
    assertEquals("custom.properties", args.getConfigFile(ContextConfigurator.systemDefaults()));
  }

  @Test
  public void argumentsCanBeRepresentedByProperties() throws IOException {
    Arguments args = new Arguments("-v", "-p", "81", "-d", "directory", "-r", "root", "-b", "someFile.txt",
              "-f", "fitnesse.properties", "-c", "SomeCommand", "--all-interfaces");
    Properties properties = args.update(ContextConfigurator.systemDefaults()).makeFitNesseContext().getProperties();

    assertEquals("verbose", properties.getProperty("LogLevel"));
    assertEquals("81", properties.getProperty("Port"));
    assertEquals("directory", properties.getProperty("RootPath"));
    assertEquals("root", properties.getProperty("FitNesseRoot"));
    assertEquals("someFile.txt", properties.getProperty("RedirectOutput"));
    assertEquals("fitnesse.properties", properties.getProperty("ConfigFile"));
    assertEquals("SomeCommand", properties.getProperty("Command"));
    assertEquals("false", properties.getProperty("LocalhostOnly"));
  }

  @Test
  public void bindsLoopbackUnlessAllInterfacesIsRequested() throws IOException {
    Properties defaults = new Arguments().update(ContextConfigurator.systemDefaults()).makeFitNesseContext().getProperties();
    assertEquals("true", defaults.getProperty("LocalhostOnly"));

    Properties all = new Arguments("--all-interfaces").update(ContextConfigurator.systemDefaults()).makeFitNesseContext().getProperties();
    assertEquals("false", all.getProperty("LocalhostOnly"));
  }

  @Test
  public void defaultArgumentsAsProperties() throws IOException {
    Arguments args = new Arguments();
    Properties properties = args.update(ContextConfigurator.empty()).makeFitNesseContext().getProperties();

    assertEquals("normal", properties.getProperty("LogLevel"));
    assertNull(properties.getProperty("ConfigFile"));
    assertEquals(".", properties.getProperty("RootPath"));
    assertEquals("FitNesseRoot", properties.getProperty("FitNesseRoot"));
    assertNull(properties.getProperty("RedirectOutput"));
    assertNull(properties.getProperty("Command"));
    assertEquals("true", properties.getProperty("LocalhostOnly"));
    assertEquals("100", properties.getProperty("MaximumWorkers"));
  }

}
