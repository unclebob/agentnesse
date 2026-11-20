{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A locked record changes only through an audited adjustment

Set is allowed while the record is open. After lock, set and tweak are refused, and adjust adds an audited amount. The invariant holds the locked value until that adjustment. Six trials, one assertion.

| script | page driver |
| --- | --- |
| given page | SequenceAuditRun | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.AuditedRecord \|<br>\| trials \| 6 \|<br>\| steps \| 1..5 \|<br>\| seed \| 3 \|<br>\| state \| phase=open; value=0; audited=no \|<br>\| command \| when \| next \| amount \| note \| result? \| value? \|<br>\| set \| phase=open \| value=@amount \| int:0..9 \| \| ok \| @value \|<br>\| lock \| phase=open \| phase=locked; lockedValue=@value \| \| \| ok \| @value \|<br>\| set \| phase=locked \| \| int:0..9 \| \| refused \| @value \|<br>\| adjust \| phase=locked \| value=@value+@amount; audited=yes \| int:1..3 \| pattern:AUD-[0-9]{2} \| ok \| @value \|<br>\| tweak \| phase=locked \| \| int:1..3 \| \| refused \| @value \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| phase=locked; audited=no \| value=@lockedValue \| value? \| @lockedValue \| |
| make | SequenceAuditRun | a test page |
| check | request page | SequenceAuditRun?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"6 trials" |
| ensure | source contains; | SequenceAuditRun | seed \| 3 |
| ensure | source contains; | SequenceAuditRun | int:0..9 |
| reject | source contains; | SequenceAuditRun | shrunk |
