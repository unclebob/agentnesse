{
  "type": "test",
  "tags": [],
  "help": "fitnesse.testsystems.slim.results.SlimExceptionResult",
  "prune": false
}
`SlimExceptionResult` keeps the text inside `message:<< >>` and translates the known Slim flags.

| import |
| --- |
| fitnesse.testsystems.slim.results |


| scenario | message of | exception | is | message | |
| --- | --- | --- | --- | --- | --- |
| start | Slim Exception Result | NOT IMPORTANT | @exception | |
| check | get message | @message | | |


| script | | | |
| --- | --- | --- | --- |
| message of | Hello FitNesse | is | null |
| message of | message:<<Hello FitNesse>> | is | Hello FitNesse |
| message of | message:<<NO_CONVERTER_FOR_ARGUMENT_NUMBER java.time.temporal.TemporalAccessor. ignores the rest>> | is | No converter for java.time.temporal.TemporalAccessor. |
| message of | message:<<COULD_NOT_INVOKE_CONSTRUCTOR TestSlim[1] ignores the rest>> | is | Could not invoke constructor for TestSlim[1] |
| message of | message:<<NO_METHOD_IN_CLASS missing[1] fitnesse.slim.test.TestSlim.>> | is | Method missing[1] not found in fitnesse.slim.test.TestSlim. |
