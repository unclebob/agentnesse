{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# all of runs the domain

`all of:red,blue` is two trials, not a sample of 100. The round trip passes. The echo that expects `nope` fails on the first case, `red`, for seed 1 and the trials cell does not record a shrink. A cross product of two columns is four trials.

````agnesse
define PropertyRoles
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | round trip | value |
| check | echo string | @value | @value |

| property | round trip |
| seed | 1 |
| value | all of:red,blue |

| scenario | echo back | word |
| check | echo string | @word | nope |

| property | echo back |
| seed | 9 |
| word | all of:red,blue |

| scenario | pair | left | and | right |
| check | echo string | @left | @left |

| property | pair and |
| seed | 4 |
| left | all of:a,b |
| right | all of:1,2 |
````

| script | page driver |
| --- | --- |
| given page | PropertyRoles | with content | ${PropertyRoles} |
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

| script | result view |
| --- | --- |
| show | rendered result |
