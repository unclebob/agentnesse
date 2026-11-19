{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# all of runs the domain

`all of:red,blue` is two trials, not a sample of 100. The round trip passes. The echo that expects `nope` fails on the first case, `red`, for seed 1 and the trials cell does not record a shrink. A cross product of two columns is four trials.

| script | page driver |
| --- | --- |
| given page | PropertyRoles | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| round trip \| value \|<br>\| check \| echo string \| @value \| @value \|<br><br>\| property \| round trip \|<br>\| seed \| 1 \|<br>\| value \| all of:red,blue \|<br><br>\| scenario \| echo back \| word \|<br>\| check \| echo string \| @word \| nope \|<br><br>\| property \| echo back \|<br>\| seed \| 9 \|<br>\| word \| all of:red,blue \|<br><br>\| scenario \| pair \| left \| and \| right \|<br>\| check \| echo string \| @left \| @left \|<br><br>\| property \| pair and \|<br>\| seed \| 4 \|<br>\| left \| all of:a,b \|<br>\| right \| all of:1,2 \| |
| make | PropertyRoles | a test page |
| check | request page | PropertyRoles?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content matches | "right":2[,}] |
| ensure | content contains | "actual":"2 trials" |
| ensure | content contains | "actual":"4 trials" |
| ensure | content contains | "actual":"red" |
| ensure | content contains | trial 1 of 2 |
| reject | content contains | shrunk |
| ensure | source contains; | PropertyRoles | all of:red,blue |
| reject | source contains; | PropertyRoles | trial 1 of 2 |
