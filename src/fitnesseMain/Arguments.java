// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesseMain;

import fitnesse.ContextConfigurator;
import util.CommandLine;

import static fitnesse.ConfigurationParameter.*;
import static fitnesse.ContextConfigurator.*;

public class Arguments {

  private final CommandLine commandLine = new CommandLine(
          "[-v][-p port][-d dir][-r root][-f config][-c command][-b output][--listen address]");

  private final String rootPath;
  private final Integer port;
  private final String rootDirectory;
  private final String command;
  private final String output;
  private final String configFile;
  private final boolean verboseLogging;
  private final String listenAddress;

  public Arguments(String... args) {
    if (!commandLine.parse(args)) {
      throw new IllegalArgumentException("Can not parse command line");
    }
    String port = commandLine.getOptionArgument("p", "port");
    this.port = port != null ? Integer.valueOf(port) : null;
    this.rootPath = commandLine.getOptionArgument("d", "dir");
    this.rootDirectory = commandLine.getOptionArgument("r", "root");
    this.command = commandLine.getOptionArgument("c", "command");
    this.output = commandLine.getOptionArgument("b", "output");
    this.configFile = commandLine.getOptionArgument("f", "config");
    this.verboseLogging = commandLine.hasOption("v");
    this.listenAddress = commandLine.getOptionArgument("listen", "address");
  }

  static void printUsage() {
    ContextConfigurator defaults = ContextConfigurator.systemDefaults();

    System.err.println("Usage: java -jar fitnesse.jar [-vpdrfcb] [--listen address]");
    System.err.println("\t-p <port number> {" + DEFAULT_PORT + "}");
    System.err.println("\t-d <working directory> {" +
      defaults.get(ROOT_PATH) + "}");
    System.err.println("\t-r <page root directory> {" +
      defaults.get(ROOT_DIRECTORY) + "}");
    System.err.println("\t-f <config properties file> {" +
      defaults.get(CONFIG_FILE) + "}");
    System.err.println("\t-c <command> execute single command.");
    System.err.println("\t-b <filename> redirect command output.");
    System.err.println("\t-v {off} Verbose logging");
    System.err.println("\t--listen <address> {" + DEFAULT_LISTEN + "} Bind to this address. 0.0.0.0 listens on all interfaces.");
  }

  public String getRootPath(ContextConfigurator configurator) {
    return rootPath == null ? configurator.get(ROOT_PATH) : rootPath;
  }

  public String getConfigFile(ContextConfigurator configurator) {
    return configFile == null ? (getRootPath(configurator) + "/" + configurator.get(CONFIG_FILE)) : configFile;
  }

  public ContextConfigurator update(ContextConfigurator defaults) {
    ContextConfigurator result = defaults;

    result = result.withParameter(LOG_LEVEL, verboseLogging ? "verbose" : "normal");
    if (configFile != null)
      result = result.withParameter(CONFIG_FILE, configFile);
    if (port != null)
      result = result.withPort(port);
    if (rootPath != null)
      result = result.withRootPath(rootPath);
    if (rootDirectory != null)
      result = result.withRootDirectoryName(rootDirectory);
    if (output != null)
      result = result.withParameter(OUTPUT, output);
    if (command != null)
      result = result.withParameter(COMMAND, command);
    if (listenAddress != null)
      result = result.withParameter(LISTEN, listenAddress);

    return result;
  }

}
