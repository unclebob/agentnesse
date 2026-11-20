{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Sequence

A sequence table lists commands. `AgNesse` draws a sequence for each trial, runs it on a fresh fixture, and checks the model. One sequence is one assertion, however many trials and steps run.

Name the fixture the way a decision table does. The class can also sit in the next cell, and cells after that are constructor arguments. Optional `trials`, `steps`, `seed`, and `state` rows come next, then the commands, then the invariants. The whole specification is one Markdown table. A blank line starts a new table, so do not leave one inside this table.

`trials` defaults to 100 when the row is omitted or the cell is blank. `steps` is `low..high`. The default is `1..8`. Each bound is from 1 through 32. Leave `seed` blank and the result shows the seed that was used. It is not written back onto the page. Copy that number into the cell to draw the same sequences again. `state` is `name=value` pairs separated by `;`. A sequence argument cannot draw a time.

A command row is the name, `when`, `next`, then arguments and checks. `when` is a precondition on the model. Every clause is `name=value`, and the value is a literal, so the draw stays a function of the seed. A command is eligible only when every clause holds. `next` is the model after the command, assignments separated by `;`, applied left to right. `@name` reads the model or an argument of this command. An argument column uses the property cell language and is sent to Slim. An empty argument cell is left out of the call. A column that ends in `?` or `!` is a check. `result?` is the value the command returned, not a method named `result`. Any other `name?` calls that no-arg query after the command.

An invariant row is `when`, `model`, `query`, and `expect`. It runs after a command whose `when` matches. `model` compares the model after `next`. It does not see the fixture, so a fixture that drifts is caught by `result?` or by a query. `query` is a no-arg fixture call, and `expect` is what it must return. `@name` in an expected cell is filled from the model.

A missing command, an unknown `@name`, a precondition that can never hold, and a steps range outside 1 through 32 are refused before any trial.

This item can be opened, blocked, or released. A blocked item cannot be released.

| sequence:agnesse.slim.test.ItemLife |
| --- |
| trials | 8 |
| steps | 1..4 |
| seed | 1 |
| state | phase=new; blocked=no |
| command | when | next | result? | phase? |
| open | phase=new | phase=open | ok | open |
| block | phase=open | phase=blocked; blocked=yes | ok | blocked |
| release | phase=open | phase=released | ok | released |
| release | phase=blocked | | refused | blocked |
| invariant | when | model | query | expect |
| | blocked=yes | phase!=released | released? | false |

A pass colors the trials cell (`8 trials`). The seed cell shows `1`.

A record can be set while it is open. Once it is locked, a plain set or a tweak is refused, and the value changes only through an audited adjustment. `lockedValue=@value` on `lock` stores the value as it stands at that assignment. The invariant applies while the record is locked and not yet audited.

| sequence:agnesse.slim.test.AuditedRecord |
| --- |
| trials | 6 |
| steps | 1..5 |
| seed | 3 |
| state | phase=open; value=0; audited=no |
| command | when | next | amount | note | result? | value? |
| set | phase=open | value=@amount | int:0..9 | | ok | @value |
| lock | phase=open | phase=locked; lockedValue=@value | | | ok | @value |
| set | phase=locked | | int:0..9 | | refused | @value |
| adjust | phase=locked | value=@value+@amount; audited=yes | int:1..3 | pattern:AUD-[0-9]{2} | ok | @value |
| tweak | phase=locked | | int:1..3 | | refused | @value |
| invariant | when | model | query | expect |
| | phase=locked; audited=no | value=@lockedValue | value? | @lockedValue |

Each trial builds a new fixture. The model is checked in `AgNesse`, and the cells are ordinary strings, so the same table runs on any Slim server. This Python process keeps one gate per instance. Release from blocked is refused.

| script | page driver |
| --- |
| given page | SequenceGuidePy | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:tiny \|<br>\| trials \| 6 \|<br>\| steps \| 1..4 \|<br>\| seed \| 1 \|<br>\| state \| phase=new \|<br>\| command \| when \| next \| result? \| phase? \|<br>\| open \| phase=new \| phase=open \| ok \| open \|<br>\| block \| phase=open \| phase=blocked \| ok \| blocked \|<br>\| release \| phase=open \| phase=released \| ok \| released \|<br>\| release \| phase=blocked \| \| refused \| blocked \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| phase=blocked \| phase!=released \| released? \| false \| |
| make | SequenceGuidePy | a test page |
| check | request page | SequenceGuidePy?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"6 trials" |
| ensure | source contains; | SequenceGuidePy | phase=blocked |
| reject | source contains; | SequenceGuidePy | shrunk |

## Shrinking

After the first failing trial, shrinking tries a shorter sequence before it changes an argument. A drop is kept only when the remaining commands are still allowed by their preconditions. A shorter length is tried when there are at most 64 ways to choose those commands, so the walk is not every subset. Arguments then shrink with the same shrinkers a property uses. The result appends the kept sequence, one row per command, and an invariant row when that is what failed. The trials cell names the trial, the commands that were drawn, and the failing step or invariant. It says `shrunk` when a shorter failure was kept, and `shrink cap` after 32 accepted steps. The seed stays on the result. The saved page still has the generators.

This fixture releases a blocked item anyway. The model says that return is `refused`, so the check fails. The shortest failure is open, block, release.

| script | page driver |
| --- |
| given page | SequenceGuideLeak | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.ItemLifeLeak \|<br>\| trials \| 8 \|<br>\| steps \| 4..4 \|<br>\| seed \| 1 \|<br>\| state \| phase=new; blocked=no \|<br>\| command \| when \| next \| result? \| phase? \|<br>\| open \| phase=new \| phase=open \| ok \| open \|<br>\| block \| phase=open \| phase=blocked; blocked=yes \| ok \| blocked \|<br>\| ping \| phase=blocked \| \| ok \| blocked \|<br>\| release \| phase=blocked \| \| refused \| blocked \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| blocked=yes \| phase!=released \| released? \| false \| |
| make | SequenceGuideLeak | a test page |
| check | request page | SequenceGuideLeak?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | trial 2 of 8 |
| ensure | content contains | step 3 release |
| ensure | content contains | "actual":"ok" |
| ensure | content contains | "expected":"refused" |
| ensure | content contains | "actual":"release" |
| ensure | content contains | shrunk |
| ensure | source contains; | SequenceGuideLeak | seed \| 1 |
| reject | source contains; | SequenceGuideLeak | shrunk |
| reject | source contains; | SequenceGuideLeak | drew |

A locked record's tweak is supposed to be refused. This one changes the value. The amount shrinks to `5`, and the page still says `int:5..20`.

| script | page driver |
| --- |
| given page | SequenceGuideAudit | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:agnesse.slim.test.AuditedLeak \|<br>\| trials \| 1 \|<br>\| steps \| 2..2 \|<br>\| seed \| 1 \|<br>\| state \| phase=open; value=0 \|<br>\| command \| when \| next \| amount \| result? \| value? \|<br>\| lock \| phase=open \| phase=locked; lockedValue=@value \| \| ok \| @value \|<br>\| tweak \| phase=locked \| \| int:5..20 \| refused \| @value \| |
| make | SequenceGuideAudit | a test page |
| check | request page | SequenceGuideAudit?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"ok" |
| ensure | content contains | "expected":"refused" |
| ensure | content contains | "actual":"5" |
| ensure | content contains | "actual":"tweak" |
| ensure | content contains | shrunk |
| ensure | source contains; | SequenceGuideAudit | int:5..20 |
| reject | source contains; | SequenceGuideAudit | shrunk |
| reject | source contains; | SequenceGuideAudit | drew |
