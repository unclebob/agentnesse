import assert from "node:assert/strict"
import net from "node:net"
import test from "node:test"
import { Readable, Writable } from "node:stream"
import { answer, commandPort, expandString, parse, readLength, rejected, run, serialize, serve, speak, status, whole, type Cell } from "./slim.ts"

function t(text: string): Cell { return text }
function call(id: string, instance: string, method: string, args: string[] = []): Cell {
  return [id, "call", instance, method, ...args]
}
function cell(value: Cell[], index: number): string {
  const row = value[index]
  return Array.isArray(row) ? String(row[1]) : ""
}
function id(value: Cell[], name: string): string {
  for (const row of value) {
    if (Array.isArray(row) && row[0] === name) return String(row[1])
  }
  return ""
}

test("hello, echo, a symbol, and ports", async () => {
  const instances = new Map()
  const symbols = new Map<string, string>()
  assert.equal(cell(await answer([call("1", "actor", "hello")], instances, symbols), 0), "world")
  assert.equal(cell(await answer([call("2", "actor", "echo", ["hi"])], instances, symbols), 0), "hi")
  assert.equal(cell(await answer([["3", "callAndAssign", "symbol", "actor", "hello"]], instances, symbols), 0), "world")
  assert.equal(cell(await answer([call("4", "actor", "echo", ["$symbol"])], instances, symbols), 0), "world")
  assert.equal(cell(await answer([["5", "call", "actor", "echo", ["$symbol"]]], instances, symbols), 0), "OK")
  assert.deepEqual(await answer(["nope"], instances, symbols), [])
  assert.equal(cell(await answer([["9", "note"]], instances, symbols), 0), "OK")
  assert.equal(status(false), 0)
  assert.equal(status(true), 1)
  assert.equal(rejected(0), false)
  assert.equal(rejected(-1), true)
  assert.equal(commandPort([]), "bad")
  assert.equal(commandPort(["slim", "nope"]), "bad")
  assert.equal(commandPort(["slim", "-1"]), "bad")
  assert.equal(commandPort(["slim", "1"]), "pipe")
  assert.equal(commandPort(["slim", "9"]), 9)
  assert.equal(await run([]), 1)
  const pipeOut = new Writable({ write(_c, _e, done) { done() } })
  assert.equal(await run(["slim", "1"], Readable.from([Buffer.from("000003:bye")]), pipeOut), 0)
  const input = Readable.from([Buffer.from(`${frame(call("1", "actor", "hello"))}000000:000003:bye`)])
  const chunks: Buffer[] = []
  const output = new Writable({ write(chunk, _enc, done) { chunks.push(Buffer.from(chunk)); done() } })
  await speak(input, output, new Map())
  const text = Buffer.concat(chunks).toString("utf8")
  assert.match(text, /^Slim -- V0\.5\n/)
  assert.match(text, /world/)
  assert.match(text, /\[000000:\]/)
  const longOut = new Writable({ write(_c, _e, done) { done() } })
  const eleven = "0".repeat(11)
  await speak(Readable.from([Buffer.from(eleven)]), longOut, new Map())
  const ten = `${"0".repeat(9)}6:hello!000003:bye`
  const tenOut: Buffer[] = []
  await speak(Readable.from([Buffer.from(ten)]), new Writable({ write(chunk, _e, done) { tenOut.push(Buffer.from(chunk)); done() } }), new Map())
  assert.match(Buffer.concat(tenOut).toString("utf8"), /\[000000:\]/)
  const empty = new Writable({ write(_c, _e, done) { done() } })
  await speak(Readable.from([]), empty, new Map())
})

