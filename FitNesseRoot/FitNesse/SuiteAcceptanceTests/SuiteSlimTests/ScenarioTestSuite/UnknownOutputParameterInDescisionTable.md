{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
An output column that the scenario does not define is an error, and the row does not pass.


| script | | | |
| --- | --- | --- | --- |
| given page | UnknownOutput | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; scenario &#124; my division &#124; numerator &#124; &#124; denominator &#124; &#124; my result2? &#124;<br>&#124; --- &#124; --- &#124; --- &#124; --- &#124; --- &#124; --- &#124; --- &#124;<br>&#124; set numerator &#124; @numerator &#124; &#124; &#124; &#124; &#124;<br>&#124; set denominator &#124; @denominator &#124; &#124; &#124; &#124; &#124;<br>&#124; $myResult2= &#124; quotient &#124; &#124; &#124; &#124; &#124;<br><br>&#124; Library &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test.Division &#124;<br><br>&#124; my division &#124; &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124; --- &#124;<br>&#124; numerator &#124; denominator &#124; my result2? &#124; missing? &#124;<br>&#124; 10 &#124; 2 &#124; 5.0 &#124; 3 &#124;<br> |
| make | UnknownOutput | a test page | |
| test results for page | UnknownOutput | should contain | not an output of the scenario |
