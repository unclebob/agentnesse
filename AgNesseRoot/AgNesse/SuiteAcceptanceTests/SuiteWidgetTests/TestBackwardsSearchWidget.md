{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
## The Backwards Search widget.
Sometimes we want to search backwards through a path to a named page.  For example, if we are on the page `.PageOne.PageTwo.PageThree.PageFour` we might be able to say `<PageTwo` to search backwards to `PageTwo`.  Now let's say there is a page named `.PageOne.PageTwo.AnotherPage` and we are on `.PageOne.PageTwo.PageThree.PageFour`.  We could say `<PageTwo.AnotherPage`.


| script | | | |
| --- | --- | --- | --- |
| given page | PageOne.PageTwo.AnotherPage | | |
| | | | |
| given page | PageOne.PageTwo.PageThree.PageFour | with content | <PageTwo.AnotherPage |
| it should have link to | PageOne.PageTwo.AnotherPage | | |
| | | | |
| given page | PageOne.PageTwo | with content | <NoSuchPage |
| it should have creating link to | NoSuchPage | | |
| | | | |
| given page | PageOne.PageX | with content | <PageOne.NoSuchPage |
| it should have creating link to | PageOne.NoSuchPage | | |
