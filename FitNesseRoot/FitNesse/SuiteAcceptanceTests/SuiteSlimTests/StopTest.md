{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Raising an exception with the term "StopTest" in it will make sure test is stopped, but "TearDown" and rest of suite is executed.


| script | | | |
| --- | --- | --- | --- |
| given page | MySuite | with content | ${SUT_PATH} <br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\`<br> |
| given page | MySuite.TestPageA | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| constructor throws \| stop test \|<br>\| --- \| --- \|<br><br><br>\| table never to be executed \|<br>\| --- \| |
| given page | MySuite.TestPageB | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| hello \| hello \| |
| given page | MySuite.TearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| tear down after \${PAGE_NAME} \| tear down after \${PAGE_NAME} \| |
| given page | MySuite.SuiteTearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br><br><br>\| script \| echo script \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| check \| echo \| suite tear down executed \| suite tear down executed \| |
| test results for suite | MySuite | should contain | table never to be executed <span class="ignore">Test not run</span> |
| ensure | content contains | <span class="pass">tear down after TestPageA</span> | |
| ensure | content contains | <span class="pass">echo script</span> | |
| ensure | content contains | <span class="pass">tear down after TestPageB</span> | |
| ensure | content contains | <span class="pass">suite tear down executed</span> | |
