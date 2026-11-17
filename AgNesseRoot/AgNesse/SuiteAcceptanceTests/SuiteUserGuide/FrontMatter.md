{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Front matter

A page file starts with a JSON object. `type` is `test`, `suite`, `static`, or `test-adjunct`. `tags` is an array of strings. Saving the page writes that object from the page properties.

A test-adjunct page is included into a test (`SetUp`, `TearDown`, `SuiteSetUp`, `SuiteTearDown`, or `ScenarioLibrary`): its tables are Slim tables, and requesting `?test` on it is an error.

The Edit page shows that object in a Front Matter section above the editor. The section starts collapsed. Saving parses the JSON into the page's type, tags, help, and prune. Invalid JSON or an invalid type does not save: the edit page opens the section, shows the error, and keeps the text you typed. A new page starts with the front matter for the selected type, and changing the type updates that JSON. The JSON is what is stored. Leaving the section untouched leaves the file's front matter as it was written.

```json
{
  "type": "test",
  "tags": ["guide"]
}
```

`suiteFilter` runs only the pages that carry the tag. `searchProperties` reads the same fields. `Tagged` is tagged `guide` and checks `guide-tagged`. `Other` is tagged `later` and checks `guide-other`.

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
| line | \| check \| echo \| guide-tagged \| guide-tagged \| |
| attributes | Test=true,Suites=guide |
| markdown page | GuideTags.Tagged |

| script | page builder |
| --- | --- |
| line | \| script \| agnesse.fixtures.EchoFixture \| |
| line | \| --- \| --- \| --- \| |
| line | \| check \| echo \| guide-other \| guide-other \| |
| attributes | Test=true,Suites=later |
| markdown page | GuideTags.Other |

| script | page driver |
| --- | --- |
| check | request page | GuideTags?suite&format=json&suiteFilter=guide | 200 |
| ensure | content contains | GuideTags.Tagged |
| reject | content contains | GuideTags.Other |
| ensure | content contains | "actual":"guide-tagged" |
| reject | content contains | "actual":"guide-other" |
| ensure | content matches | "pages":1[,}] |
| ensure | content matches | "right":1[,}] |
| reject | content contains | "status":"empty" |
| check | request page | GuideTags?responder=searchProperties&tags=guide&type=test | 200 |
| ensure | content contains | "tags":["guide"] |
| ensure | content contains | GuideTags.Tagged |
| reject | content contains | GuideTags.Other |
