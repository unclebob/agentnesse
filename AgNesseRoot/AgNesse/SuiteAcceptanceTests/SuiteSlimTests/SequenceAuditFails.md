{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An audited leak shrinks the amount

Tweak on a locked record is supposed to be refused. This fixture changes the value and returns `ok`. The amount was drawn from `int:5..20` and shrinks to `5`. The saved page still has the generator.

| script | page driver |
| --- | --- |
| given page | SequenceTweakedLeak | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.AuditedLeak \|<br>\| trials \| 1 \|<br>\| steps \| 2..2 \|<br>\| seed \| 1 \|<br>\| state \| phase=open; value=0 \|<br>\| command \| when \| next \| amount \| result? \| value? \|<br>\| lock \| phase=open \| phase=locked; lockedValue=@value \| \| ok \| @value \|<br>\| tweak \| phase=locked \| \| int:5..20 \| refused \| @value \| |
| make | SequenceTweakedLeak | a test page |
| check | request page | SequenceTweakedLeak?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"ok" |
| ensure | content contains | "expected":"refused" |
| ensure | content contains | "actual":"5" |
| ensure | content contains | "actual":"tweak" |
| ensure | content contains | shrunk |
| ensure | source contains; | SequenceTweakedLeak | int:5..20 |
| reject | source contains; | SequenceTweakedLeak | shrunk |
| reject | source contains; | SequenceTweakedLeak | drew |
