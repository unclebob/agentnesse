{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
## Identify if the Slim Server process dies during startup

If a slim server process dies before a connection has been established this should be identified quickly and the test aborted.
We use a test runner class which doesn't exists. This ensures the slim server dies.



| script | | | |
| --- | --- | --- | --- |
| create page | TestPage | with content | \`\`\`agentnesse<br>define TEST_SYSTEM<br>SLIM<br>\`\`\`<br>\`\`\`agentnesse<br>define slim.timeout<br>2<br>\`\`\`<br>\`\`\`agentnesse<br>define SLIM_PORT<br>0<br>\`\`\`<br>\`\`\`agentnesse<br>define TEST_RUNNER<br>something.that.does.not.exists<br>\`\`\` |
| check | request page | TestPage?test | 200 |
| ensure | content contains | Error SLiM server died before a connection could be established. | |
| show collapsed | content | | |
