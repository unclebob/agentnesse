{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Clojure check-then-act booking is not linearizable

`racy` reads the seat, waits, then writes it. Two threads can both read it free. The page that runs the table fails and shrinks. This page checks that result, so the suite stays green. The saved table still has the generators.

| script | page driver |
| --- | --- |
| given page | ClojureRaceRun | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>slim/clj/run<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| sequence:slim \| racy \|<br>\| trials \| 4 \|<br>\| threads \| 2..2 \|<br>\| prefix \| 0..0 \|<br>\| steps \| 2..2 \|<br>\| seed \| 1 \|<br>\| state \| seats=1 \|<br>\| command \| when \| next \| result? \|<br>\| book \| seats=1 \| seats=0 \| booked \|<br>\| book \| seats=0 \| \| full \| |
| make | ClojureRaceRun | a test page |
| check | request page | ClojureRaceRun?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | no serial order |
| ensure | content contains | shrunk |
| ensure | source contains; | ClojureRaceRun | steps \| 2..2 |
| reject | source contains; | ClojureRaceRun | shrunk |
