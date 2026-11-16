{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# JSON errors

A missing page, an unknown parameter, and a refused move or rename are JSON objects with `status`, `error`, and `message`. `getPage` with `dontCreatePage` is a 404 `notFound`. Removed parameters include `searchType`.

| script | page driver |
| --- | --- |
| check | request page | NoSuchGuidePage?getPage&dontCreatePage | 404 |
| ensure | content contains | "error":"notFound" |
| check | request page | FrontPage?responder=search&searchType=title | 400 |
| ensure | content contains | "error":"badRequest" |
| ensure | content contains | Unknown parameter searchType |
| given page | GuideMove | with content | stay |
| check | request page | GuideMove?responder=movePage&newLocation=GuideMove | 400 |
| ensure | content contains | Cannot move |
| given page | GuideDot | with content | stay |
| check | request page | GuideDot?responder=renamePage&newName=GuideDot.Nested | 400 |
| ensure | content contains | Cannot rename |
