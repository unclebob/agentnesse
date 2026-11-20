{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Dates, times, and period boundaries round-trip

Each check expects the exact ISO-8601 cell. Seed 0 draws the day `2033-12-23T00:00:00Z`, the Sunday `2045-10-08T00:00:00Z`, and the month `2071-09-01T00:00:00Z`. The zoned datetime is noon UTC rendered in New York. `$now` is the clock the table fixed.

| script | page driver |
| --- | --- |
| given page | PropertyClock | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define zone<br>UTC<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| date is \| value \|<br>\| check \| echo string \| @value \| 2020-01-15 \|<br><br>\| property \| date is \|<br>\| trials \| 1 \|<br>\| seed \| 20 \|<br>\| value \| date:2020-01-15..2020-01-15 \|<br><br>\| scenario \| time is \| value \|<br>\| check \| echo string \| @value \| 08:00:00 \|<br><br>\| property \| time is \|<br>\| trials \| 1 \|<br>\| seed \| 21 \|<br>\| value \| time:08:00:00..08:00:00 \|<br><br>\| scenario \| instant is \| value \|<br>\| check \| echo string \| @value \| 2024-06-01T00:00:00Z \|<br><br>\| property \| instant is \|<br>\| trials \| 1 \|<br>\| seed \| 22 \|<br>\| value \| datetime:2024-06-01T00:00:00Z..2024-06-01T00:00:00Z \|<br><br>\| scenario \| zoned is \| value \|<br>\| check \| echo string \| @value \| 2024-06-01T08:00:00-04:00 \|<br><br>\| property \| zoned is \|<br>\| trials \| 1 \|<br>\| seed \| 23 \|<br>\| value \| datetime zone:America/New_York :2024-06-01T12:00:00Z..2024-06-01T12:00:00Z \|<br><br>\| scenario \| soon is \| value \|<br>\| check \| echo string \| @value \| 2024-06-02T11:59:00Z \|<br><br>\| property \| soon is \|<br>\| trials \| 1 \|<br>\| seed \| 24 \|<br>\| now \| 2024-06-01T00:00:00Z \|<br>\| value \| datetime:now+35h59m..now+35h59m \|<br><br>\| scenario \| day boundary is \| value \|<br>\| check \| echo string \| @value \| 2033-12-23T00:00:00Z \|<br><br>\| property \| day boundary is \|<br>\| trials \| 1 \|<br>\| seed \| 0 \|<br>\| value \| period:day \|<br><br>\| scenario \| week boundary is \| value \|<br>\| check \| echo string \| @value \| 2045-10-08T00:00:00Z \|<br><br>\| property \| week boundary is \|<br>\| trials \| 1 \|<br>\| seed \| 0 \|<br>\| value \| period:week \|<br><br>\| scenario \| month boundary is \| value \|<br>\| check \| echo string \| @value \| 2071-09-01T00:00:00Z \|<br><br>\| property \| month boundary is \|<br>\| trials \| 1 \|<br>\| seed \| 0 \|<br>\| value \| period:month \|<br><br>\| scenario \| clock is \| value \|<br>\| check \| echo string \| @value \| 2024-06-01T00:00:00Z \|<br><br>\| property \| clock is \|<br>\| trials \| 1 \|<br>\| seed \| 25 \|<br>\| now \| 2024-06-01T00:00:00Z \|<br>\| value \| now \|<br><br>\| scenario \| read the clock \| instant \|<br>\| check \| echo string \| $now \| 2024-06-01T00:00:00Z \|<br>\| check \| echo string \| @instant \| 2024-06-02T12:00:00Z \|<br><br>\| property \| read the clock \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| now \| 2024-06-01T00:00:00Z \|<br>\| instant \| datetime:2024-06-02T12:00:00Z..2024-06-02T12:00:00Z \| |
| make | PropertyClock | a test page |
| check | request page | PropertyClock?test&format=json | 200 |
| ensure | content matches | "right":10[,}] |
| ensure | content contains | "wrong":0 |
| reject | content contains | "status":"fail" |
