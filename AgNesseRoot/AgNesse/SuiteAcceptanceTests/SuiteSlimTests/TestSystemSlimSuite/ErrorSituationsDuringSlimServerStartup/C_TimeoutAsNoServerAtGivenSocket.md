{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
## Trying to connect to a socket where no slim server is running should fail quickly.

To test this we run in debug mode and request to start the slim server ourselfs. But we never start one :)

Use the property slim.timeout to define how long the test system should wait for the slim client. The default is 10 seconds.
In this test we set it to 0. The client tries the closed port, waits one retry, and then reports that it could not connect.

<!-- To run this test interactive don't forget to add at the end "&remote_debug" to the rest test command -->


| script | | | |
| --- | --- | --- | --- |
| create page | TestPage | with content | <br>\`\`\`agnesse<br>define slim.timeout<br>0<br>\`\`\`<br>\`\`\`agnesse<br>define SLIM_PORT<br>8099<br>\`\`\`<br>\`\`\`agnesse<br>define MANUALLY_START_TEST_RUNNER_ON_DEBUG<br>true<br>\`\`\` |
| check | request page | TestPage?test&remote_debug&format=json | 200 |
| ensure | content contains | Error connecting to SLiM server on localhost | |
| show collapsed | content | | |
