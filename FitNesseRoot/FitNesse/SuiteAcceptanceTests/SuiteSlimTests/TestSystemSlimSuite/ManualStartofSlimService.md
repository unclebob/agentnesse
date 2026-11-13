{
  "type": "test",
  "tags": ["noJunit"],
  "help": null,
  "prune": false
}
With the manual runner, Slim connects to the given host and port and reports a missing header.


| script | | | |
| --- | --- | --- | --- |
| given page | ManualSlim | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>SLIM<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define slim.timeout<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define MANUALLY_START_TEST_RUNNER<br>true<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define slim.pool.size<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define SLIM_PORT<br>${FITNESSE_PORT}<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define SLIM_HOST<br>127.0.0.1<br>&#96;&#96;&#96;<br> |
| make | ManualSlim | a test page | |
| test results for page | ManualSlim | should contain | Timeout while reading slim header from client. |
