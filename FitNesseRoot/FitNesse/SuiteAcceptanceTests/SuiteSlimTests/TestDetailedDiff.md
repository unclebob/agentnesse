{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A plain string compare shows a character diff. A regular expression compare does not.


| script | | | |
| --- | --- | --- | --- |
| given page | DiffPage | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; import &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test &#124;<br><br>&#124; Detailed Diff &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; actual string &#124; expected string? &#124; # &#124;<br>&#124; abc &#124; abc &#124; equal &#124;<br>&#124; abc &#124; xyz &#124; different &#124;<br>&#124; abc &#124; xbc &#124; first &#124;<br>&#124; &#124; xyz &#124; empty actual &#124;<br><br>&#124; Detailed Diff &#124; &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; actual string &#124; expected string? &#124; # &#124;<br>&#124; abc &#124; =~/abc/ &#124; match &#124;<br>&#124; abc &#124; =~/^\w{1}$/ &#124; no match &#124;<br><br>&#124; scenario &#124; Detailed Diff Scenario Test &#124; &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; start &#124; Detailed Diff &#124; &#124;<br>&#124; check &#124; echo &#124; hello &#124; hello &#124;<br>&#124; check &#124; echo &#124; hello &#124; hallo &#124;<br><br>&#124; script &#124;<br>&#124; --- &#124;<br>&#124; Detailed Diff Scenario Test &#124;<br> |
| make | DiffPage | a test page | |
| test results for page | DiffPage | should contain | class="pass">abc |
| and should contain | class="diff" | | |
| and should contain | class="fail">expected | | |
| and should contain | class="pass">hello | | |
