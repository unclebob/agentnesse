{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A blocked item is never released

Open, block, and release are drawn under their preconditions. Release from blocked returns `refused`, and the invariant says a blocked item is not released. Eight trials, one assertion. The saved page keeps the seed.

| script | page driver |
| --- | --- |
| given page | SequenceLifeRun | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.ItemLife \|<br>\| trials \| 8 \|<br>\| steps \| 1..4 \|<br>\| seed \| 1 \|<br>\| state \| phase=new; blocked=no \|<br>\| command \| when \| next \| result? \| phase? \|<br>\| open \| phase=new \| phase=open \| ok \| open \|<br>\| block \| phase=open \| phase=blocked; blocked=yes \| ok \| blocked \|<br>\| release \| phase=open \| phase=released \| ok \| released \|<br>\| release \| phase=blocked \| \| refused \| blocked \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| blocked=yes \| phase!=released \| released? \| false \| |
| make | SequenceLifeRun | a test page |
| check | request page | SequenceLifeRun?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"8 trials" |
| ensure | source contains; | SequenceLifeRun | seed \| 1 |
| reject | source contains; | SequenceLifeRun | shrunk |
