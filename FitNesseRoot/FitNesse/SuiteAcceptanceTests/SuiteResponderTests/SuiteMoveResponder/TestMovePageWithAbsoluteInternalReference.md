{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
An absolute reference is rewritten when the page moves.

| script | | | |
| --- | --- | --- | --- |
| given page | MvAbsParent | with content | .MvAbsParent.MvAbsSub |
| given page | MvAbsParent.MvAbsSub | with content | sub page |
| given page | MvAbsNew | with content | x |
| check | request page | MvAbsParent.MvAbsSub?responder=movePage&newLocation=MvAbsNew&refactorReferences=on | 303 |
| page | MvAbsParent | should contain | .MvAbsNew.MvAbsSub |
