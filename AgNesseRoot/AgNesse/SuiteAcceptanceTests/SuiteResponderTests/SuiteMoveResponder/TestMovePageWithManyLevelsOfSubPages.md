{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Moving a page keeps every descendant.

| script | | | |
| --- | --- | --- | --- |
| given page | MvDeepParent | with content | x |
| given page | MvDeepParent.MvDeepChild | with content | child page |
| given page | MvDeepParent.MvDeepChild.MvDeepGrand | with content | grandchild page |
| given page | MvDeepNew | with content | x |
| check | request page | MvDeepParent?responder=movePage&newLocation=MvDeepNew | 303 |
| page | MvDeepNew.MvDeepParent | should contain | x |
| page | MvDeepNew.MvDeepParent.MvDeepChild | should contain | child page |
| page | MvDeepNew.MvDeepParent.MvDeepChild.MvDeepGrand | should contain | grandchild page |
