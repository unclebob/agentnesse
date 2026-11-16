{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
If a `SuiteSetUp` or `SuiteTearDown` page is executed without errors, it should be counted as a correctly executed page.

| Library |
| --- |
| page driver |
| echo fixture |

| script | page driver |
| --- | --- |
| given page | SuiteParent | with content | ${SUT_PATH}<br> |
| make a suite page | SuiteParent |
| given page | SuiteParent.SuiteSetUp | with content | |
| given page | SuiteParent.SuiteTearDown | with content | |
| given page | SuiteParent.TestOneOne | with content | \| script \| fitnesse.fixtures.Pass \|<br>\| --- \|<br>\| ensure \| ok \| |
| make a test page | SuiteParent.TestOneOne |
| given page | SuiteParent.TestOneTwo | with content | \| script \| fitnesse.fixtures.Pass \|<br>\| --- \|<br>\| ensure \| ok \| |
| make a test page | SuiteParent.TestOneTwo |

For normal (HTML) responses:

| script |
| --- |
| check | request page | SuiteParent?suite | 200 |
| show | content |
| ensure | content contains | 4 right, 0 wrong, 0 ignored, 0 exceptions |
| ensure | content contains | SuiteParent.SuiteSetUp |
| ensure | content contains | SuiteParent.SuiteTearDown |
