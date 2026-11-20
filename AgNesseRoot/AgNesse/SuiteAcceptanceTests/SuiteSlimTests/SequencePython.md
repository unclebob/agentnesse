{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A Python gate refuses a blocked release

The wiki draws the sequence and sends string cells. The Python server keeps one gate per instance, created by make at phase new. Release from blocked is refused. Six trials, one assertion.

| script | page driver |
| --- | --- |
| given page | SequencePyGate | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:tiny \|<br>\| trials \| 6 \|<br>\| steps \| 1..4 \|<br>\| seed \| 1 \|<br>\| state \| phase=new \|<br>\| command \| when \| next \| result? \| phase? \|<br>\| open \| phase=new \| phase=open \| ok \| open \|<br>\| block \| phase=open \| phase=blocked \| ok \| blocked \|<br>\| release \| phase=open \| phase=released \| ok \| released \|<br>\| release \| phase=blocked \| \| refused \| blocked \|<br>\| invariant \| when \| model \| query \| expect \|<br>\| \| phase=blocked \| phase!=released \| released? \| false \| |
| make | SequencePyGate | a test page |
| check | request page | SequencePyGate?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"6 trials" |
| ensure | source contains; | SequencePyGate | phase=blocked |
| reject | source contains; | SequencePyGate | shrunk |
