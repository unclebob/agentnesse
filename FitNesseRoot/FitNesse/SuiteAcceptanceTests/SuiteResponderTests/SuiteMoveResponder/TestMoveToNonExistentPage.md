{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Moving under a parent that does not exist is rejected.

| script | | | |
| --- | --- | --- | --- |
| given page | MvMissParent | with content | x |
| given page | MvMissParent.MvMissSub | with content | sub page |
| check | request page | MvMissParent.MvMissSub?responder=movePage&newLocation=MvMissMissing | 400 |
| ensure | content contains | Cannot move |
| page | MvMissParent.MvMissSub | should contain | sub page |
