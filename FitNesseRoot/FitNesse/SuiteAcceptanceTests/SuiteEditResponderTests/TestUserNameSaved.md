{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
The username is saved when a page is edited. It is the optional `user` front matter field.


| script | | | |
| --- | --- | --- | --- |
| save page | MyPage | with contents | hello | by user | ann |
| it's last modified by should be | ann | | |
| save page | MyPage | with contents | hello again | by user | bob |
| it's last modified by should be | bob | | |
