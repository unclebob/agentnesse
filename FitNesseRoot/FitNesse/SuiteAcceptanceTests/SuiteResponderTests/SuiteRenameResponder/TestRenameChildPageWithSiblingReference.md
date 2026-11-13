{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A sibling page's reference is rewritten, including a self reference.

| script | | | |
| --- | --- | --- | --- |
| given page | RnBroParent | with content | refer to nothing |
| given page | RnBroParent.RnBroBrother | with content | you are my RnBroSister |
| given page | RnBroParent.RnBroSister | with content | thanks brother, love RnBroSister |
| check | request page | RnBroParent.RnBroSister?responder=renamePage&newName=RnBroNew&refactorReferences=on | 303 |
| page | RnBroParent.RnBroBrother | should contain | RnBroNew |
| page | RnBroParent.RnBroBrother | should not contain | RnBroSister |
| page | RnBroParent.RnBroNew | should contain | RnBroNew |
| page | RnBroParent.RnBroNew | should not contain | RnBroSister |
