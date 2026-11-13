{
  "type": "test",
  "tags": ["noJunit"],
  "help": null,
  "prune": false
}
If the socket never sends a Slim header, the test stops when slim.timeout elapses.


| script | | | |
| --- | --- | --- | --- |
| given page | TimeoutPage | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>SLIM<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define slim.timeout<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define slim.pool.size<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define SLIM_PORT<br>${FITNESSE_PORT}<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agentnesse<br>define SLIM_FLAGS<br>-v<br>&#96;&#96;&#96;<br> |
| make | TimeoutPage | a test page | |
| test results for page | TimeoutPage | should contain | Timeout while reading slim header from client. |
