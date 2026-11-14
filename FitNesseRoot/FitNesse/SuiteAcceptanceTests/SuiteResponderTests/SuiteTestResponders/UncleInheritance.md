{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A `SetUp`, `TearDown`, `SuiteSetUp`, `SuiteTearDown`, and `ScenarioLibrary` that belong to an ancestor are applied to a grandchild. The SetUp has to run: the grandchild names `test slim`, which exists only because that SetUp imports `fitnesse.slim.test`.

| Library |
| --- |
| page driver |
| echo fixture |

| script | page driver |
| --- | --- |
| given page | UncleInherit | with content | ${SUT_PATH}<br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\` |
| given page | UncleInherit.SetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br>\| fitnesse.slim.test \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| uncle setup \| uncle setup \| |
| given page | UncleInherit.TearDown | with content | \| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| uncle teardown \| uncle teardown \| |
| given page | UncleInherit.Mid | with content | mid |
| given page | UncleInherit.Mid.Leaf | with content | \| script \| test slim \|<br>\| --- \|<br>\| check \| echo string \| hi \| hi \| |
| make a test page | UncleInherit.Mid.Leaf |
| given page | UncleNear | with content | ${SUT_PATH}<br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\` |
| given page | UncleNear.SetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br>\| fitnesse.slim.test \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| far setup \| far setup \| |
| given page | UncleNear.Mid | with content | mid |
| given page | UncleNear.Mid.SetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br>\| fitnesse.slim.test \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| near setup \| near setup \| |
| given page | UncleNear.Mid.Leaf | with content | \| script \| test slim \|<br>\| --- \|<br>\| check \| echo string \| hi \| hi \| |
| make a test page | UncleNear.Mid.Leaf |
| given page | UncleSuite | with content | ${SUT_PATH}<br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\` |
| make a suite page | UncleSuite |
| given page | UncleSuite.SuiteSetUp | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| suite uncle setup \| suite uncle setup \| |
| given page | UncleSuite.SuiteTearDown | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| suite uncle teardown \| suite uncle teardown \| |
| given page | UncleSuite.Mid | with content | mid |
| given page | UncleSuite.Mid.Child | with content | \| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| child ran \| child ran \| |
| make a test page | UncleSuite.Mid.Child |
| given page | UncleLibrary | with content | ${SUT_PATH}<br>\`\`\`agentnesse<br>define TEST_SYSTEM<br>slim<br>\`\`\` |
| given page | UncleLibrary.ScenarioLibrary | with content | \| scenario \| from the uncle \|<br>\| --- \|<br>\| check \| echo \| from uncle \| from uncle \| |
| given page | UncleLibrary.Mid | with content | mid |
| given page | UncleLibrary.Mid.Leaf | with content | \| import \|<br>\| --- \|<br>\| fitnesse.fixtures \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| from the uncle \| |
| make a test page | UncleLibrary.Mid.Leaf |
| page | UncleInherit.Mid.Leaf?test | should match | uncle setup.*hi.*uncle teardown |
| page | UncleNear.Mid.Leaf?test | should contain | near setup |
| page | UncleNear.Mid.Leaf?test | should not contain | far setup |
| page | UncleSuite?suite | should match | suite uncle setup.*child ran.*suite uncle teardown |
| page | UncleLibrary.Mid.Leaf?test | should contain | from uncle |
