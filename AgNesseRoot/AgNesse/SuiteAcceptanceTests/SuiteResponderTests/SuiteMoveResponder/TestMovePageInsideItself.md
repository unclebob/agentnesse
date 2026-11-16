{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page cannot be moved inside itself.

| script | | | |
| --- | --- | --- | --- |
| given page | MvSelfPage | with content | some content |
| check | request page | MvSelfPage?responder=movePage&newLocation=MvSelfPage | 400 |
| ensure | content contains | Cannot move |
| page | MvSelfPage | should contain | some content |
