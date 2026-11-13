{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A new name may not contain a dot. The page stays where it is.

| script | | | |
| --- | --- | --- | --- |
| given page | RnDotPage | with content | X |
| given page | RnDotTarget | with content | X |
| check | request page | RnDotPage?responder=renamePage&newName=RnDotTarget.RnDotSome | 400 |
| ensure | content contains | Cannot rename |
| check | request page | RnDotTarget.RnDotSome?getPage&dontCreatePage | 404 |
| page | RnDotPage | should contain | X |
