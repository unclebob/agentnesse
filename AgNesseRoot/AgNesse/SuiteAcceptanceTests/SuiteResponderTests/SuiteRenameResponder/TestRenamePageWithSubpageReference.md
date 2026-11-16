{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A child reference written with > is rewritten and still links to the child.

| script | | | |
| --- | --- | --- | --- |
| given page | RnSubParent | with content | refer to >RnSubChild |
| given page | RnSubParent.RnSubChild | with content | whatever |
| check | request page | RnSubParent.RnSubChild?responder=renamePage&newName=RnSubNew&refactorReferences=on | 303 |
| page | RnSubParent | should contain | >RnSubNew |
| page | RnSubParent | should contain | href="RnSubParent.RnSubNew" |
| page | RnSubParent | should not contain | >RnSubChild |
