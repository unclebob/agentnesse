# AgentNesse

AgentNesse is a fork of [FitNesse](https://github.com/unclebob/fitnesse). It is for AI agents that write and run acceptance tests. The mission is a minimal, agent-friendly acceptance-testing system.

FitNesse accumulated a wiki, two test systems, page history, its own version store, plugins, and authentication. An agent does not need that. It needs pages it can read and write, Slim tables it can fill in, and a run whose result it can read. AgentNesse is that, and nothing else.

This tree is early and under active rework, phase by phase. What you find here is not the finished tool. Features below marked planned are not built yet.

## Direction

Slim only. Fit is gone.

No test history. A run returns its results. Nothing is kept afterward.

No page versions inside the tool. Pages are written in place. Git is the history.

Pages can be Markdown files with JSON front matter. The pages already in this tree are still FitNesse wiki text. Samples of the new syntax are under `AgentNesseSamples`.

Slim tables are bare GitHub-flavored Markdown tables. The table type is the name in the first header cell, for example `decision:Division`. A name with no prefix is a decision table.

Commands that are not tables go in fenced `agentnesse` blocks.

Run results are JSON. A JUnit XML file is written from that JSON, for CI. (Planned. HTTP `format=xml` and `format=junit` are already gone. The `fitnesse.junit` runner still writes JUnit XML on its own, and it does not read test history.)

A command line runs a page or a suite and exits with the failure count. (The current entry point does this. A thinner agent CLI is planned.)

A thin MCP server lists pages, runs a page or suite (with tags, and a rerun of failures), and returns the JSON results. (Planned.)

No plugins. No authentication. One author. (Planned. Plugins and authentication are still in this tree.)

## Build and run

AgentNesse requires Java 21. That is the version the project compiles for, and the version the wiki and the Slim server run on.

Build and test:

```
./gradlew build
```

Start the wiki on port 8001. It listens on 127.0.0.1. `--listen 0.0.0.0` listens on every interface. Any other `--listen` address binds that address.

```
./gradlew run
java -jar build/libs/fitnesse-*-standalone.jar -p 8001 --listen 127.0.0.1
```

Configuration is `agentnesse.json` in the working directory. If that file is missing, the built-in defaults are used. A null value leaves that default too. `-f` names a different file, and that file must exist. A command-line flag overrides the file. The port default is 80. A `-c` command uses port 9123 when port is left unset.

```json
{
  "port": null,
  "rootPath": ".",
  "pageRoot": "FitNesseRoot",
  "logDirectory": null,
  "maximumWorkers": 100,
  "theme": "bootstrap",
  "refactorStartPath": null,
  "listen": "127.0.0.1"
}
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Credit and license

AgentNesse is derived from FitNesse, originally at <https://github.com/unclebob/fitnesse>. The license is unchanged: Common Public License 1.0, in `LICENSE.txt`.
