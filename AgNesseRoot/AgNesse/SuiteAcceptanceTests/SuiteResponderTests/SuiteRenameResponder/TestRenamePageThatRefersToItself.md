{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page that names itself is updated when it is renamed.

| script | | | |
| --- | --- | --- | --- |
| given page | RnSelfPage | with content | refer to self RnSelfPage |
| check | request page | RnSelfPage?responder=renamePage&newName=RnSelfNew&refactorReferences=on | 303 |
| page | RnSelfNew | should contain | RnSelfNew |
| page | RnSelfNew | should not contain | RnSelfPage |
