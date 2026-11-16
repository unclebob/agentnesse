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
| >FrontMatter | A tag on a page selects it with `suiteFilter` and with `searchProperties` |
| >Define | A `define` block substitutes into a Slim cell |
| >Include | An `include` block runs the tables on the named page |
| >Scenarios | A `scenarios` block loads the named page before the tables that follow |
| >Path | A `path` block is on `java.class.path` |
| >Contents | A `contents` block links to the child pages |
| >Alias | An `alias` block gives a table a business name |
| >JavaSlim | A Java fixture answers a Slim table |
| >PythonSlim | A command that is not Java can speak Slim |
| >CommandLine | `-c` writes JSON and exits with the failure count. `--rerun-failures` skips pages that passed |
| >JsonErrors | Missing pages, unknown parameters, and refused moves are JSON |
| >RenameSource | A rename rewrites a reference and leaves a `path` block alone |
| >UserGuideRenders | The shipped guide includes these pages |
