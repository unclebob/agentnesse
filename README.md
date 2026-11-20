![AgNesse](src/agnesse/resources/images/agnesse-logo.png)

# AgNesse

AgNesse is a Slim acceptance-test wiki for an agent that writes pages and reads results. It is a fork of [FitNesse](https://github.com/unclebob/fitnesse), cut down to that job.

Pages are Markdown files with JSON front matter. Slim tables are GitHub-flavored Markdown tables. The table type is the first header cell, for example `decision:Division`. A name with no prefix is a decision table. Commands that are not tables go in fenced `agnesse` blocks.

There is one test system, Slim. There is no plugin loader and no authentication. Pages are written in place. Git is the history of the pages. A run returns JSON and does not keep a test history.

This guide is for a developer who has not used FitNesse or AgNesse. It explains how a page becomes a test, how Slim runs fixtures, and how to write each table type. The in-wiki guide at `AgNesse.UserGuide` is the acceptance-tested syntax for pages, Slim, the command line, and MCP. Those pages are included from `AgNesse.SuiteAcceptanceTests.SuiteUserGuide`, so the guide and the suite stay the same text.

## Contents

- [Theory of operation](#theory-of-operation)
- [Pages](#pages)
- [Slim](#slim)
- [Table types](#table-types)
- [Symbols and cell conventions](#symbols-and-cell-conventions)
- [Property tables](#property-tables)
- [Running tests](#running-tests)
- [Configuration](#configuration)
- [MCP](#mcp)
- [Build](#build)
- [User guide](#user-guide)
- [Credit and license](#credit-and-license)

## Theory of operation

A wiki page is an executable acceptance test. You write the example in tables. AgNesse turns those tables into Slim instructions, a Slim server runs your fixtures, and the results are colored back onto the page.

### How a page runs

1. You open a test page in the browser and click Test, or you run `-c Wiki.Path?test` on the command line.
2. AgNesse assembles one document for that page: inherited `SuiteSetUp` pages, `ScenarioLibrary` pages, `SetUp`, the page itself (including `include` blocks), `TearDown`, then `SuiteTearDown` pages.
3. GitHub-flavored Markdown tables become Slim tables. A table whose first cell is `comment` (or `comment:…`) is skipped. Fenced `agnesse` blocks are commands, not tables.
4. Each Slim table is compiled into instructions: construct a fixture, call methods, assign symbols, import a package.
5. AgNesse starts a Slim server (or connects to one) and sends those instructions. The default server is Java. A page can start a different process.
6. The server constructs fixtures and calls methods. Return values are compared to the expected cells.
7. Results come back. Passing cells are green, failing cells red, ignored cells gray. Exceptions are marked on the table. The same run is a JSON document.

A suite page does that for each child test. The suite itself is not a test.

### Suites

A page with `"type": "suite"` is a suite. Running `?suite` (or `-c Wiki.Path?suite`) runs the test pages under it. `suiteFilter` keeps only pages that carry a given tag. `excludeSuiteFilter` drops pages that carry a given tag.

A suite can define classpath entries, `COMMAND_PATTERN`, and `SLIM_PORT`. Child pages inherit those wiki variables. Import and library tables run only if they sit on the test page or on an included adjunct (`SetUp`, `SuiteSetUp`, `ScenarioLibrary`).

### Test pages and test-adjunct pages

`"type": "test"` is a test. `"type": "test-adjunct"` is included into a test. The adjunct names are `SetUp`, `TearDown`, `SuiteSetUp`, `SuiteTearDown`, and `ScenarioLibrary`. Their tables are Slim tables. Requesting `?test` on a test-adjunct page is an error. MCP lists those pages and does not run them on their own.

Composition, nearest ancestor first unless noted:

| Page | When it runs |
| --- | --- |
| `ScenarioLibrary` | Every uncle named `ScenarioLibrary` is included, outer first, before `SetUp`. Set `INCLUDE_SCENARIO_LIBRARIES` to `false` to skip them. |
| `SuiteSetUp` | The nearest uncle, before `SetUp`. Set `ALL_UNCLE_SUITE_SETUPS` to `true` to include every uncle. |
| `SetUp` | The closest inherited `SetUp`, before the page body. |
| the test page | The page you asked to run, with its `include` blocks. |
| `TearDown` | The closest inherited `TearDown`, after the page body. |
| `SuiteTearDown` | The nearest uncle, after `TearDown`. With `ALL_UNCLE_SUITE_SETUPS`, every uncle, inner first. |

`SetUp` and `TearDown` are not included when the page being run is itself a `SuiteSetUp` or `SuiteTearDown`.

`PageHeader` and `PageFooter` are display only. They are not test-adjunct pages.

### Includes

An `include` block runs the tables on the named page as part of this page. The name is a sibling, or a path from this page's parent. The rendered page shows that table. A missing page is named where the table would have been, and the run reports no assertions.

````
```agnesse
include IncludeTarget
```
````

A `scenarios` block loads the named page before the tables that follow, so a scenario defined there can be called as a script row.

### Front matter

A page file starts with a JSON object. Saving the page writes that object from the page properties.

```json
{
  "type": "test",
  "tags": ["guide"],
  "help": "How to write a page and run it",
  "prune": false
}
```

`type` is `test`, `suite`, `static`, or `test-adjunct`. `tags` is an array of strings. `help` is a string. `prune` skips the page and its children when a parent suite runs. Unknown keys are an error.

### Every test page must pass on its own

Running one test includes that page's inherited adjuncts. It does not run sibling tests. A page that only works after another test has already run will fail when you click Test on that page alone. Put shared construction on `SetUp` or `SuiteSetUp`. Put shared scenarios on `ScenarioLibrary`. Put cleanup on `TearDown` or `SuiteTearDown`.

## Pages

The wiki root is `AgNesseRoot`. The file `AgNesseRoot/AgNesse/UserGuide.md` is the wiki page `AgNesse.UserGuide`. A child page `AgNesse.SuiteAcceptanceTests` is `AgNesseRoot/AgNesse/SuiteAcceptanceTests.md`, and its children live in `AgNesseRoot/AgNesse/SuiteAcceptanceTests/`. Packages in this project are `agnesse.*`.

### Markdown tables

A Slim table is a bare GitHub-flavored Markdown table. The first header cell is the type and the fixture name. Later cells on that header row are constructor arguments. Trailing empty cells are not cells. The second row is the separator (`| --- |`). Later rows are data.

```
| decision:Division | | |
| --- | --- | --- |
| numerator | denominator | quotient? |
| 10 | 2 | 5.0 |
```

`decision:Division` is a decision table named `Division`. `dt:` is the same as `decision:`. A name with no type prefix is a decision table.

### agnesse blocks

Commands that are not tables go in fenced `agnesse` blocks. The first word is the command. The commands are `define`, `include`, `scenarios`, `path`, `contents`, and `alias`.

`define` sets a wiki variable. `${NAME}` in a Slim cell is replaced before the fixture sees the cell. A name with no definition is left as it was written. This is not a Slim symbol.

````
```agnesse
define GREETING
hello
```
````

`path` adds a classpath entry. Entries on this page come before entries on ancestor pages. The Slim process sees them on `java.class.path`.

`contents` renders a link to each child page.

`alias` names a table. The first line is the name used on the page. The next line is the fixture or table type it runs. A one-line alias does nothing.

````
```agnesse
alias split
Division
```
````

After that, a table headed `split` runs `Division`. The alias can also set the type, for example `ddt: add up change` or `query: $tq`.

## Slim

Slim is the protocol between the wiki and the code under test. AgNesse sends a list of instructions. The Slim server executes them and returns a list of results. The header is `Slim -- V0.5`. Each message is a six-digit length, a colon, and the bytes.

There is one test system. A page that defines `TEST_SYSTEM` is an error.

### Java Slim

The default command is `java`. The default runner is `agnesse.slim.SlimService`. The process is:

```
$JAVA_HOME/bin/java -cp <classpath> agnesse.slim.SlimService <port>
```

If `JAVA_HOME` is unset, the executable is `java`. `%p` in `COMMAND_PATTERN` is the classpath. `%m` is the runner.

A script table against `agnesse.fixtures.JavaProperties` can read `java.specification.version` from that JVM.

### A Slim server in another language

`COMMAND_PATTERN` is the process AgNesse starts. The first token must not contain `java`, or AgNesse treats it as a Java command and injects the classpath. AgNesse appends the Slim port.

Port `1` means the protocol is stdin and stdout. Any other port is a socket on `127.0.0.1`. The default port is `1`.

The shipped example is `AgNesseRoot/files/agnesse/tiny_slim.py`. It answers `world` from `hello`:

````
```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```
````

```
| script | tiny |
| --- | --- |
| check | hello | world |
```

### How a page picks its server

`agnesse.json` does not choose the Slim server. The keys there are wiki-server settings (`port`, `listen`, `rootPath`, `pageRoot`, `maximumWorkers`, `refactorStartPath`, `zone`).

The Slim process is chosen on the page, or on an ancestor:

| Setting | What it does |
| --- | --- |
| `COMMAND_PATTERN` | `define` the process to start. Default is `java -cp %p %m`. |
| `TEST_RUNNER` | `define` the class or program `%m` expands to. Default is `agnesse.slim.SlimService`. |
| `SLIM_PORT` | `define` `1` for a pipe. Any other value is a socket port on `127.0.0.1`. Default is `1`. |
| `SLIM_HOST` | `define` the host for a socket. |
| `SLIM_FLAGS` | `define` extra flags passed before the port. |
| `path` | An `agnesse` `path` block. Classpath entries for the Java Slim process. |

A command-line flag does not override these page variables. They travel with the page.

### Fixtures, imports, and names

A fixture is a class the Slim server constructs. An import table adds a package. After `| import |` / `| agnesse.fixtures |`, the name `echo fixture` finds `agnesse.fixtures.EchoFixture`.

Names are disgraced: spaces and punctuation break words. A class name capitalizes each word (`echo fixture` → `EchoFixture`). A method name is camel case (`set name` → `setName`, `echo` stays `echo`). A name that already contains a dot is used as written, so `agnesse.slim.test.Division` is the fully qualified class.

A library table constructs extra fixtures. When the current script fixture has no such method, Slim calls the most recently added library that does.

### How methods map to cells

The mapping depends on the table type. In a decision table, each input column `numerator` calls `setNumerator`, and each output column `quotient?` calls `quotient()`. In a script table, cells alternate between method-name fragments and arguments: `| check | echo | Hello | Hello |` calls `echo("Hello")` and compares the result to `Hello`. A cell that ends with `;` takes the following cells as sequential arguments instead of every other cell.

Optional decision-table methods `table`, `beginTable`, `reset`, `execute`, and `endTable` are called if they exist. A missing method of those names is not a failure. `Division` only has setters and `quotient()`, and that is enough.

## Table types

The first header cell selects the type. These are the types AgNesse registers.

### Decision

A decision table is a list of examples. Each data row sets the inputs, then checks the outputs. Output columns end in `?` or `!`. A column whose header starts with `#` is a comment.

```
| import |
| --- |
| agnesse.slim.test |

| decision:Division | | |
| --- | --- | --- |
| numerator | denominator | quotient? |
| 10 | 2 | 5.0 |
| 5 | 2 | 2.5 |
```

```java
package agnesse.slim.test;

public class Division {
  private double numerator;
  private double denominator;

  public void setNumerator(double numerator) {
    this.numerator = numerator;
  }

  public void setDenominator(double denominator) {
    this.denominator = denominator;
  }

  public double quotient() {
    return numerator / denominator;
  }
}
```

`dt:Division` is the same table. A header that is only `Division` is also a decision table.

Constructor arguments are the remaining cells on the header row. For each data row Slim calls `reset` (if present), the setters, `execute` (if present), then the getters.

A decision table can call a scenario of the same name instead of a fixture. Column headers then become scenario inputs, and output columns must be scenario outputs.

Column names can be rewritten with wiki variables `SLIM_DT_SETTER` and `SLIM_DT_GETTER`. That is still a decision table. The rules are JSON method-extractor lists. See `AgNesse.SuiteAcceptanceTests.SuiteSlimTests.HybridDecisionTable`.

### Dynamic decision

A dynamic decision table calls `set(columnName, value)` for each input and `get(columnName)` for each output, instead of one method per column. Use it when the columns are data keys, not a fixed set of methods.

```
| import |
| --- |
| agnesse.slim.test |

| ddt: add up change | | | | | | | | |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| # description | 1c | 5c | 10c | 25c | 50c | $1 | total cents? | $ total? |
| some simple addition | 2 | 2 | 4 | 0 | 0 | 0 | 52 | 0.52 |
```

```java
public class AddUpChange {
  public void reset() { /* zero the total */ }
  public void set(String coin, Integer amount) { /* add coin * amount */ }
  public String get(String requestedValue) { /* "$ total" or "total cents" */ }
}
```

`dynamic decision:` is the same as `ddt:`.

### Baseline decision

A baseline decision table is a decision table whose first data row is the base scenario. Later rows fill only the cells that differ. Empty cells copy the base row. The fixture is an ordinary decision fixture.

```
| import |
| --- |
| agnesse.slim.test |

| baseline: should I buy milk | | | | |
| --- | --- | --- | --- | --- |
| cash in wallet | credit card | pints of milk remaining | go to store? | # |
| 0 | no | 1 | no | . |
| | | 2 | | . |
| 10 | | | | . |
| | yes | | | . |
```

Defining `SLIM_DT_BASELINE` makes every decision table on that page behave this way.

### Query, subset query, and ordered query

A query table checks a collection. The fixture's `query()` method returns a list of rows. Each row is a list of `[fieldName, value]` pairs. The table's second row names the fields. Later rows are expected records.

A query table matches as a set: every expected row must be present, extra actual rows fail, order does not matter.

```
| Query:SplitFixture | Bob;Bill;Ben |
| --- | --- |
| 1 | |
| Bob | |
| Bill | |
| Ben | |
```

```java
public class SplitFixture {
  public SplitFixture(String schema) { /* split schema on ';' */ }

  public List<Object> query() {
    // one row per name; field "1" is the name
  }
}
```

`SplitFixture` lives in `agnesse.fixtures`. The constructor argument is the next cell on the header row, here `Bob;Bill;Ben`.

A subset query is the same, except extra actual rows are allowed. The empty set is a subset. The complete set is a subset.

```
| Subset Query:SplitFixture | Bob;Bill;Ben |
| --- | --- |
| 1 | |
| Ben | |
| Bill | |
```

An ordered query requires the expected rows in that order. A match that arrives too early is marked out of order. Extra actual rows still fail.

```
| Ordered Query:duplicate rows | A |
| --- | --- |
| x | |
| SuiteChildOne.SuiteSetUp | |
| SuiteChildOne.TestOneOne | |
| SuiteChildOne.TestOneTwo | |
| SuiteChildOne.SuiteTearDown | |
```

Empty expected cells are ignored and show the actual value. A field header that starts with `#` is a comment column. Expected cells may use the same comparisons as decision tables (`<3`, `=~/Mar.*/`, `$name=`).

### Script

A script table is a sequence of actions on one fixture (the actor). The fixture name is in the header, or in a later `start` row.

```
| script | echo fixture | | |
| --- | --- | --- | --- |
| check | echo | Hello | Hello |
```

`script: echo fixture` is the same header.

Keywords in the first cell:

| Keyword | Meaning |
| --- | --- |
| `start` | Construct a new actor. |
| `check` | Call a method; the last cell is the expected value. |
| `check not` | Pass when the value is not the last cell. |
| `ensure` | Pass when the method returns true. |
| `reject` | Pass when the method returns false. |
| `show` | Call a method and display the result. Not an assertion. |
| `note` | Do nothing. An empty first cell, or one that starts with `#` or `*`, is also a note. |
| `$name=` | Call the rest of the row and store the result in `$name`. |

A row that is not a keyword is an action: it calls the method, or it calls a scenario of that name. A boolean true colors green; false colors red; void is plain.

Cells alternate between method-name fragments and arguments: `| check | return constructor arg | 99 |` calls `returnConstructorArg()`. A cell that ends with `;` takes the following cells in order:

```
| script | agnesse.slim.test.TestSlim | | | |
| --- | --- | --- | --- | --- |
| concatenate three args; | 1 | 2.0 | three | |
```

That calls `concatenateThreeArgs("1", "2.0", "three")`. Mixing every-other-cell and sequential processing in one row is allowed.

### Verify script

`verify script` is a script table whose check keyword is `verify` instead of `check`. Use it when a scenario should say `verify` so a later decision table that calls that scenario uses the same keyword.

```
| verify script | agnesse.slim.test.Division |
| --- | --- |
```

### Scenario

A scenario is a reusable script fragment. It does not run where it is defined. A later script row or decision table with that name runs the body, substituting `@parameter`.

```
| scenario | beat | name | with a | object |
| --- | --- | --- | --- | --- |
| check | echo | @name | Bill | |
| check | echo | @object | noodle | |

| script |
| --- |
| beat Bill with a noodle |
```

Two header styles work. Alternating cells: `| scenario | login as | user |`. Underscores: `| scenario | login user _ with password _ | user name, password |`. Output parameters end with `?` in the scenario header; the body assigns `$name=`, and the caller reads `$name`.

A `scenarios` block loads a page of scenarios before the tables that follow.

### Table

A table table hands the body of the table to one method, `doTable`. The fixture returns a list of lists of cell results: `pass`, `fail`, `ignore`, `no change`, or `pass:message` / `fail:message`.

```
| table: agnesse.fixtures.TableOrDecisionFixture |
| --- | --- |
| parsing as? | Parsed as Table fixture |
```

```java
public class TableOrDecisionFixture {
  public List<List<String>> doTable(List<List<String>> table) {
    return List.of(List.of("", "pass: Parsed as Table fixture"));
  }
}
```

If `doTable` returns null, the table is ignored.

### Import

An import table adds a package to the Slim search path. It must have at least two rows. Empty rows are skipped.

```
| import |
| --- |
| agnesse.fixtures |
| agnesse.slim.test |
```

### Library

A library table constructs fixtures that fill in methods the current actor does not have. Later libraries take precedence. Constructor arguments work the same way as on a script header.

```
| Library |
| --- |
| echo support |
```

```
| library | |
| --- | --- |
| test query | 2 |
```

After that, a script whose fixture has no `query` method still runs `query` on the `TestQuery` instance.

### Comment

A comment table is not Slim. The marker is the whole first cell, or the text before a colon. The table is skipped and does not count as an assertion.

```
| Comment |
| --- |
| SuiteWidgetTests | this is not a test |
```

### Property

A property table draws inputs and checks one claim against many generated values. One property is one assertion, however many trials run. The cell language, the seed, and shrinking are in [Property tables](#property-tables) and in the wiki guide.

```
| property:agnesse.slim.test.ListReversal |
| --- |
| trials | 20 |
| seed | |
| items | reversed twice? |
| list of int:-5..5 size 0..4 | true |
```

An unknown type in the first cell is an error table (`"…" is not a valid table type.`).

### Sequence

A sequence table draws commands and checks one model across many sequences. One sequence is one assertion. The table form and shrinking are in [Sequence tables](#sequence-tables) and in the wiki guide.

```
| sequence:agnesse.slim.test.ItemLife |
| --- |
| trials | 8 |
| steps | 1..4 |
| seed | 1 |
| state | phase=new; blocked=no |
| command | when | next | result? | phase? |
| open | phase=new | phase=open | ok | open |
| block | phase=open | phase=blocked; blocked=yes | ok | blocked |
| release | phase=open | phase=released | ok | released |
| release | phase=blocked | | refused | blocked |
| invariant | when | model | query | expect |
| | blocked=yes | phase!=released | released? | false |
```

## Symbols and cell conventions

### Slim symbols

`$name=` in an expected cell stores the actual value in a Slim symbol. `$name` in a later cell is replaced with that value. Names start with a letter, then letters, digits, and underscores. Unicode letters are allowed. Names are case sensitive. An undefined `$NAME` is left as `$NAME`.

```
| import |
| --- |
| agnesse.slim.test |

| test slim | |
| --- | --- |
| string | getStringArg? |
| foo | $x3_= |
| $x3_ | foo |
```

A script row can assign as well:

```
| script | test slim | |
| --- | --- | --- |
| $X= | echo int | 99 |

| script | test slim | $X |
| --- | --- | --- |
| check | return constructor arg | 99 |
```

A symbol can hold null or a blank. An empty expected cell cannot write a blank, so a blank is checked with `=~/^$/`. A symbol whose name starts with `SECRET_` is shown as `*****` in the HTML.

`$name=` must be the whole cell. `$name` can sit inside a longer string. Wiki variables (`${NAME}` from `define`) are substituted first, and are not Slim symbols.

### Comparisons

An expected cell that is not an exact match can be a comparison. These are the forms the suite uses:

| Expected | Passes when the actual value |
| --- | --- |
| `=5` | equals 5 |
| `!=5` | is not 5 |
| `>5`, `>=5`, `<5`, `<=5` | compares as a number |
| `~=5.0` | is approximately 5.0 (precision of the expected text) |
| `!~=5.0` | is not approximately 5.0 |
| `3<=_<=5`, `3<_<5` | lies in that range (`_` is the actual value) |
| `=~/Bob/` | contains a match for the regular expression |

The same forms work in query tables.

### Blank, ignored, and comment cells

An empty expected cell is ignored. The actual value is shown and is not an assertion. In a baseline decision table, an empty cell in the base row is an ignored output for every row that does not fill it in.

A decision or query column whose header starts with `#` is a comment. The cell is not sent to the fixture.

In a script, a row whose first cell is empty, `note`, `#…`, or `*…` is a note.

A fixture can ignore the rest of the script by throwing an ignore-script exception, or ignore the rest of the page by throwing an ignore-all exception. `TearDown` still runs.

## Property tables

A property table checks one claim against generated inputs and counts as one assertion. The cell language, the seed, and shrinking are in the user guide at `AgNesse.UserGuide`.

Name a fixture the way a decision table does. Optional `trials`, `seed`, and `now` rows come next, then a column header and one generator row. An output column ends in `?` or `!` and its generator cell is the expected value. The default is 100 trials when that row is omitted or left blank. `all of` is the exception: the trial count is the number of cases, and a number you write has to match it.

Leave `seed` blank and the result shows the seed that was used. A property that draws a time also shows `now`. Copy either value back into its cell to draw the same inputs again. A failure appends one shrunk row and names the original draw in the trials cell.

Generators are lowercase. Ranges are inclusive. `int` draws from `-10000..10000`. `boolean`, `string`, `list of`, and `one of:red,green,blue` are the other forms. `decimal:0..1 scale 2` and `money:0..1` are fixed-point, never a binary fraction. `one of:a 70, b 30` weights the choices. `string:adversarial` mixes in hostile text. `pattern:REG-[0-9]{4}` draws an identifier shape. `all of:red,blue` and `all of:admin,user x view,edit` run every case. `date:2020-01-15..2020-01-31`, `time:08:00:00..17:00:00`, `datetime:now+35h59m..now+36h01m`, `period:day`, and `now` draw ISO-8601 text. `zone:America/New_York` overrides the `zone` key for that generator. A week starts on Sunday. `int after start:0..9` stays one above an earlier column, and `before` stays one below. `list of int:0..20 size 0..5 unique sorted` is strictly increasing. `span:0..100 length 1..10` draws `start+length`. `partition:10.00 into 3` splits a total into that many non-negative parts. `monotonic:0..100 size 1..5 reset` is a meter that may drop and keeps its running total when it shrinks. `record:name string; age int:0..120` is a JSON object whose leaves are strings; nest another record in parentheses. A scenario can be the claim; `@value` is the drawn cell, and `$now` is the clock. A column whose generator is `now` sends that same text as a Slim argument.

The full language, shrinking, and the Python example are the Property page included from `AgNesse.UserGuide`.

## Sequence tables

A sequence table lists commands. Optional `trials`, `steps`, `seed`, and `state` rows come first. `trials` defaults to 100. `steps` defaults to `1..8`, and each bound is from 1 through 32. Leave `seed` blank and the result shows the seed. It is not written back onto the page. `state` is `name=value` pairs separated by `;`.

A command row is the name, `when`, and `next`, then arguments and checks. `when` is a precondition, and every value is a literal, so a command is drawn only when the model matches. `next` assigns the model, left to right. `@name` reads the model or an argument of this step. An argument column uses the property cell language and is sent to Slim. An empty argument cell is left out of the call. `result?` is the return value. Any other `name?` is a no-arg query after the command. `!` means the same as `?`.

An invariant row is `when`, `model`, `query`, and `expect`. It is checked after a command whose `when` matches, and it has to hold in every row still kept. `model` sees the model after `next`, not the fixture. A query sees the fixture.

There is no `or` inside `when` or `next`. Two endings are two rows of the same command:

| command | when | next | result? |
| --- | --- | --- | --- |
| reserve | phase=open | phase=held | held |
| reserve | phase=open | phase=wait | wait |

After the call, every row whose checks match the fixture is kept. A later command is drawn when its `when` holds in at least one of those rows. The trial fails when none match. The trials cell names the step and what was seen against each row (`held or wait`). More than 64 candidates is a bad table: `model too ambiguous`.

Each trial is a new fixture. The cells are ordinary strings, so Java, Python, and any other Slim server run the same table.

A `threads` row runs one branch per thread against that same fixture. `threads` is `low..high`, from 2 through 32. `steps` is how long each branch is. `prefix` is the serial length before the branches, from 0 through 32, and it defaults to `0..0`. Each branch is a legal continuation of the prefix on its own. AgNesse runs the prefix, then one command from each branch at the same time, and looks for a serial order that explains every return. One such order passes. None fails. The search stops after 4096 states and the trials cell says `linearizability search hit its bound`.

Those calls are ordinary Slim calls, one socket connection per thread. The Java server needs `-d` so the connections share the fixture the first connection created. A pipe cannot, and the table says so. A server that keeps a private fixture per connection says it does not share one fixture. `SLIM_FLAGS` `-d` and `SLIM_PORT` `0` are the Java settings. The shipped Python server shares instances on a socket.

```
| sequence:agnesse.slim.test.LastSeatSafe |
| threads | 2..2 |
| prefix | 0..0 |
| steps | 1..1 |
| state | seats=1 |
| command | when | next | result? |
| book | seats=1 | seats=0 | booked |
| book | seats=0 | | full |
```

Shrinking drops threads, then the prefix, then commands, then arguments. A race may not show on every run, so each candidate runs 3 times and is kept if any run fails. The trials cell shows the drawn prefix and branches, what came back, and `shrunk` when a shorter failure was kept.

A failure drops commands that are not needed, shortest first, and only when the remaining preconditions still hold. A shorter length is tried when there are at most 64 ways to choose those commands. Argument cells then shrink with the property shrinkers. The trials cell names the trial, the drawn commands, and the failing step or invariant. It says `shrunk` when a shorter failure was kept, and `shrink cap` after 32 accepted steps. The result rows show that sequence. The saved page is unchanged.

The whole specification is one Markdown table. A blank line starts a new table.

A missing command, an unknown `@name`, a precondition that can never hold, a steps range outside 1 through 32, and more than 64 candidates after one command are refused before any trial.

The Sequence page included from `AgNesse.UserGuide` runs the examples, including a Python gate.

## Running tests

### Browser wiki

Start the wiki on port 8001. It listens on 127.0.0.1.

```
./gradlew run
java -jar build/libs/agnesse-*-standalone.jar -p 8001
```

The standalone jar's main class is `agnesseMain.AgNesseMain`. `--listen 0.0.0.0` listens on every interface. Any other `--listen` address binds that address.

Open `http://127.0.0.1:8001/`. `FrontPage` links to the user guide and the acceptance suite. Open a test page and click Test. Open a suite page and click Suite. The page is rewritten with colored cells. JSON is available with `?test&format=json` or `?suite&format=json`.

### Command line

`-c` runs one page or suite and prints JSON. The process exits with the number of failing pages. `-b` names the output file instead of standard output.

```
java -jar build/libs/agnesse-*-standalone.jar -c AgNesse.SuiteAcceptanceTests?suite -b results.json
```

The document has `"schemaVersion": 1`. A page that passed has `"status": "pass"`. The same run writes `.agnesse/last-failures.json` beside the wiki root. `--rerun-failures` points at that file, or at a full results document, and runs only the failed pages.

When `-c` is used and the port is left unset, the run uses port 9123. The wiki's port default is 80.

The `agnesse.junit` runner writes JUnit XML for its own runs. `-c` does not. HTTP `format=xml` and `format=junit` are gone.

Other flags: `-v` turns on verbose logging, `-d` is the working directory, `-r` is the page root, `-f` names the config file.

## Configuration

`agnesse.json` in the working directory supplies the defaults. If that file is missing, the built-in defaults are used. `-f` names a different file, and that file must exist. A null value leaves the default. A command-line flag overrides the file. Unknown keys are an error.

```json
{
  "port": null,
  "rootPath": ".",
  "pageRoot": "AgNesseRoot",
  "maximumWorkers": 100,
  "refactorStartPath": null,
  "listen": "127.0.0.1",
  "zone": "UTC"
}
```

`port` and `maximumWorkers` are integers. `listen`, `rootPath`, `pageRoot`, `refactorStartPath`, and `zone` are strings. `zone` is the default zone for property time generators, a `java.time` zone id such as `UTC` or `America/New_York`. It is not hard-coded. A time generator that omits `zone:` needs this key. A null value leaves it unset.

This file does not choose the Slim server. See [How a page picks its server](#how-a-page-picks-its-server).

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

`run` takes a wiki path. Omit `suite` and a suite page runs its children, a test page runs alone. `tags` and `excludeTags` are the command line's `suiteFilter` and `excludeSuiteFilter`. `rerunFailures` is a file of failed pages. A test-adjunct page is listed and does not run on its own.

## Build

AgNesse requires Java 21. The Gradle wrapper builds it.

```
./gradlew test
./gradlew acceptanceTest
./gradlew build
```

`test` is the unit suite. `acceptanceTest` runs `AgNesse.SuiteAcceptanceTests` and writes `build/reports/acceptance-results.json`. `build` runs both, checks that JSON against the results schema, and enforces the coverage floors and the removed-name check. On Windows, use `gradlew.bat`.

## User guide

The in-wiki guide is `AgNesseRoot/AgNesse/UserGuide.md`, wiki path [`AgNesse.UserGuide`](AgNesseRoot/AgNesse/UserGuide.md). It includes the pages under [`AgNesse.SuiteAcceptanceTests.SuiteUserGuide`](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide.md), so the guide and the suite stay the same text:

| Page | What it specifies |
| --- | --- |
| [FrontMatter](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/FrontMatter.md) | Page JSON, tags, `suiteFilter` |
| [Define](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Define.md) | `${NAME}` substitution |
| [Include](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Include.md) | `include` blocks |
| [Scenarios](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Scenarios.md) | `scenarios` blocks |
| [Path](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Path.md) | classpath entries |
| [Contents](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Contents.md) | child-page links |
| [Alias](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Alias.md) | table aliases |
| [JavaSlim](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/JavaSlim.md) | the default Java Slim process |
| [Property](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Property.md) | property tables |
| [Sequence](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Sequence.md) | sequence tables |
| [PythonSlim](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/PythonSlim.md) | `COMMAND_PATTERN` for a non-Java Slim server |
| [CommandLine](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/CommandLine.md) | `-c`, JSON, `--rerun-failures` |
| [Mcp](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/Mcp.md) | `-mcp` |
| [JsonErrors](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/JsonErrors.md) | JSON error objects |
| [RenameSource](AgNesseRoot/AgNesse/SuiteAcceptanceTests/SuiteUserGuide/RenameSource.md) | rename and `path` blocks |

Worked samples that are not the guide itself live under [`AgNesseSamples`](AgNesseRoot/AgNesseSamples.md).

## Credit and license

AgNesse is derived from FitNesse, originally at <https://github.com/unclebob/fitnesse>. The license is unchanged: Common Public License 1.0, in `LICENSE.txt`.
