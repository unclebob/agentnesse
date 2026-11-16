{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Front matter

A page file starts with a JSON object. `type` is `test`, `suite`, or `static`. `tags` is an array of strings. Saving the page writes that object from the page properties.

```json
{
  "type": "test",
  "tags": ["guide"]
}
```

`suiteFilter` runs only the pages that carry the tag. `searchProperties` reads the same fields.

| script | page builder |
| --- | --- |
| line | &#96;&#96;&#96;agnesse |
| line | define SLIM_PORT |
| line | 1 |
| line | &#96;&#96;&#96; |
| attributes | Suite=true |
| markdown page | GuideTags |

| script | page builder |
| --- | --- |
| line | \| script \| agnesse.fixtures.EchoFixture \| |
| line | \| --- \| --- \| --- \| |
| line | \| check \| echo \| hello \| hello \| |
| attributes | Test=true,Suites=guide |
| markdown page | GuideTags.Tagged |

| script | page builder |
| --- | --- |
| line | \| script \| agnesse.fixtures.EchoFixture \| |
| line | \| --- \| --- \| --- \| |
| line | \| check \| echo \| hello \| hello \| |
| attributes | Test=true,Suites=later |
| markdown page | GuideTags.Other |

| script | page driver |
| --- | --- |
| check | request page | GuideTags?suite&format=json&suiteFilter=guide | 200 |
| ensure | content contains | Tagged |
| reject | content contains | Other |
| check | request page | GuideTags?responder=searchProperties&tags=guide&type=test | 200 |
| ensure | content contains | "tags":["guide"] |
| ensure | content contains | Tagged |
| reject | content contains | Other |
