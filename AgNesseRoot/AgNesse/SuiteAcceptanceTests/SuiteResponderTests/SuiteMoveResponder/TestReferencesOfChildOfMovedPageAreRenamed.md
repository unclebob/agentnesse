{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A reference to a child of a moved page follows the child.

| script | | | |
| --- | --- | --- | --- |
| given page | MvKidRefer | with content | MvKidAy.MvKidBee.MvKidCee.MvKidEee |
| given page | MvKidAy | with content | >MvKidBee |
| given page | MvKidAy.MvKidBee | with content | >MvKidCee |
| given page | MvKidAy.MvKidBee.MvKidCee | with content | page C |
| given page | MvKidAy.MvKidBee.MvKidCee.MvKidEee | with content | page E |
| given page | MvKidAy.MvKidDee | with content | page D |
| check | request page | MvKidAy.MvKidBee.MvKidCee?responder=movePage&newLocation=MvKidAy.MvKidDee&refactorReferences=on | 303 |
| page | MvKidRefer | should contain | .MvKidAy.MvKidDee.MvKidCee.MvKidEee |
| check | request page | MvKidAy.MvKidBee.MvKidCee?getPage&dontCreatePage | 404 |
| page | MvKidAy.MvKidDee.MvKidCee.MvKidEee | should contain | page E |
