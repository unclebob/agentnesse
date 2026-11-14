{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
When you execute a test page, `FitNesse` gathers up the classpaths prior to running the test. The classpaths are found in the path blocks of the test page, and every ancestor of the test page. The paths are ordered so that paths found on the subpages come before paths found on their parents.

| Library |
| --- |
| echo fixture |

| script | page driver |
| --- | --- |
| given page | ParentPage | with content | \`\`\`agentnesse<br>path<br>parentPath<br>\`\`\` |
| given page | ParentPage.TestPage | with content | \`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\`<br>\`\`\`agentnesse<br>path<br>classes<br>\`\`\`<br>\`\`\`agentnesse<br>path<br>fitnesse-standalone.jar<br>\`\`\`<br><br>\| Import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br><br>\| script \| java properties \|<br>\| --- \| --- \|<br>\| show \| property \| java.class.path \| |
| make a test page | ParentPage.TestPage |
| page | ParentPage.TestPage?test | should contain | classes${path.separator}fitnesse-standalone.jar${path.separator}parentPath |
