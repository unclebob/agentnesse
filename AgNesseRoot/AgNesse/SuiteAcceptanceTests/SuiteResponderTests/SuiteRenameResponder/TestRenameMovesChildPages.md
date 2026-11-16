{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Renaming a page keeps its children under the new name.

| script | | | |
| --- | --- | --- | --- |
| given page | RnKidParent | with content | parent |
| given page | RnKidParent.RnKidChild | with content | child page |
| check | request page | RnKidParent?responder=renamePage&newName=RnKidNew | 303 |
| page | RnKidNew.RnKidChild | should contain | child page |
