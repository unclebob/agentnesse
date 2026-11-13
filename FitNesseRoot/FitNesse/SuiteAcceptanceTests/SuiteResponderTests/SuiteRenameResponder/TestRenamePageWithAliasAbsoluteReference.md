{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A markdown link that names a page with an absolute path is rewritten.

| script | | | |
| --- | --- | --- | --- |
| given page | RnAlAbsParent | with content | refer to [[link][.RnAlAbsParent.RnAlAbsChild]] |
| given page | RnAlAbsParent.RnAlAbsChild | with content | whatever |
| check | request page | RnAlAbsParent.RnAlAbsChild?responder=renamePage&newName=RnAlAbsNew&refactorReferences=on | 303 |
| page | RnAlAbsParent | should contain | href="RnAlAbsParent.RnAlAbsNew" |
| page | RnAlAbsParent | should not contain | RnAlAbsChild |
