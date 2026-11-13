{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A markdown link that uses a child prefix is rewritten.

| script | | | |
| --- | --- | --- | --- |
| given page | RnAlSubParent | with content | refer to [[link][^RnAlSubChild]] |
| given page | RnAlSubParent.RnAlSubChild | with content | whatever |
| check | request page | RnAlSubParent.RnAlSubChild?responder=renamePage&newName=RnAlSubNew&refactorReferences=on | 303 |
| page | RnAlSubParent | should contain | href="RnAlSubParent.RnAlSubNew" |
| page | RnAlSubParent | should not contain | RnAlSubChild |
