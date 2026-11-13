{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
The first matching method-extractor rule turns a column name into a method and a parameter list. A JSON configuration is accepted by the constructor.

| import |
| --- |
| fitnesse.testsystems.slim.tables |


| script | Method Extractor | |
| --- | --- | --- |
| add; | Cell\s+(\w+):(\d+)\s* | getRowColumn | $2,$1 |
| add; | .+ | set | $0 |
| $rule= | find rule | Cell A:5 |
| start | $rule |
| check | get method name | getRowColumn |
| check | get disgraced method name | getRowColumn |
| check | get parameters | [5, A] |


| script | Method Extractor | {"FormatVersion":"1.0","MethodExtractorRules":[{"Scope":"property\\\\s+(\\\\w*)\\\\s*","TargetName":"get property","Parameters":"$1"},{"Scope":".+","TargetName":"set $0","Parameters":""}]} |
| --- | --- | --- |
| $rule= | find rule | property abc |
| start | $rule |
| check | get disgraced method name | getProperty |
| check | get parameters | [abc] |
