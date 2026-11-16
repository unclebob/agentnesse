{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A sibling reference is rewritten when the target page is renamed.

| script | | | |
| --- | --- | --- | --- |
| given page | RnSibSource | with content | refer to RnSibTarget |
| given page | RnSibTarget | with content | some page |
| check | request page | RnSibTarget?responder=renamePage&newName=RnSibNew&refactorReferences=on | 303 |
| page | RnSibSource | should contain | RnSibNew |
| page | RnSibSource | should not contain | RnSibTarget |
