{
  "type": "suite",
  "tags": [],
  "help": null,
  "prune": false
}
## *`FitNesse` acceptance tests suites*

| SuiteWidgetTests | *Specify the markup language of wiki pages.* |
| --- | --- |
| SuiteWikiPageResponderTests | *Specify general behavior of all wiki pages.* |
| SuiteEditResponderTests | *Specify the behavior of edit requests.* |
| SuiteResponderTests | *Specify the behavior of the responders.* |
| SuiteSlimTests | *Slim Specific Tests* |
| SuiteTestSystems | Test interaction with custom test systems |

## *Classpaths*
```agentnesse
define SUT_PATH
path lib/*.jar
```
---
## *Port*
FitNesse is running on port: ${FITNESSE_PORT}

Use a non-default port number, so SLIM tests systems can be run as well:

```agentnesse
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
```agentnesse
define COLLAPSE_SETUP
true
```
```agentnesse
define COLLAPSE_TEARDOWN
true
```

```agentnesse
define TITLE_TAG
li
```
