{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
If we do a remote debug the slim client writes messages to standard out / err before the slim header is written.
The Slim Test Server must skip these until the Slim Header is received.


```agentnesse
define TEST_SYSTEM
SLIM:remote_debug
```
```agentnesse
define COMMAND_PATTERN
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -cp %p %m
```
```agentnesse
define SLIM_PORT
1
```


| import |
| --- |
| fitnesse.slim.test |


| test slim | 3 |
| --- | --- |
| string | getStringArg? |
| €Bob€ | €Bob€ |
| Köln | Köln |

