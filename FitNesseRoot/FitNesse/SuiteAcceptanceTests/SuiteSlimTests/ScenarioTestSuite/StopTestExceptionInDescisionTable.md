{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A stop-test exception in a decision table skips the rest of that table.


| script | | | |
| --- | --- | --- | --- |
| given page | StopDecision | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; import &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test &#124;<br><br>&#124; scenario &#124; my division _ &#124; numerator, denominator, my result3? &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; set numerator &#124; @numerator &#124; &#124;<br>&#124; set denominator &#124; @denominator &#124; &#124;<br>&#124; start &#124; Constructor Throws &#124; stop test &#124;<br>&#124; $myResult3= &#124; quotient &#124; &#124;<br><br>&#124; Library &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test.Division &#124;<br><br>&#124; my division &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; numerator &#124; denominator &#124; my result3? &#124;<br>&#124; 10 &#124; 2 &#124; 5.0 &#124;<br>&#124; 20 &#124; 5 &#124; 4.0 &#124;<br> |
| make | StopDecision | a test page | |
| test results for page | StopDecision | should contain | class="error" |
| and should contain | Test not run | | |
| and should contain | class="ignore" | | |
