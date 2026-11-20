{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A check-then-act booking is not linearizable

Two threads book the last seat without a lock. Both can observe it free. The shortest failing schedule is one `book` on each thread. The saved page still has `steps` `2..2` and the seed.

| script | page driver |
| --- | --- |
| given page | SequenceRaceRun | with content | &#96;&#96;&#96;agnesse<br>define SLIM_FLAGS<br>-d<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>0<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.LastSeat \|<br>\| trials \| 4 \|<br>\| threads \| 2..2 \|<br>\| prefix \| 0..0 \|<br>\| steps \| 2..2 \|<br>\| seed \| 1 \|<br>\| state \| seats=1 \|<br>\| command \| when \| next \| result? \|<br>\| book \| seats=1 \| seats=0 \| booked \|<br>\| book \| seats=0 \| \| full \| |
| make | SequenceRaceRun | a test page |
| check | request page | SequenceRaceRun?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | no serial order |
| ensure | content contains | t1 book |
| ensure | content contains | t2 book |
| ensure | content contains | booked |
| ensure | content contains | shrunk |
| ensure | source contains; | SequenceRaceRun | steps \| 2..2 |
| ensure | source contains; | SequenceRaceRun | seed \| 1 |
| reject | source contains; | SequenceRaceRun | shrunk |
| reject | source contains; | SequenceRaceRun | drew |