test("comparisons, kind, and symbol edges", async () => {
  const gate = () => answer([], new Map(), new Map())
  await gate()
  const symbols = new Map<string, string>()
  const instances = new Map()
  const cases: [Cell, string][] = [
    [call("1", "a", "endIsAfterStart", ["0", "1"]), "yes"],
    [call("2", "a", "endIsAfterStart", ["2", "2"]), "no"],
    [call("3", "a", "endIsAfterStart", ["4", "1"]), "no"],
    [call("4", "a", "endIsAfterStart", ["a", "1"]), "no"],
    [call("5", "a", "endIsAfterStart"), "no"],
    [call("6", "a", "endIsAfterStart", ["1"]), "no"],
    [call("8", "a", "kind", ['{"doc":{"kind":"id"}}']), "id"],
    [call("9", "a", "kind", ['{"kind":"passport"}']), "no"],
    [call("10", "a", "kind"), "no"],
  ]
  for (const [row, want] of cases) {
    assert.equal(cell(await answer([row], instances, symbols), 0), want)
  }
  assert.equal(cell(await answer([["7", "call", "a", "endIsAfterStart", ["1"], "2"]], instances, symbols), 0), "no")
  assert.equal(whole(["1"]), undefined)
  assert.equal(whole("a"), undefined)
  assert.equal(whole("4"), 4)
  assert.equal(expandString("$", symbols), "$")
  symbols.set("blank", "")
  assert.equal(expandString("$blank", symbols), "")
  symbols.set("kept", "yes")
  assert.equal(expandString("pre $kept tail $missing", symbols), "pre yes tail $missing")
  for (const name of ["A", "Z", "a", "z", "a0", "a9", "a1", "a_b"]) {
    symbols.set(name, "YES")
    assert.equal(expandString(`$${name}`, symbols), "YES")
  }
})

