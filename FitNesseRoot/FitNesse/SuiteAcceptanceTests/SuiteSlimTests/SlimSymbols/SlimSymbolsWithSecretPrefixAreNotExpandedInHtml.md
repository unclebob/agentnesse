{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
### When a symbol is prefixed with SECRET, its contents will not be shown upon accessing the symbol


| script | | | |
| --- | --- | --- | --- |
| given page | SecretSymbolTest | with content | \`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\`<br><br><br>\| import \|<br>\| --- \|<br>\| fitnesse.slim.test \|<br>\| fitnesse.fixtures \|<br><br><br>\| script \| echo fixture \| \| \|<br>\| --- \| --- \| --- \| --- \|<br>\| $SECRET_symbol= \| echo \| this is secret info \| \|<br>\| set name \| $SECRET_symbol \| \| \|<br>\| check \| echo \| $SECRET_symbol \| this is secret info \| |
| check | request page | SecretSymbolTest?test | 200 |
| ensure | content contains | $SECRET_symbol->[\*\*\*\*\*] | |
| ensure | content contains | <span class="pass">this is secret info</span> | |
| show | content | | |

