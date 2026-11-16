{
  "type": "suite",
  "tags": [],
  "help": null,
  "prune": false
}
## `SetUp` and `TearDown`

**`SetUp` and `TearDown` pages behave just like [PageHeader and PageFooter](.AgNesse.SuiteAcceptanceTests.SuiteWikiPageResponderTests) except that they apply only to pages that have the *Test* attribute set.**

Any tables that you want executed **before** every Test page should go in `SetUp`.
Any tables that you want executed **after** every Test page should go in `TearDown`.
---

```agnesse
contents
```
