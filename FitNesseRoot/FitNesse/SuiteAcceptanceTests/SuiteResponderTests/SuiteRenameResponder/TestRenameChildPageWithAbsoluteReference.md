{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
An absolute reference to a renamed child uses the new name.

| script | | | |
| --- | --- | --- | --- |
| given page | RnAbsParent | with content | refer to .RnAbsParent.RnAbsChild |
| given page | RnAbsParent.RnAbsChild | with content | whatever |
| check | request page | RnAbsParent.RnAbsChild?responder=renamePage&newName=RnAbsNew&refactorReferences=on | 303 |
| page | RnAbsParent | should contain | href="RnAbsParent.RnAbsNew" |
| page | RnAbsParent | should not contain | RnAbsParent.RnAbsChild |
