{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A stop-test exception marks the rest of the page ignored, including a later table.


| script | | | |
| --- | --- | --- | --- |
| given page | IgnoredColor | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; import &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test &#124;<br><br>&#124; scenario &#124; Stop Test &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; start &#124; Constructor Throws &#124; stop test &#124;<br><br>&#124; scenario &#124; Not Executed &#124; message &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; check &#124; echo &#124; @message &#124; cannot fail @message &#124;<br><br>&#124; script &#124;<br>&#124; --- &#124;<br>&#124; Stop Test &#124;<br>&#124; Not Executed and should be ignored &#124;<br><br>&#124; script &#124;<br>&#124; --- &#124;<br>&#124; Not Executed and should be ignored as well &#124;<br> |
| make | IgnoredColor | a test page | |
| test results for page | IgnoredColor | should contain | class="error" |
| and should contain | Not Executed and should be ignored | | |
| and should contain | class="ignore" | | |
| and should contain | Test not run | | |
