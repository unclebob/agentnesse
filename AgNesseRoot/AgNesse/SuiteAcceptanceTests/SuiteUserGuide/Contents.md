{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Contents

A `contents` block renders a link to each child page.

| script | page driver |
| --- | --- |
| given page | GuideContents | with content | &#96;&#96;&#96;agnesse<br>contents<br>&#96;&#96;&#96; |
| given page | GuideContents.Child | with content | child |
| page | GuideContents | should have link to | GuideContents.Child |
