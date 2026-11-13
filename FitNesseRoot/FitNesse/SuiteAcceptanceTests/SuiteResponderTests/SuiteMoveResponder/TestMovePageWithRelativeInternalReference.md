{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A relative reference on the old parent is rewritten to the new absolute path.

| script | | | |
| --- | --- | --- | --- |
| given page | MvRelParent | with content | >MvRelSub |
| given page | MvRelParent.MvRelSub | with content | sub page |
| given page | MvRelNew | with content | x |
| given page | MvRelRefer | with content | MvRelParent.MvRelSub |
| check | request page | MvRelParent.MvRelSub?responder=movePage&newLocation=MvRelNew&refactorReferences=on | 303 |
| page | MvRelParent | should contain | .MvRelNew.MvRelSub |
| page | MvRelRefer | should contain | .MvRelNew.MvRelSub |
