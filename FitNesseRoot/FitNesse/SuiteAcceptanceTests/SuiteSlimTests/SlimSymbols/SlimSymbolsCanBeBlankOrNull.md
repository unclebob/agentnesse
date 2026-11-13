{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A symbol can hold null, and a symbol can hold a blank. A blank value is stored as an empty symbol. An empty expected cell cannot be written, so the blank is checked with an empty pattern.

| import |
| --- |
| fitnesse.slim.test |


| null fixture | |
| --- | --- |
| get null? | # |
| $NULL= | . |
| null | . |


| script | test slim | | |
| --- | --- | --- | --- |
| check | echo string | $NULL | null |


| null fixture | |
| --- | --- |
| get blank? | # |
| $BLANK= | . |


| script | test slim | | |
| --- | --- | --- | --- |
| check | echo string | $BLANK | =~/^$/ |
