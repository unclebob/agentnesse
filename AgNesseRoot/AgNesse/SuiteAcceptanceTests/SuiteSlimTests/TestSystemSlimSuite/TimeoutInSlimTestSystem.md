{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
For this test to perform we reduce the timeout to 500 milliseconds. If the server is not responding in time, we should raise an error.



| script | | | |
| --- | --- | --- | --- |
| given page | TestPage | with content | ${SUT_PATH} <br><br>\`\`\`agnesse<br>define slim.flags<br>-s 500ms<br>\`\`\`<br><!-- Will pass --><br><br>\| agnesse.fixtures.Sleep \| 20 \|<br>\| --- \| --- \|<br><br><!-- Will fail --><br><br>\| agnesse.fixtures.Sleep \| 90000 \|<br>\| --- \| --- \|<br> |
| test results for page in debug mode | TestPage | should contain | The instruction timed out after 0.5 seconds |
| and should contain | agnesse.fixtures.Sleep | | |
