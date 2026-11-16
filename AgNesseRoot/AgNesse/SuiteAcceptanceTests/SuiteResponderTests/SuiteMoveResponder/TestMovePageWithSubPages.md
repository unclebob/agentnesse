{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Child pages move with their parent.

| script | | | |
| --- | --- | --- | --- |
| given page | MvSubParent | with content | x |
| given page | MvSubParent.MvSubChild | with content | child page |
| given page | MvSubParent.MvSubChild.MvSubGrand | with content | grandchild page |
| given page | MvSubNew | with content | x |
| check | request page | MvSubParent.MvSubChild?responder=movePage&newLocation=MvSubNew | 303 |
| page | MvSubNew.MvSubChild | should contain | child page |
| page | MvSubNew.MvSubChild.MvSubGrand | should contain | grandchild page |
| check | request page | MvSubParent.MvSubChild?getPage&dontCreatePage | 404 |
