{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A markdown link to a sibling page is rewritten.

| script | | | |
| --- | --- | --- | --- |
| given page | RnAlSource | with content | refer to [[link][RnAlTarget]] |
| given page | RnAlTarget | with content | whatever |
| check | request page | RnAlTarget?responder=renamePage&newName=RnAlNew&refactorReferences=on | 303 |
| page | RnAlSource | should contain | href="RnAlNew" |
| page | RnAlSource | should not contain | RnAlTarget |
