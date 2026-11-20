{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A blocked release shrinks to the forbidden step

The fixture releases a blocked item and returns `ok`. The table expects `refused`. Seed 1, trial 2 of 8, shrinks to open, block, release. The saved page still has the commands, not the counterexample.

| script | page driver |
| --- | --- |
| given page | SequenceBlockedLeak | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.ItemLifeLeak \|<br>\| trials \| 8 \|<br>\| steps \| 4..4 \|<br>\| seed \| 1 \|<br>\| state \| phase=new; blocked=no \|<br>\| command \| when \| next \| result? \| phase? \|<br>\| open \| phase=new \| phase=open \| ok \| open \|<br>\| block \| phase=open \| phase=blocked; blocked=yes \| ok \| blocked \|<br>\| ping \| phase=blocked \| \| ok \| blocked \|<br>\| release \| phase=blocked \| \| refused \| blocked \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| blocked=yes \| phase!=released \| released? \| false \| |
| make | SequenceBlockedLeak | a test page |
| check | request page | SequenceBlockedLeak?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | trial 2 of 8 |
| ensure | content contains | step 3 release |
| ensure | content contains | "actual":"ok" |
| ensure | content contains | "expected":"refused" |
| ensure | content contains | "actual":"release" |
| ensure | content contains | shrunk |
| ensure | source contains; | SequenceBlockedLeak | seed \| 1 |
| reject | source contains; | SequenceBlockedLeak | shrunk |
| reject | source contains; | SequenceBlockedLeak | drew |
