{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | | | | | | |
| --- | --- | --- | --- | --- | --- | --- |
| given page | ParentPage | | | | | |
| add child page | ChildPage | containing | child content | to parent page | ParentPage | should return Redirect |
| page | ParentPage.ChildPage | should contain | child content | | | |
