{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Raising an exception with the term "StopTest" in it during "SuiteSetUp" will make sure all remaining tests are skipped.


| script | | | |
| --- | --- | --- | --- |
| given page | MySuite | with content | ${SUT_PATH} <br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\`<br> |
| given page | MySuite.SuiteSetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| constructor throws \| stop test \|<br>\| --- \| --- \|<br><br><br>\| table in page setup never to be executed \|<br>\| --- \| |
| given page | MySuite.TestPageA | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| table in page A never to be executed \|<br>\| --- \| |
| given page | MySuite.TestPageB | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| table in page B never to be executed \|<br>\| --- \| |
| given page | MySuite.TearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| tear down after \${PAGE_NAME} \| tear down after \${PAGE_NAME} \| |
| given page | MySuite.SuiteTearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| suite tear down executed \| suite tear down executed \| |
| test results for suite | MySuite | should contain | table in page setup never to be executed <span class="ignore">Test not run</span> |
| ensure | content contains | table in page A never to be executed <span class="ignore">Test not run</span> | |
| ensure | content contains | tear down after TestPageA <span class="ignore">Test not run</span> | |
| ensure | content contains | table in page B never to be executed <span class="ignore">Test not run</span> | |
| ensure | content contains | tear down after TestPageB <span class="ignore">Test not run</span> | |
| ensure | content contains | <span class="pass">suite tear down executed</span> | |
