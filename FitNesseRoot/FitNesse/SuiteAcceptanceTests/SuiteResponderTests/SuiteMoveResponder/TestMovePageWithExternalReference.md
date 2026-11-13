{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A reference from another page is rewritten, and the old location is gone.

| script | | | |
| --- | --- | --- | --- |
| given page | MvExtParent | with content | x |
| given page | MvExtParent.MvExtSub | with content | sub page |
| given page | MvExtNew | with content | x |
| given page | MvExtRefer | with content | MvExtParent.MvExtSub |
| check | request page | MvExtParent.MvExtSub?responder=movePage&newLocation=MvExtNew&refactorReferences=on | 303 |
| page | MvExtNew.MvExtSub | should contain | sub page |
| check | request page | MvExtParent.MvExtSub?getPage&dontCreatePage | 404 |
| page | MvExtRefer | should contain | .MvExtNew.MvExtSub |
