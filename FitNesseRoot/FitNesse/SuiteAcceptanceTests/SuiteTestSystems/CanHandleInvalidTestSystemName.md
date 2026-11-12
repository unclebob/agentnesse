{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | | | |
| --- | --- | --- | --- |
| create page | TestPage | with content | \`\`\`agentnesse<br>define TEST_SYSTEM<br>foo<br>\`\`\` |


| script | | | |
| --- | --- | --- | --- |
| check | request page | TestPage?test | 200 |
| ensure | content contains | Unknown test system: 'foo' | |
| show | content | | |
