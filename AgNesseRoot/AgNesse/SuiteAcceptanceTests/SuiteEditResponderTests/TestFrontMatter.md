{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
## The edit form shows front matter, and the JSON is what a save stores.

| script | page driver |
| --- | --- |
| given page | FrontMatterPage | with content | original |
| check | request page | FrontMatterPage?edit | 200 |
| ensure | content contains | Front Matter ▸ |
| ensure | content contains | name="frontMatter" |
| ensure | content contains | "type": "static" |
| ensure | content contains | original |
| reject | content contains | name="helpText" |
| reject | content contains | id="front-matter" open |
| check | request page save | FrontMatterPage | with contents | changed | and front matter | {"type":"suite","tags":["later"],"help":"how","prune":true} | 303 |
| check | request page | FrontMatterPage?responder=searchProperties&type=suite&tags=later&help=how&prune=true | 200 |
| ensure | content contains | "tags":["later"] |
| ensure | content contains | "type":"suite" |
| ensure | content contains | "help":"how" |
| ensure | content contains | "prune":true |
| ensure | content contains | FrontMatterPage |
| check | request page | FrontMatterPage?edit | 200 |
| ensure | content contains | changed |
| check | request page save | FrontMatterPage | with contents | BADBODY | and front matter | { | 400 |
| ensure | content contains | malformed JSON |
| ensure | content contains | BADBODY |
| ensure | content contains | Front Matter ▾ |
| check | request page | FrontMatterPage?edit | 200 |
| ensure | content contains | changed |
| reject | content contains | BADBODY |
| check | request page save | FrontMatterPage | with contents | changed | and front matter | {"type":"wiki"} | 400 |
| ensure | content contains | wiki |
| given page | FrontMatterParent | with content | parent |
| check | request page | FrontMatterParent?new | 200 |
| ensure | content contains | Front Matter ▸ |
| ensure | content contains | "type": null |
| ensure | content contains | pageType_Default |
| ensure | content contains | pageType_test-adjunct |
| ensure | content contains | var adjunctNames = ['SetUp', 'TearDown', 'SuiteSetUp', 'SuiteTearDown', 'ScenarioLibrary']; |
| check | request page | FrontMatterParent?new&pageType=Test | 200 |
| ensure | content contains | "type": "test" |
| ensure | content contains | name="pageType" value="Test" |
| check | request new page named with contents of type and front matter; | FrontMatterParent | JsonChild | hello | Suite | {"type":"test","tags":[],"help":null,"prune":false} | 303 |
| check | page | FrontMatterParent.JsonChild | has attribute | Test | true |
| check | page | FrontMatterParent.JsonChild | has attribute | Suite | false |
| check | request new page named with contents of type and front matter; | FrontMatterParent | BadChild | kept-token | Suite | { | 400 |
| ensure | content contains | malformed JSON |
| ensure | content contains | kept-token |
| ensure | content contains | value="BadChild" |
| check | request page | FrontMatterParent.BadChild?getPage&dontCreatePage | 404 |
| check | request new page named with contents of type and front matter; | FrontMatterParent | TestInferred | body | Static | {"type":null,"tags":[],"help":null,"prune":false} | 303 |
| check | page | FrontMatterParent.TestInferred | has attribute | Test | true |
| check | request page | FrontMatterParent?new&pageType=test-adjunct | 200 |
| ensure | content contains | "type": "test-adjunct" |
| check | request new page named with contents of type and front matter; | FrontMatterParent | SetUp | body | Default | {"type":null,"tags":[],"help":null,"prune":false} | 303 |
| check | page | FrontMatterParent.SetUp | has attribute | test-adjunct | true |
| check | page | FrontMatterParent.SetUp | has attribute | Test | false |
| check | request new page named with contents of type and front matter; | FrontMatterParent | PageHeader | body | Default | {"type":null,"tags":[],"help":null,"prune":false} | 303 |
| check | page | FrontMatterParent.PageHeader | has attribute | test-adjunct | false |
| check | page | FrontMatterParent.PageHeader | has attribute | Test | false |
| check | request new page named with contents of type and front matter; | FrontMatterParent | Helper | body | Static | {"type":"test-adjunct","tags":[],"help":null,"prune":false} | 303 |
| check | page | FrontMatterParent.Helper | has attribute | test-adjunct | true |
| check | page | FrontMatterParent.Helper | has attribute | Test | false |

| script | page builder |
| line | &bar; SetUp &bar; |
| line | &bar; --- &bar; |
| markdown page | SetUp |

| script | page driver |
| check | request page | SetUp?test | 400 |
| ensure | content contains | does not run |
| check | request page | SetUp | 200 |
| ensure | content contains | data-slim="SetUp">SetUp</td> |
| reject | content contains | accesskey="t">Test |
