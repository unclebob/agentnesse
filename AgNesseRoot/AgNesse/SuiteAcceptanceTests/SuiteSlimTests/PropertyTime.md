{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Dates, times, and period boundaries round-trip

Each check expects the exact ISO-8601 cell. Seed 0 draws the day `2033-12-23T00:00:00Z`, the Sunday `2045-10-08T00:00:00Z`, and the month `2071-09-01T00:00:00Z`. The zoned datetime is noon UTC rendered in New York. `$now` is the clock the table fixed.

````agnesse
define PropertyClock
```agnesse
define SLIM_PORT
1
```

```agnesse
define zone
UTC
```

| script | agnesse.slim.test.TestSlim |

| scenario | date is | value |
| check | echo string | @value | 2020-01-15 |

| property | date is |
| trials | 1 |
| seed | 20 |
| value | date:2020-01-15..2020-01-15 |

| scenario | time is | value |
| check | echo string | @value | 08:00:00 |

| property | time is |
| trials | 1 |
| seed | 21 |
| value | time:08:00:00..08:00:00 |

| scenario | instant is | value |
| check | echo string | @value | 2024-06-01T00:00:00Z |

| property | instant is |
| trials | 1 |
| seed | 22 |
| value | datetime:2024-06-01T00:00:00Z..2024-06-01T00:00:00Z |

| scenario | zoned is | value |
| check | echo string | @value | 2024-06-01T08:00:00-04:00 |

| property | zoned is |
| trials | 1 |
| seed | 23 |
| value | datetime zone:America/New_York :2024-06-01T12:00:00Z..2024-06-01T12:00:00Z |

| scenario | soon is | value |
| check | echo string | @value | 2024-06-02T11:59:00Z |

| property | soon is |
| trials | 1 |
| seed | 24 |
| now | 2024-06-01T00:00:00Z |
| value | datetime:now+35h59m..now+35h59m |

| scenario | day boundary is | value |
| check | echo string | @value | 2033-12-23T00:00:00Z |

| property | day boundary is |
| trials | 1 |
| seed | 0 |
| value | period:day |

| scenario | week boundary is | value |
| check | echo string | @value | 2045-10-08T00:00:00Z |

| property | week boundary is |
| trials | 1 |
| seed | 0 |
| value | period:week |

| scenario | month boundary is | value |
| check | echo string | @value | 2071-09-01T00:00:00Z |

| property | month boundary is |
| trials | 1 |
| seed | 0 |
| value | period:month |

| scenario | clock is | value |
| check | echo string | @value | 2024-06-01T00:00:00Z |

| property | clock is |
| trials | 1 |
| seed | 25 |
| now | 2024-06-01T00:00:00Z |
| value | now |

| scenario | read the clock | instant |
| check | echo string | $now | 2024-06-01T00:00:00Z |
| check | echo string | @instant | 2024-06-02T12:00:00Z |

| property | read the clock |
| trials | 1 |
| seed | 1 |
| now | 2024-06-01T00:00:00Z |
| instant | datetime:2024-06-02T12:00:00Z..2024-06-02T12:00:00Z |
````

| script | page driver |
| --- | --- |
| given page | PropertyClock | with content | ${PropertyClock} |
| make | PropertyClock | a test page |
| check | request page | PropertyClock?test&format=json | 200 |
| ensure | content matches | "right":10[,}] |
| ensure | content contains | "wrong":0 |
| reject | content contains | "status":"fail" |

| script | result view |
| --- | --- |
| show | rendered result |
