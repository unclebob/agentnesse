{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
The user guide includes the example pages rather than copying them.

| script | page driver |
| --- | --- |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.FrontMatter |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Define |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Include |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Scenarios |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Path |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Contents |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.Alias |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.JavaSlim |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.PythonSlim |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.CommandLine |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.JsonErrors |
| ensure | bundled source contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.RenameSource |
| ensure | bundled source contains; | AgNesse.SuiteAcceptanceTests.SuiteUserGuide.FrontMatter | test-adjunct page is included into a test |
| ensure | bundled source contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.SetUp | "type": "test-adjunct" |
| ensure | bundled source contains; | AgNesse.SuiteAcceptanceTests.SuiteUserGuide.Define | define GREETING |
| ensure | bundled source contains; | AgNesse.SuiteAcceptanceTests.SuiteUserGuide.PythonSlim | tiny_slim.py |
| ensure | bundled source contains; | AgNesse.SuiteAcceptanceTests.SuiteUserGuide.CommandLine | schemaVersion |
