{
  "type": "static",
  "tags": [],
  "help": null,
  "prune": false
}

| scenario | the edit response should contain | contents | |
| --- | --- | --- | --- |
| check | request page | $IT?edit | 200 |
| ensure | content contains | @contents | |
| show | content | | |


| scenario | attempt to edit | page | |
| --- | --- | --- | --- |
| check | request page | @page?edit | 200 |


| scenario | save page | page | with contents | contents | |
| --- | --- | --- | --- | --- | --- |
| check | request page save | @page | with contents | @contents | 303 |
| $IT= | echo | @page | | | |


| scenario | save page | page | with contents | contents | by user | user | | | |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| check | request page save | @page | with contents | @contents | by user | @user | and password | empty | 303 |
| $IT= | echo | @page | | | | | | | |


| scenario | save page | page | by user | user | | | | | |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| check | request page save | @page | with contents | empty | by user | @user | and password | empty | 303 |
| $IT= | echo | @page | | | | | | | |


| scenario | save page | page | |
| --- | --- | --- | --- |
| save page | @page | with contents | empty |


| scenario | it's last modified by should be | user | |
| --- | --- | --- | --- |
| check | last modified of page | $IT | @user |





