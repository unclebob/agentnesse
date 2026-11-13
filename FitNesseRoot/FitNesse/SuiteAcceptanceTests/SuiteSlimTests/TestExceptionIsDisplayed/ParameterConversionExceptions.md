{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A value that cannot be converted names the target type. A valid call still passes.


| script | | | |
| --- | --- | --- | --- |
| given page | ConvertPage | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>&#124; import &#124;<br>&#124; --- &#124;<br>&#124; fitnesse.slim.test &#124;<br>&#124; java.lang &#124;<br><br>&#124; script &#124; test slim &#124;<br>&#124; --- &#124; --- &#124;<br>&#124; one int &#124; twenty &#124;<br><br>&#124; script &#124; java.time.Instant.ofEpochSecond &#124; Hello &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br><br>&#124; script &#124; java.lang.String.valueOf &#124; 5 &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br><br>&#124; script &#124; Boolean &#124; 0 &#124;<br>&#124; --- &#124; --- &#124; --- &#124;<br>&#124; $ok= &#124; String.valueOf &#124; 5 &#124;<br>&#124; $bad= &#124; java.time.Instant.ofEpochSecond &#124; aWrongValue &#124;<br>&#124; $bad= &#124; java.time.Instant.from &#124; 9.8 &#124;<br> |
| make | ConvertPage | a test page | |
| test results for page | ConvertPage | should contain | Can't convert twenty to integer |
| and should contain | Can't convert Hello to long | | |
| and should contain | class="pass" | | |
| and should contain | Can't convert aWrongValue to long | | |
| and should contain | No converter for java.time.temporal.TemporalAccessor | | |
