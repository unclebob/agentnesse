{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An unknown method is an error

A call to a method the fixture does not have returns `NO_METHOD_IN_CLASS`, names the method and the argument count, and lists the methods the server does have. The page that makes the call fails. This page checks that result, so the suite stays green.

| script | page driver |
| --- | --- |
| given page | UnknownMethodPython | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| tiny \|<br>\| check \| noSuch \| ok \| |
| make | UnknownMethodPython | a test page |
| check | request page | UnknownMethodPython?test&format=json | 200 |
| ensure | content contains | NO_METHOD_IN_CLASS |
| ensure | content contains | No Method noSuch[0] in class tiny |
| ensure | content contains | Available methods: |
| ensure | content contains | hello() -> string |

| script | page driver |
| --- | --- |
| given page | UnknownMethodGo | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>go -C slim/go run .<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| slim \|<br>\| check \| noSuch \| ok \| |
| make | UnknownMethodGo | a test page |
| check | request page | UnknownMethodGo?test&format=json | 200 |
| ensure | content contains | No Method noSuch[0] in class slim |
| ensure | content contains | hello() -> string |

| script | page driver |
| --- | --- |
| given page | UnknownMethodRust | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>cargo run --quiet --manifest-path slim/rust/Cargo.toml --<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| slim \|<br>\| check \| noSuch \| ok \| |
| make | UnknownMethodRust | a test page |
| check | request page | UnknownMethodRust?test&format=json | 200 |
| ensure | content contains | No Method noSuch[0] in class slim |

| script | page driver |
| --- | --- |
| given page | UnknownMethodTs | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>node --experimental-strip-types slim/ts/slim.ts<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| slim \|<br>\| check \| noSuch \| ok \| |
| make | UnknownMethodTs | a test page |
| check | request page | UnknownMethodTs?test&format=json | 200 |
| ensure | content contains | No Method noSuch[0] in class slim |
