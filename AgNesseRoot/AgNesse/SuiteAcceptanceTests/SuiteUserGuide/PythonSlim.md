{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Slim in another language

`COMMAND_PATTERN` is the process `AgNesse` starts. The first token must not contain `java`, or `AgNesse` treats it as a Java command. `AgNesse` appends the Slim port. Port `1` means the protocol is stdin and stdout, which is how this suite runs. Any other port is a socket on `127.0.0.1`.

The script answers `world` from `hello`. The header is `Slim -- V0.5` and each message is a six-digit length, a colon, and the bytes.

The same protocol is spoken by the servers in `slim/go`, `slim/rust`, and `slim/ts`. Each one has a decision table, a property table, and a sequence table. A concurrent sequence sends one `lanes` instruction on the single connection. Go and Rust run a real thread per lane. The `slim/ts` server has no real threads, so it runs the lanes as async tasks on Node's one event loop. That choice is written in the server and on its acceptance page. A `.js` file uses the `slim/ts` server: `node` runs the `.ts` file.

```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

| script | tiny |
| --- | --- |
| check | hello | world |
