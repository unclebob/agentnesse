{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
The user guide includes the example pages. Rendering [UserGuide](.AgNesse.UserGuide) shows their text.

| script | page driver |
| --- | --- |
| ensure | bundled html contains; | AgNesse.UserGuide | <a href="AgNesse.SuiteAcceptanceTests.SuiteUserGuide.FrontMatter">SuiteAcceptanceTests.SuiteUserGuide.FrontMatter</a> |
| ensure | bundled html contains; | AgNesse.UserGuide | starts with a JSON object |
| ensure | bundled html contains; | AgNesse.UserGuide | left as it was written |
| ensure | bundled html contains; | AgNesse.UserGuide | named where the table would have been |
| ensure | bundled html contains; | AgNesse.UserGuide | becomes a script row |
| ensure | bundled html contains; | AgNesse.UserGuide | come before entries on ancestor pages |
| ensure | bundled html contains; | AgNesse.UserGuide | renders a link to each child page |
| ensure | bundled html contains; | AgNesse.UserGuide | A one-line alias does nothing |
| ensure | bundled html contains; | AgNesse.UserGuide | which a command that is not Java cannot do |
| ensure | bundled html contains; | AgNesse.UserGuide | A property table draws the inputs |
| ensure | bundled html contains; | AgNesse.UserGuide | A sequence table lists commands |
| ensure | bundled html contains; | AgNesse.UserGuide | The first token must not contain |
| ensure | bundled html contains; | AgNesse.UserGuide | exits with the number of failing pages |
| ensure | bundled html contains; | AgNesse.UserGuide | does not open a port |
| ensure | bundled html contains; | AgNesse.UserGuide | Removed parameters include |
| ensure | bundled html contains; | AgNesse.UserGuide | refactorReferences=on |
| reject | bundled html contains; | AgNesse.UserGuide | include SuiteAcceptanceTests.SuiteUserGuide.FrontMatter |
