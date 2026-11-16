{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| Library |
| --- |
| page driver |
| echo fixture |

| script | page driver |
| --- | --- |
| given page | SuiteParent | with content | ${SUT_PATH}<br> |
| make a suite page | SuiteParent |
| given page | SuiteParent.SuiteChildOne | with content | |
| make a suite page | SuiteParent.SuiteChildOne |
| given page | SuiteParent.SuiteChildOne.SuiteSetUp | with content | |
| given page | SuiteParent.SuiteChildOne.SuiteTearDown | with content | |
| given page | SuiteParent.SuiteChildOne.TestOneOne | with content | |
| make a test page | SuiteParent.SuiteChildOne.TestOneOne |
| given page | SuiteParent.SuiteChildOne.TestOneTwo | with content | |
| make a test page | SuiteParent.SuiteChildOne.TestOneTwo |
| given page | SuiteParent.SuiteChildOne.TestOneThree | with content |  |
| make a test page | SuiteParent.SuiteChildOne.TestOneThree |
| given page | SuiteParent.SuiteChildTwo | with content | |
| make a suite page | SuiteParent.SuiteChildTwo |
| given page | SuiteParent.SuiteChildTwo.SuiteSetUp | with content | |
| given page | SuiteParent.SuiteChildTwo.SuiteTearDown | with content | |
| given page | SuiteParent.SuiteChildTwo.TestTwoOne | with content | |
| make a test page | SuiteParent.SuiteChildTwo.TestTwoOne |
| given page | SuiteParent.SuiteChildTwo.TestTwoTwo | with content | |
| make a test page | SuiteParent.SuiteChildTwo.TestTwoTwo |
| given page | SuiteParent.SuiteChildThree | with content | |
| make a suite page | SuiteParent.SuiteChildThree |
| given page | SuiteParent.SuiteChildThree.TestThreeOne | with content | |
| make a test page | SuiteParent.SuiteChildThree.TestThreeOne |
| given page | SuiteParent.SuiteChildThree.TestThreeTwo | with content | |
| make a test page | SuiteParent.SuiteChildThree.TestThreeTwo |

| script |
| --- |
| check | request page | SuiteParent?suite&format=json | 200 |

| ordered query:pages run in suite | SuiteChildOne | slim |
| --- | --- | --- |
| page name | | |
| SuiteChildOne.SuiteSetUp | | |
| SuiteChildOne.TestOneOne | | |
| SuiteChildOne.TestOneThree | | |
| SuiteChildOne.TestOneTwo | | |
| SuiteChildOne.SuiteTearDown | | |

| ordered query:pages run in suite | SuiteChildTwo | slim |
| --- | --- | --- |
| page name | | |
| SuiteChildTwo.SuiteSetUp | | |
| SuiteChildTwo.TestTwoOne | | |
| SuiteChildTwo.TestTwoTwo | | |
| SuiteChildTwo.SuiteTearDown | | |

| ordered query:pages run in suite | SuiteChildThree | slim |
| --- | --- | --- |
| page name | | |
| SuiteChildThree.TestThreeOne | | |
| SuiteChildThree.TestThreeTwo | | |
