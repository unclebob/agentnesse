{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Raising an exception with the term "StopTest" in it during "`SuiteSetUp`" will make sure all remaining tests are skipped.


| script | | | |
| --- | --- | --- | --- |
| given page | MySuite | with content | ${SUT_PATH} <br><br> |
| given page | MySuite.SuiteSetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| constructor throws \| stop test \|<br>\| --- \| --- \|<br><br><br>\| table in page setup never to be executed \|<br>\| --- \| |
| given page | MySuite.TestPageA | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| table in page A never to be executed \|<br>\| --- \| |
| given page | MySuite.TestPageB | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| table in page B never to be executed \|<br>\| --- \| |
| given page | MySuite.TearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| tear down after \${PAGE_NAME} \| tear down after \${PAGE_NAME} \| |
| given page | MySuite.SuiteTearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| suite tear down executed \| suite tear down executed \| |
| test results for suite | MySuite | should contain | table in page setup never to be executed |
| ensure | content contains | "status":"ignore" | |
| ensure | content contains | Test not run | |
| ensure | content contains | table in page A never to be executed | |
| ensure | content contains | tear down after TestPageA | |
| ensure | content contains | table in page B never to be executed | |
| ensure | content contains | tear down after TestPageB | |
| ensure | content contains | suite tear down executed | |
| ensure | content contains | "status":"pass" | |
