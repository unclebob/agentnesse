{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
An include of a moved page is rewritten, and the included text is still shown.

| script | | | |
| --- | --- | --- | --- |
| given page | MvIncParent | with content | x |
| given page | MvIncParent.MvIncSub | with content | sub page |
| given page | MvIncNew | with content | x |
| given page | MvIncRefer | with content | start<br>&#96;&#96;&#96;agentnesse<br>include MvIncParent.MvIncSub<br>&#96;&#96;&#96;<br>end |
| check | request page | MvIncParent.MvIncSub?responder=movePage&newLocation=MvIncNew&refactorReferences=on | 303 |
| page | MvIncRefer | should contain | .MvIncNew.MvIncSub |
| page | MvIncRefer | should contain | sub page |
