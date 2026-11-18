{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Include

An `include` block runs the tables on the named page. The name is a sibling, or a path from this page's parent. The rendered page shows that table. A missing page is named where the table would have been, and the run reports no assertions.

```agnesse
include IncludeTarget
```

| script | page driver |
| --- | --- |
| given page | GuideIncluded | with content | <br><br>\| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| check \| echo \| from-include \| from-include \| |
| given page | GuideInclude | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agnesse<br>include GuideIncluded<br>&#96;&#96;&#96; |
| make | GuideInclude | a test page |
| check | request page | GuideInclude | 200 |
| ensure | content contains | <button type="button" class="disclosure" aria-expanded="false">&gt;</button><a href="GuideIncluded">GuideIncluded</a> |
| ensure | content contains | from-include |
| check | request page | GuideInclude?test&format=json | 200 |
| ensure | content contains | "origin":"GuideIncluded" |
| ensure | content contains | "actual":"from-include" |
| ensure | content matches | "right":1[,}] |
| given page | GuideMissing | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agnesse<br>include MissingGuide<br>&#96;&#96;&#96; |
| make | GuideMissing | a test page |
| check | request page | GuideMissing | 200 |
| ensure | content contains | <button type="button" class="disclosure" aria-expanded="false">&gt;</button>MissingGuide |
| reject | content contains | <a href="MissingGuide"> |
| reject | content contains | from-include |
| check | request page | GuideMissing?test&format=json | 200 |
| ensure | content contains | "status":"empty" |
| ensure | content matches | "right":0[,}] |
