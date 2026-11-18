![AgNesse](src/agnesse/resources/images/agnesse-logo.png)

# AgNesse

AgNesse is a Slim acceptance-test wiki for an agent that writes pages and reads results. It is a fork of [FitNesse](https://github.com/unclebob/fitnesse), cut down to that job.

Pages are Markdown files with JSON front matter. Slim tables are GitHub-flavored Markdown tables. The table type is the first header cell, for example `decision:Division`. A name with no prefix is a decision table. Commands that are not tables go in fenced `agnesse` blocks.

There is one test system, Slim. There is no plugin loader and no authentication. Pages are written in place. Git is the history of the pages. A run returns JSON and does not keep a test history.

## Build and run

AgNesse requires Java 21. The Gradle wrapper builds it.

```
./gradlew test
./gradlew acceptanceTest
./gradlew build
```

`test` is the unit suite. `acceptanceTest` runs `AgNesse.SuiteAcceptanceTests` and writes `build/reports/acceptance-results.json`. `build` runs both, checks that JSON against the results schema, and enforces the coverage floors and the removed-name check. On Windows, use `gradlew.bat`.

Start the wiki on port 8001. It listens on 127.0.0.1.

```
./gradlew run
java -jar build/libs/agnesse-*-standalone.jar -p 8001
```

The standalone jar's main class is `agnesseMain.AgNesseMain`. `--listen 0.0.0.0` listens on every interface. Any other `--listen` address binds that address.

## JSON results

`-c` runs one page or suite and prints JSON. The process exits with the number of failing pages. `-b` names the output file instead of standard output.

```
java -jar build/libs/agnesse-*-standalone.jar -c AgNesse.SuiteAcceptanceTests?suite -b results.json
```

The document has `"schemaVersion": 1`. A page that passed has `"status": "pass"`. The same run writes `.agnesse/last-failures.json` beside the wiki root. `--rerun-failures` points at that file, or at a full results document, and runs only the failed pages.

When `-c` is used and the port is left unset, the run uses port 9123. The wiki's port default is 80.

The `agnesse.junit` runner writes JUnit XML for its own runs. `-c` does not. HTTP `format=xml` and `format=junit` are gone.

## MCP

`-mcp` speaks MCP on standard input and output. It does not open a port. The tools are `list_pages`, `run`, and `get_results`. `get_results` returns schema version 1 for a run id, kept until that process exits. Pages are files. `-mcp` and `-c` together exit 1.

```json
{
  "mcpServers": {
    "agnesse": {
      "command": "java",
      "args": ["-jar", "agnesse.jar", "-mcp"]
    }
  }
}
```

## Configuration

`agnesse.json` in the working directory supplies the defaults. If that file is missing, the built-in defaults are used. `-f` names a different file, and that file must exist. A null value leaves the default. A command-line flag overrides the file. Unknown keys are an error.

```json
{
  "port": null,
  "rootPath": ".",
  "pageRoot": "AgNesseRoot",
  "maximumWorkers": 100,
  "refactorStartPath": null,
  "listen": "127.0.0.1"
}
```

`port` and `maximumWorkers` are integers. `listen`, `rootPath`, `pageRoot`, and `refactorStartPath` are strings.

Other flags: `-v` turns on verbose logging, `-d` is the working directory, `-r` is the page root.

## User guide

The guide is `AgNesseRoot/AgNesse/UserGuide.md`, wiki path `AgNesse.UserGuide`. It includes the pages under `AgNesse.SuiteAcceptanceTests.SuiteUserGuide`, so the guide and the suite stay the same text: front matter, define, include, scenarios, path, contents, alias, Java Slim, Python Slim, the command line, MCP, JSON errors, and rename.

## Credit and license

AgNesse is derived from FitNesse, originally at <https://github.com/unclebob/fitnesse>. The license is unchanged: Common Public License 1.0, in `LICENSE.txt`.
