{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Define

A `define` block sets a variable. The value replaces `${NAME}` in a Slim cell before the fixture sees the cell. A name with no definition is left as it was written.

| script | page driver |
| --- | --- |
| given page | GuideDefine | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agnesse<br>define GREETING<br>hello<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| set name \| ${GREETING} \|<br>\| check \| name \| hello \|<br>\| set name \| ${NOT_DEFINED} \|<br>\| check \| name \| ${NOT_DEFINED} \| |
| make | GuideDefine | a test page |
| check | request page | GuideDefine?test&format=json | 200 |
| ensure | content contains | "actual":"hello" |
| ensure | content contains | "actual":"${NOT_DEFINED}" |
| ensure | content matches | "right":2[,}] |
| reject | content contains | "actual":"${GREETING}" |
