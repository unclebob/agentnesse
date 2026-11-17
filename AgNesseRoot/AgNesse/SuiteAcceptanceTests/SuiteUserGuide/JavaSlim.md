{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Java Slim

The default command is `java`. A script table runs in that process. `java.specification.version` is answered by the JVM, which a command that is not Java cannot do.

| script | page driver |
| --- | --- |
| given page | GuideJava | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.fixtures.JavaProperties \|<br>\| --- \| --- \|<br>\| ensure \| property should match; \| java.specification.version \| 21 \| |
| make | GuideJava | a test page |
| check | request page | GuideJava?test&format=json | 200 |
| ensure | content matches | "command":"[^"]*java |
| reject | content contains | tiny_slim.py |
| ensure | content matches | "right":1[,}] |
| reject | content contains | "status":"empty" |
| reject | content contains | "status":"error" |
