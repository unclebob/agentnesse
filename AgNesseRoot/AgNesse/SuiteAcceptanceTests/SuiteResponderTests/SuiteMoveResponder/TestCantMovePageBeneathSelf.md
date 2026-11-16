{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page cannot be moved under one of its own children.

| script | | | |
| --- | --- | --- | --- |
| given page | MvCantParent | with content | x |
| given page | MvCantParent.MvCantChild | with content | child page |
| given page | MvCantParent.MvCantChild.MvCantGrand | with content | grandchild page |
| check | request page | MvCantParent.MvCantChild?responder=movePage&newLocation=MvCantParent.MvCantChild.MvCantGrand | 400 |
| ensure | content contains | Cannot move |
| page | MvCantParent.MvCantChild | should contain | child page |
| page | MvCantParent.MvCantChild.MvCantGrand | should contain | grandchild page |
