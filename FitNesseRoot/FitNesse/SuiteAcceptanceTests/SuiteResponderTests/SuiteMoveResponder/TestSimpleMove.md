{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page can be moved under a new parent.

| script | | | |
| --- | --- | --- | --- |
| given page | MvSimpleParent | with content | x |
| given page | MvSimpleParent.MvSimpleSub | with content | sub page |
| given page | MvSimpleNew | with content | x |
| check | request page | MvSimpleParent.MvSimpleSub?responder=movePage&newLocation=MvSimpleNew | 303 |
| page | MvSimpleNew.MvSimpleSub | should contain | sub page |
| check | request page | MvSimpleParent.MvSimpleSub?getPage&dontCreatePage | 404 |
