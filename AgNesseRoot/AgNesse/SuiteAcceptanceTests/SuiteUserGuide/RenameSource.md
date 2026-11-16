{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Rename

A rename with `refactorReferences=on` rewrites a reference in the stored page. A `path` block that happens to contain the old name is left as it was.

| script | page driver |
| --- | --- |
| given page | GuideRename | with content | &#96;&#96;&#96;agnesse<br>path GuideOld<br>&#96;&#96;&#96;<br><br>see GuideOld |
| given page | GuideOld | with content | old |
| check | request page | GuideOld?responder=renamePage&newName=GuideNew&refactorReferences=on | 303 |
| ensure | source contains; | GuideRename | path GuideOld |
| ensure | source contains; | GuideRename | see GuideNew |
| reject | source contains; | GuideRename | see GuideOld |
