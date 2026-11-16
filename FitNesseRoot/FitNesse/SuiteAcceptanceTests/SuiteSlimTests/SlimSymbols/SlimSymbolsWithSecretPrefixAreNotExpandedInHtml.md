{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
### When a symbol is prefixed with SECRET, its contents will not be shown upon accessing the symbol


| script | | | |
| --- | --- | --- | --- |
| given page | SecretSymbolTest | with content | <br><br><br>\| import \|<br>\| --- \|<br>\| agnesse.slim.test \|<br>\| agnesse.fixtures \|<br><br><br>\| script \| echo fixture \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| $SECRET_symbol= \| echo \| this is secret info \| \|<br>\| set name \| $SECRET_symbol \| \| \|<br>\| check \| echo \| $SECRET_symbol \| this is secret info \| |
| check | request page | SecretSymbolTest?test | 200 |
| ensure | content contains | "symbol":"SECRET_symbol" | |
| ensure | content contains | "value":"*****" | |
| ensure | content contains | this is secret info | |
| ensure | content contains | "status":"pass" | |
| show | content | | |

