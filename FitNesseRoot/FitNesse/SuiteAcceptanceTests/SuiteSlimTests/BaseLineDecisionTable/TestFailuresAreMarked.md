{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A baseline decision table marks a wrong value as a failure, a matching blank as ignored, and a near miss as a diff.


| script | | | |
| --- | --- | --- | --- |
| given page | FailureMarks | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; import &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test &#124;<br><br>&#124; baseline: fitnesse.slim.test.Division &#124; &#124; &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124; --- &#124; --- &#124;<br>&#124; numerator &#124; denominator &#124; quotient? &#124; # &#124; &#124;<br>&#124; 10 &#124; 2 &#124; 5.0 &#124; S1 Base scenario &#124; &#124;<br>&#124; 20 &#124; 4 &#124; &#124; S2 Same result as base - correct &#124; &#124;<br>&#124; 20 &#124; 6 &#124; &#124; W3 Same result as base - wrong &#124; &#124;<br>&#124; &#124; 5 &#124; 2.0 &#124; S4 Same numerator as base &#124; &#124;<br>&#124; 16 &#124; &#124; 8.0 &#124; S5 Same denominator as base &#124; &#124;<br>&#124; 22 &#124; 7 &#124; ~=3.14 &#124; S6 Special comparator &#124; &#124;<br>&#124; 9 &#124; 3 &#124; 7 &#124; W7 Wrong result &#124; &#124;<br>&#124; 11 &#124; 2 &#124; 4&lt;_&lt;6 &#124; S8 Special comparator &#124; &#124;<br>&#124; &#124; &#124; &#124; S9 Correct but duplicate &#124; &#124;<br><br>&#96;&#96;&#96;agentnesse<br>define SLIM_DT_BASELINE<br>true<br>&#96;&#96;&#96;<br><br>&#124; fitnesse.slim.test.Division &#124; &#124; &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124; --- &#124; --- &#124;<br>&#124; numerator &#124; denominator &#124; quotient? &#124; # &#124; &#124;<br>&#124; 10 &#124; 2.5 &#124; &#124; S1 Base scenario - ignore &#124; &#124;<br>&#124; 20 &#124; 4 &#124; 5.0 &#124; S2 correct result &#124; &#124;<br>&#124; 20 &#124; 20 &#124; &#124; W3 Same result as base - ignore &#124; &#124;<br> |
| make | FailureMarks | a test page | |
| test results for page | FailureMarks | should contain | 8 right, 2 wrong, 2 ignored, 0 exceptions |
| and should contain | expected [5 | | |
| and should contain | ignore">4.0 | | |
| and should contain | ignore">1.0 | | |