test("java symbol forms", async () => {
  const symbols = new Map<string, unknown>([["ab", "YES"], ["Äpfel", "fruit"], ["`hello world`", "hi"]])
  assert.equal(expandString("$abc", symbols), "YESc")
  assert.equal(expandString("x$abc", symbols), "xYESc")
  assert.equal(expandString("$Äpfel!", symbols), "fruit!")
  assert.equal(expandString("$`hello world`!", symbols), "hi!")
  assert.equal(expandString("$missing", symbols), "$missing")
  assert.equal(expandString("  $name=  ", symbols), "")
  const instances = new Map()
  assert.equal(cell(await answer([["1", "call", "again", "plus", "2", "3"]], instances, symbols), 0), "5")
  assert.equal(cell(await answer([["2", "call", "again", "flag", "true"]], instances, symbols), 0), "yes")
  assert.equal(cell(await answer([["3", "call", "again", "join", "[red, blue]"]], instances, symbols), 0), "red,blue")
  await answer([["4", "assign", "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", "again\\.echo"]], instances, symbols)
  await answer([["5", "make", "again", "slim"]], instances, symbols)
  assert.equal(cell(await answer([["6", "call", "again", "echo", "$ab"]], instances, symbols), 0), "$ab")
  await answer([["7", "assign", "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", "["]], instances, symbols)
  assert.equal(cell(await answer([["8", "call", "again", "echo", "$ab"]], instances, symbols), 0), "YES")
  await answer([["9", "make", "gate", "slim"]], instances, symbols)
  symbols.set("actor", instances.get("gate"))
  await answer([["10", "callAndAssign", "copy", "SlimHelperLibrary", "cloneSymbol", "$actor"]], instances, symbols)
  await answer([["11", "make", "same", "$copy"]], instances, symbols)
  await answer([["12", "call", "gate", "open"]], instances, symbols)
  assert.equal(cell(await answer([["13", "call", "same", "phase"]], instances, symbols), 0), "open")
  await answer([["14", "make", "scriptTableActor", "slim"]], instances, symbols)
  await answer([["15", "call", "SlimHelperLibrary", "pushFixture"]], instances, symbols)
  await answer([["16", "make", "scriptTableActor", "slim"]], instances, symbols)
  await answer([["17", "call", "gate", "setWord", "other"]], instances, symbols)
  await answer([["18", "call", "SlimHelperLibrary", "pushFixture"]], instances, symbols)
  await answer([["19", "make", "scriptTableActor", "slim", "safe"]], instances, symbols)
  await answer([["20", "call", "SlimHelperLibrary", "popFixture"]], instances, symbols)
  await answer([["21", "call", "SlimHelperLibrary", "popFixture"]], instances, symbols)
  assert.equal(cell(await answer([["22", "call", "scriptTableActor", "phase"]], instances, symbols), 0), "new")
  assert.equal(cell(await answer([["23", "call", "again", "day", "2020-01-01"]], instances, symbols), 0), "2020-01-01")
  assert.equal(cell(await answer([["24", "call", "again", "day", "2020-01-00"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["25", "call", "again", "shade", "green"]], instances, symbols), 0), "green")
  assert.equal(cell(await answer([["26", "call", "again", "shade", "pink"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["27", "call", "again", "label", [["name", "Ada"]]]], instances, symbols), 0), "Ada")
  assert.equal(cell(await answer([["28", "call", "again", "wide", "4"]], instances, symbols), 0), "4")
  assert.equal(cell(await answer([["29", "call", "again", "scale", "1.5"]], instances, symbols), 0), "1.5")
  assert.equal(cell(await answer([["30", "call", "again", "initial", "Q"]], instances, symbols), 0), "Q")
  assert.equal(cell(await answer([["31", "call", "again", "plus", "x", "3"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["32", "call", "again", "plus", "2", "x"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["33", "call", "again", "plus", "2"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["34", "call", "again", "scale", ""]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["35", "call", "again", "scale", "abc"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["36", "call", "again", "join", "[red"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["37", "call", "again", "join", "red]"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["38", "call", "again", "day", "nope"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["39", "call", "again", "day", "2020-00-01"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["40", "call", "again", "day", "2020-13-01"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["41", "call", "again", "day", "2020-12-01"]], instances, symbols), 0), "2020-12-01")
  assert.equal(cell(await answer([["42", "call", "again", "day", "2020-01-32"]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["43", "call", "again", "day", "2020-01-31"]], instances, symbols), 0), "2020-01-31")
  assert.equal(cell(await answer([["44", "call", "again", "label", [[{ 0: "name", 1: "Ada" }]]]], instances, symbols), 0), "no")
  assert.equal(cell(await answer([["44b", "call", "again", "label", [["name", "$ab"]]]], instances, symbols), 0), "YES")
  assert.equal(cell(await answer([["45", "call", "SlimHelperLibrary", "popFixture"]], instances, symbols), 0), "OK")
  assert.equal(cell(await answer([["46", "call", "scriptTableActor", "phase"]], instances, symbols), 0), "new")
  symbols.set("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", null)
  await answer([["47", "assign", "other", "x"]], instances, symbols)
  assert.equal(cell(await answer([["48", "call", "again", "echo", "$ab"]], instances, symbols), 0), "YES")
  await answer([["49", "assign", "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", "again\\.echo"]], instances, symbols)
  symbols.set("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", undefined)
  await answer([["50", "assign", "other", "x"]], instances, symbols)
  assert.equal(cell(await answer([["51", "call", "again", "echo", "$ab"]], instances, symbols), 0), "YES")
  await answer([["52", "make", "decoy", "slim"]], instances, symbols)
  await answer([["53", "call", "decoy", "open"]], instances, symbols)
  symbols.set("lim", instances.get("decoy"))
  await answer([["54", "make", "plain", "slim"]], instances, symbols)
  assert.equal(cell(await answer([["55", "call", "plain", "phase"]], instances, symbols), 0), "new")
  symbols.set("klass", "slim")
  await answer([["56", "make", "named", "$klass"]], instances, symbols)
  assert.equal(cell(await answer([["57", "call", "named", "phase"]], instances, symbols), 0), "new")
  await answer([["58", "make", "absent", "$missing"]], instances, symbols)
  assert.equal(cell(await answer([["59", "call", "absent", "phase"]], instances, symbols), 0), "new")
  symbols.set("q", instances.get("decoy"))
  await answer([["60", "make", "alias", "$q"]], instances, symbols)
  assert.equal(cell(await answer([["61", "call", "alias", "phase"]], instances, symbols), 0), "open")
  await answer([["62", "make", "boxed", ["slim"]]], instances, symbols)
  assert.equal(cell(await answer([["63", "call", "boxed", "phase"]], instances, symbols), 0), "new")
  await answer([["64", "make", "widget", "widget"]], instances, symbols)
  assert.match(cell(await answer([["65", "call", "widget", "noSuch"]], instances, symbols), 0), /in class widget/)
  await answer([["66", "make", "blank", ""]], instances, symbols)
  assert.match(cell(await answer([["67", "call", "blank", "noSuch"]], instances, symbols), 0), /in class slim/)
  symbols.set("", "EMPTY")
  assert.equal(expandString("$", symbols), "$")
  assert.equal(expandString("$wc", symbols), "$wc")
  symbols.set("b", "B")
  assert.equal(expandString("ab", symbols), "ab")
  symbols.set("a", instances.get("decoy"))
  assert.equal(cell(await answer([["68", "call", "again", "echo", "$a"]], instances, symbols), 0), "OK")
  symbols.set("u", undefined)
  assert.equal(expandString("$u", symbols), "null")
  symbols.set("z", null)
  assert.equal(expandString("$z", symbols), "null")
  assert.equal(expandString("$ab", symbols), "YES")
  const lanes = await answer([["69", "lanes", ["t1", [["a", "call", "again", "hello"], ["b", "call", "again", "hello"]]], "short"]], instances, symbols)
  const laneRows = lanes[0]?.[1]
  assert.ok(Array.isArray(laneRows) && Array.isArray(laneRows[0]) && Array.isArray(laneRows[1]))
  assert.equal(laneRows[0][0], "t1")
  assert.equal(laneRows[0][1], "a")
  assert.equal(laneRows[0][2], "1")
  assert.equal(laneRows[0][3], "2")
  assert.equal(laneRows[1][2], "3")
})

test("a gate, a word, and a booking", async () => {
  const instances = new Map()
  const symbols = new Map<string, string>()
  const got = await answer([
    ["1", "make", "gate", "slim"],
    call("2", "gate", "open"),
    call("3", "gate", "block"),
    call("4", "gate", "release"),
    call("5", "gate", "phase"),
    call("6", "gate", "released"),
    ["7", "make", "other", "slim"],
    call("8", "other", "phase"),
    call("9", "gate", "setWord", ["red"]),
    call("10", "gate", "echo"),
    call("11", "gate", "hello"),
  ], instances, symbols)
  for (const [name, want] of [["1", "OK"], ["2", "ok"], ["3", "ok"], ["4", "refused"], ["5", "blocked"], ["6", "false"], ["8", "new"], ["9", "OK"], ["10", "red"], ["11", "world"]] as const) {
    assert.equal(id(got, name), want, name)
  }
  const opened = new Map()
  await answer([["1", "make", "gate", "slim"], call("2", "gate", "open"), call("3", "gate", "release")], opened, symbols)
  assert.equal(cell(await answer([call("4", "gate", "phase")], opened, symbols), 0), "released")
  assert.equal(cell(await answer([call("5", "gate", "released")], opened, symbols), 0), "true")
  assert.equal(cell(await answer([call("12", "missing", "open")], instances, symbols), 0), "refused")
  assert.equal(cell(await answer([call("12b", "missing", "released")], instances, symbols), 0), "false")
  assert.equal(cell(await answer([call("13", "missing", "phase")], instances, symbols), 0), "")
  assert.equal(cell(await answer([call("14", "missing", "echo")], instances, symbols), 0), "OK")
  assert.equal(cell(await answer([["15", "make"]], instances, symbols), 0), "OK")
  const unknown = cell(await answer([call("16", "gate", "noSuch")], instances, symbols), 0)
  assert.match(unknown, /No Method noSuch\[0\] in class slim/)
  assert.match(unknown, /Available methods:/)
  assert.match(unknown, /hello\(\) -> string/)
  assert.match(cell(await answer([call("16b", "gate", "noSuch", ["x"])], instances, symbols), 0), /No Method noSuch\[1\] in class slim/)
  assert.match(cell(await answer([call("16c", "absent", "noSuch")], instances, symbols), 0), /NO_INSTANCE absent/)
  assert.equal(cell(await answer([call("16d", "gate", "beginTable")], instances, symbols), 0), "OK")
  assert.equal(cell(await answer([call("16e", "gate", "execute")], instances, symbols), 0), "OK")
  const seat = new Map()
  await answer([["1", "make", "seat", "slim", "safe"]], seat, symbols)
  assert.equal(cell(await answer([call("2", "seat", "book")], seat, symbols), 0), "booked")
  assert.equal(cell(await answer([call("3", "seat", "book")], seat, symbols), 0), "full")
  assert.equal(cell(await answer([["4", "lanes"]], seat, symbols), 0), "lanes")
  const skipped = await answer([["6", "lanes", "not-a-group"]], seat, symbols)
  assert.deepEqual(skipped[0]?.[1], [])
  assert.match(cell(await answer([call("5", "other", "book")], seat, symbols), 0), /NO_INSTANCE other/)
  const raced = new Map()
  await answer([["1", "make", "seat", "slim", "racy"]], raced, symbols)
  assert.equal(cell(await answer([call("2", "seat", "book")], raced, symbols), 0), "booked")
  assert.equal(cell(await answer([call("3", "seat", "book")], raced, symbols), 0), "full")
})

test("two racy books can both succeed and two safe books cannot", async () => {
  const racy = await both("racy")
  assert.deepEqual(racy.results.sort(), ["booked", "booked"])
  assert.equal(racy.overlap, true)
  const safe = await both("safe")
  assert.deepEqual(safe.results.sort(), ["booked", "full"])
})

async function both(mark: string): Promise<{ results: string[], overlap: boolean }> {
  const instances = new Map()
  const symbols = new Map<string, string>()
  await answer([["1", "make", "seat", "slim", mark]], instances, symbols)
  const got = await answer([["9", "lanes",
    ["t1", [["a", "call", "seat", "book"]]],
    ["t2", [["b", "call", "seat", "book"]]],
  ]], instances, symbols)
  const rows = got[0]?.[1]
  if (!Array.isArray(rows) || !Array.isArray(rows[0]) || !Array.isArray(rows[1])) {
    return { results: [], overlap: false }
  }
  return {
    results: [String(rows[0][4]), String(rows[1][4])],
    overlap: Number(rows[0][2]) < Number(rows[1][3]) && Number(rows[1][2]) < Number(rows[0][3]),
  }
}

test("lists, lengths, and a socket", async () => {
  assert.deepEqual(parse("nope"), [])
  assert.deepEqual(parse("[000001:000001:a:000001:b:]"), ["a"])
  assert.deepEqual(parse("[000002:000001:a:000001:b:]"), ["a", "b"])
  const bad = new Writable({ write(_c, _e, done) { done() } })
  const badChunks: Buffer[] = []
  await speak(Readable.from([Buffer.from("abc:")]), new Writable({ write(chunk, _e, done) { badChunks.push(Buffer.from(chunk)); done() } }), new Map())
  assert.equal(Buffer.concat(badChunks).toString("utf8"), "Slim -- V0.5\n")
  assert.deepEqual(parse("[000001:000001:z"), ["z"])
  assert.deepEqual(parse("[000001:000003:z"), [])
  assert.deepEqual(parse(serialize([["z"]])), [["z"]])
  assert.equal(serialize("world"), "world")
  assert.deepEqual(readLength("abc:", 0), { number: 0, next: 4 })
  assert.deepEqual(readLength(":12", 0), { number: 0, next: 1 })
  assert.deepEqual(readLength("xx000001:", 2), { number: 1, next: 9 })
  assert.deepEqual(readLength("", 0), { number: 0, next: 0 })
  const port = await freePort()
  let settled = false
  const done = run(["slim", String(port)]).then((code) => { settled = true; return code })
  await delay(300)
  assert.equal(settled, false)
  const first = await dial(port)
  const header = await readUntil(first, "\n")
  assert.equal(header, "Slim -- V0.5\n")
  await delay(80)
  assert.equal(settled, false)
  first.write(frame(["1", "make", "seat", "slim", "safe"]))
  assert.match(await readUntil(first, "OK"), /OK/)
  first.write(frame(["2", "call", "seat", "book"]))
  assert.match(await readUntil(first, "booked"), /booked/)
  first.write(frame(["3", "call", "seat", "book"]))
  assert.match(await readUntil(first, "full"), /full/)
  first.write("000003:bye")
  first.end()
  assert.equal(await done, 0)
})

function frame(value: Cell): string {
  const body = serialize([value])
  return `${String(body.length).padStart(6, "0")}:${body}`
}

function freePort(): Promise<number> {
  return new Promise((resolve) => {
    const server = net.createServer()
    server.listen(0, "127.0.0.1", () => {
      const address = server.address()
      const port = typeof address === "object" && address !== null ? address.port : 0
      server.close(() => resolve(port))
    })
  })
}

function dial(port: number): Promise<net.Socket> {
  return new Promise((resolve, reject) => {
    const socket = net.connect(port, "127.0.0.1")
    socket.once("connect", () => resolve(socket))
    socket.once("error", reject)
  })
}

function readUntil(socket: net.Socket, marker: string): Promise<string> {
  return new Promise((resolve, reject) => {
    let buf = ""
    const timer = setTimeout(() => reject(new Error(buf)), 2000)
    socket.on("data", (chunk) => {
      buf += chunk.toString("utf8")
      if (buf.includes(marker)) {
        clearTimeout(timer)
        resolve(buf)
      }
    })
  })
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

void Readable
void t
