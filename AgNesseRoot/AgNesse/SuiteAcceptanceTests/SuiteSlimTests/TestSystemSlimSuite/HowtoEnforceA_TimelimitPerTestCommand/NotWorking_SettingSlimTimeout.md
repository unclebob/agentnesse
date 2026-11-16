{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

Setting slim.timeout doesn't works :( 
```agnesse
define slim.timeout
1
```


This table should fails as the duration is above the slim.timeout but is does not :(

| agnesse.slim.test.Sleep | |
| --- | --- |
| Timer | do Sleep ? |
| 12000 | WakeUp 12000 |


