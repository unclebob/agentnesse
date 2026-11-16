{
  "type": "suite",
  "tags": [],
  "help": null,
  "prune": false
}
## *`AgNesse` acceptance tests suites*

| Suite | Description |
| --- | --- |
| >SuiteWidgetTests | *Specify the markup language of wiki pages.* |
| >SuiteWikiPageResponderTests | *Specify general behavior of all wiki pages.* |
| >SuiteEditResponderTests | *Specify the behavior of edit requests.* |
| >SuiteResponderTests | *Specify the behavior of the responders.* |
| >SuiteSlimTests | *Slim Specific Tests* |

## *Classpaths*
```agnesse
define SUT_PATH
path lib/*.jar
```
---
## *Port*
`AgNesse` is running on port: ${AGNESSE_PORT}

Use a non-default port number, so SLIM tests systems can be run as well:

```agnesse
define SLIM_PORT
1
```
---
## *Utilities*
 - SuiteAcceptanceTests.PageHeader
 - SuiteAcceptanceTests.PageFooter
 - SuiteAcceptanceTests.SetUp
 - SuiteAcceptanceTests.TearDown

---
## Collapse Set Up and Tear Down
```agnesse
define COLLAPSE_SETUP
true
```
```agnesse
define COLLAPSE_TEARDOWN
true
```

```agnesse
define TITLE_TAG
li
```
