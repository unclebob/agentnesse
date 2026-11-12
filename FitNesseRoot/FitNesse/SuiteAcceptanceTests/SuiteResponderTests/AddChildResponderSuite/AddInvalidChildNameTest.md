{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | | | | | | |
| --- | --- | --- | --- | --- | --- | --- |
| given page | ParentPage | | | | | |
| add child page | bad+name | containing | child content | to parent page | ParentPage | should return BadRequest |
