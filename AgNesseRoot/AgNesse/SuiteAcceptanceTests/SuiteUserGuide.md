{
  "type": "suite",
  "tags": [],
  "help": "The user guide. Each page is a test.",
  "prune": false
}
# User guide

Each page in this suite is an example, and the suite runs it. The same pages are included from [UserGuide](.AgNesse.UserGuide).

| Page | What it specifies |
| --- | --- |
| [FrontMatter](>FrontMatter) | A tag on a page selects it with `suiteFilter` and with `searchProperties` |
| [Define](>Define) | A `define` block substitutes into a Slim cell |
| [Include](>Include) | An `include` block runs the tables on the named page |
| [Scenarios](>Scenarios) | A `scenarios` block loads the named page before the tables that follow |
| [Path](>Path) | A `path` block is on `java.class.path` |
| [Contents](>Contents) | A `contents` block links to the child pages |
| [Alias](>Alias) | An `alias` block gives a table a business name |
| [JavaSlim](>JavaSlim) | A Java fixture answers a Slim table |
| [Property](>Property) | A property table draws inputs, shrinks a failure, and counts as one assertion |
| [Sequence](>Sequence) | A sequence table draws commands, checks a model, and shrinks a failing sequence |
| [PythonSlim](>PythonSlim) | A command that is not Java can speak Slim |
| [CommandLine](>CommandLine) | `-c` writes JSON and exits with the failure count. `--rerun-failures` skips pages that passed |
| [Mcp](>Mcp) | `-mcp` serves list_pages, run, and get_results on standard input and output |
| [JsonErrors](>JsonErrors) | Missing pages, unknown parameters, and refused moves are JSON |
| [RenameSource](>RenameSource) | A rename rewrites a reference and leaves a `path` block alone |
| [UserGuideRenders](>UserGuideRenders) | The shipped guide includes these pages |
