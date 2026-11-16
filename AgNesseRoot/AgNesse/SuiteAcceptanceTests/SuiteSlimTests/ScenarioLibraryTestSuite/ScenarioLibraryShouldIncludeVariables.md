{
  "type": "test",
  "tags": [],
  "help": "",
  "prune": true
}

| script | | | |
| --- | --- | --- | --- |
| create page | SuitePage | with content | <br><br>\`\`\`agnesse<br>define VARIABLE<br>some value<br>\`\`\`<br> ${SUT_PATH} |
| make | SuitePage | a suite page | |
| create page | SuitePage.ScenarioLibrary | with content | Scenario Library: "${VARIABLE}" |
| create page | SuitePage.TestPage | with content | Test Page: "${VARIABLE}" |
| make | SuitePage.TestPage | a test page | |

You would expect the variable to be substituted when the test page is executed:


| script | | | |
| --- | --- | --- | --- |
| check | request page | SuitePage.TestPage?test&format=json | 200 |
| show | content | | |
| ensure | content contains | Scenario Library: "some value" | |
| ensure | content contains | Test Page: "some value" | |


Now, it we execute from Suite level, the variable should be substituted too:



| script | | | |
| --- | --- | --- | --- |
| check | request page | SuitePage?suite&format=json | 200 |
| show | content | | |
| ensure | content contains | Scenario Library: "some value" | |
| ensure | content contains | Test Page: "some value" | |
