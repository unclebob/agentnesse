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
