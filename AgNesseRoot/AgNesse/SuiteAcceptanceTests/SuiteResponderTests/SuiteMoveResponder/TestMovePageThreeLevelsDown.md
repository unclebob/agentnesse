{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page several levels down can be moved, and a relative reference is rewritten.

| script | | | |
| --- | --- | --- | --- |
| given page | MvThreeAy | with content | >MvThreeBee |
| given page | MvThreeAy.MvThreeBee | with content | >MvThreeCee |
| given page | MvThreeAy.MvThreeDee | with content | page D |
| given page | MvThreeAy.MvThreeBee.MvThreeCee | with content | page C |
| check | request page | MvThreeAy.MvThreeBee.MvThreeCee?responder=movePage&newLocation=MvThreeAy.MvThreeDee&refactorReferences=on | 303 |
| page | MvThreeAy.MvThreeBee | should contain | .MvThreeAy.MvThreeDee.MvThreeCee |
| check | request page | MvThreeAy.MvThreeBee.MvThreeCee?getPage&dontCreatePage | 404 |
| page | MvThreeAy.MvThreeDee.MvThreeCee | should contain | page C |
