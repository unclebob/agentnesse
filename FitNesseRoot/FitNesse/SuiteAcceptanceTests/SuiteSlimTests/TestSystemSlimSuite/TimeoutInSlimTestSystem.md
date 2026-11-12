{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
For this test to perform we reduce the timeout to 1 seconds. If the server is not responding in time, we should raise an error.



| script | | | |
| --- | --- | --- | --- |
| given page | TestPage | with content | ${SUT_PATH} <br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\`<br>\`\`\`agentnesse<br>define slim.flags<br>-s 1<br>\`\`\`<br><!-- Will pass --><br><br>\| fitnesse.fixtures.Sleep \| 20 \|<br>\| --- \| --- \|<br><br><!-- Will fail --><br><br>\| fitnesse.fixtures.Sleep \| 90000 \|<br>\| --- \| --- \|<br> |
| test results for page in debug mode | TestPage | should contain | The instruction timed out after 1 seconds |
| test results for page in debug mode | TestPage | should contain | span class="pass">fitnesse.fixtures.Sleep</span |
