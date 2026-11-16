{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | | | | | | |
| --- | --- | --- | --- | --- | --- | --- |
| given page | ParentPage | | | | | |
| add child page | | containing | child content | to parent page | ParentPage | should return BadRequest |
