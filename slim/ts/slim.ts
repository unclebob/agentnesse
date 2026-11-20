// A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
// socket on 127.0.0.1.
//
// TypeScript has no real threads. A lanes instruction runs every lane as an
// async task on Node's one event loop. The tasks interleave at await. A racy
// book awaits a timer, so two lanes can both see the seat free. A safe book
// waits its turn on a promise chain. There are no worker threads.
import net from "node:net"
import process from "node:process"

export type Cell = string | Cell[]

interface Seat {
  phase: string
  seats: number | undefined
  race: string
  word: string
  tail: Promise<void>
}

export function status(failed: boolean): number {
  return failed ? 1 : 0
}

export function rejected(port: number): boolean {
  return port < 0
}

export function commandPort(args: string[]): number | "pipe" | "bad" {
  const port = args[args.length - 1]
  if (port === undefined) return "bad"
  if (port === "1") return "pipe"
  const number = Number(port)
  if (!Number.isInteger(number) || rejected(number)) return "bad"
  return number
}

export async function run(args: string[], input: NodeJS.ReadableStream = process.stdin, output: NodeJS.WritableStream = process.stdout): Promise<number> {
  const port = commandPort(args)
  if (port === "bad") return status(true)
  if (port === "pipe") {
    await speak(input, output, new Map())
    return 0
  }
  await serve(port)
  return 0
}

export function serve(port: number): Promise<void> {
  const instances = new Map<string, Seat>()
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.once("connection", (socket) => {
      server.close()
      speak(socket, socket, instances).finally(() => {
        socket.end()
        resolve()
      })
    })
    server.on("error", (err) => reject(err))
    server.listen(port, "127.0.0.1")
  })
}

class Bytes {
  buf = Buffer.alloc(0)
  async take(input: AsyncGenerator<Buffer>, count: number): Promise<Buffer | undefined> {
    while (this.buf.length < count) {
      const next = await input.next()
      if (next.done) return undefined
      this.buf = Buffer.concat([this.buf, next.value])
    }
    const out = this.buf.subarray(0, count)
    this.buf = this.buf.subarray(count)
    return out
  }
}

export async function speak(input: NodeJS.ReadableStream, output: NodeJS.WritableStream, instances: Map<string, Seat>): Promise<void> {
  output.write("Slim -- V0.5\n")
  const symbols = new Map<string, string>()
  const chunks = readChunks(input)
  const bytes = new Bytes()
  for (;;) {
    const message = await readMessage(chunks, bytes)
    if (message === undefined || message.toLowerCase() === "bye") return
    const body = serialize(await answer(parse(message), instances, symbols))
    output.write(`${pad(body.length)}:${body}`)
  }
}

async function* readChunks(input: NodeJS.ReadableStream): AsyncGenerator<Buffer> {
  for await (const chunk of input) yield Buffer.from(chunk)
}

async function readMessage(chunks: AsyncGenerator<Buffer>, bytes: Bytes): Promise<string | undefined> {
  const prefix = await readPrefix(chunks, bytes)
  if (prefix === undefined) return undefined
  const length = Number(prefix)
  if (!Number.isInteger(length) || length < 0) return undefined
  const body = await bytes.take(chunks, length)
  return body === undefined ? undefined : body.toString("utf8")
}

async function readPrefix(chunks: AsyncGenerator<Buffer>, bytes: Bytes): Promise<string | undefined> {
  let prefix = ""
  while (prefix.length <= 10) {
    const byte = await bytes.take(chunks, 1)
    if (byte === undefined) return undefined
    if (byte[0] === 0x3a) return prefix
    prefix += byte.toString("utf8")
  }
  return undefined
}

export async function answer(instructions: Cell[], instances: Map<string, Seat>, symbols: Map<string, string>): Promise<Cell[]> {
  const results: Cell[] = []
  for (const item of instructions) {
    if (!Array.isArray(item)) continue
    results.push([textAt(item, 0), await effect(item, instances, symbols)])
  }
  return results
}

async function effect(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, string>): Promise<Cell> {
  if (textAt(row, 1) === "lanes") return runLanes(row, instances, symbols)
  return reply(row, instances, symbols)
}

async function runLanes(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, string>): Promise<Cell> {
  if (row.length <= 2) return "lanes"
  let counter = 0
  const stamp = (): string => {
    counter += 1
    return String(counter)
  }
  const rows: Cell[] = []
  await Promise.all(row.slice(2).map(async (item) => {
    if (!Array.isArray(item) || item.length < 2) return
    const calls = item[1]
    if (!Array.isArray(calls)) return
    const tag = textAt(item, 0)
    for (const call of calls) {
      if (!Array.isArray(call)) continue
      const started = stamp()
      const value = await reply(call, instances, symbols)
      const finished = stamp()
      rows.push([tag, textAt(call, 0), started, finished, value])
    }
  }))
  return rows
}

async function reply(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, string>): Promise<string> {
  switch (textAt(row, 1)) {
    case "make":
      return made(row, instances)
    case "call":
      return call(row, 2, 3, instances, symbols)
    case "callAndAssign": {
      const value = await call(row, 3, 4, instances, symbols)
      remember(symbols, textAt(row, 2), value)
      return value
    }
    default:
      return "OK"
  }
}

function remember(symbols: Map<string, string>, name: string, value: string): void {
  if (name === "") return
  symbols.set(name, value)
}

async function call(row: Cell[], instanceAt: number, methodAt: number, instances: Map<string, Seat>, symbols: Map<string, string>): Promise<string> {
  const method = textAt(row, methodAt)
  const args = expandAll(argsFrom(row, methodAt + 1), symbols)
  const name = textAt(row, instanceAt)
  const seat = name === "" ? undefined : instances.get(name)
  if (method === "echo") return echoOf(seat, args)
  const plainValue = plain(method, args)
  if (plainValue !== undefined) return plainValue
  return onSeat(method, name, firstText(args), seat)
}

function plain(method: string, args: Cell[]): string | undefined {
  switch (method) {
    case "hello": return "world"
    case "endIsAfterStart": return ordered(args)
    case "kind": return kindOf(args)
    default: return undefined
  }
}

function echoOf(seat: Seat | undefined, args: Cell[]): string {
  if (args.length === 0) return wordOf(seat)
  return typeof args[0] === "string" ? args[0] : "OK"
}

function ordered(args: Cell[]): string {
  if (args.length < 2) return "no"
  const [leftArg, rightArg] = args
  const left = whole(leftArg)
  const right = whole(rightArg)
  if (left !== undefined && right !== undefined && right > left) return "yes"
  return "no"
}

export function whole(value: Cell | undefined): number | undefined {
  if (typeof value !== "string" || !/^-?\d+$/.test(value)) return undefined
  return Number(value)
}

function kindOf(args: Cell[]): string {
  return firstText(args).includes('"kind":"id"') ? "id" : "no"
}

function firstText(args: Cell[]): string {
  const first = args[0]
  if (typeof first !== "string") return ""
  return first
}

async function onSeat(method: string, name: string, arg: string, seat: Seat | undefined): Promise<string> {
  const moved = transition(method, seat)
  if (moved !== undefined) return moved
  return asked(method, name, arg, seat)
}

function transition(method: string, seat: Seat | undefined): string | undefined {
  switch (method) {
    case "open": return move(seat, "new", "open")
    case "block": return move(seat, "open", "blocked")
    case "release": return move(seat, "open", "released")
    default: return undefined
  }
}

async function asked(method: string, name: string, arg: string, seat: Seat | undefined): Promise<string> {
  switch (method) {
    case "book": return book(seat, name)
    case "__agnesse_lane_probe__": return probe(seat, name)
    case "phase": return phaseOf(seat)
    case "released": return releasedOf(seat)
    case "setWord": return storeWord(seat, arg)
    default: return "OK"
  }
}

function phaseOf(seat: Seat | undefined): string {
  return seat === undefined ? "" : seat.phase
}

function releasedOf(seat: Seat | undefined): string {
  return seat !== undefined && seat.phase === "released" ? "true" : "false"
}

async function book(seat: Seat | undefined, name: string): Promise<string> {
  if (seat === undefined || seat.race === "") return missingInstance(name)
  if (seat.race === "racy") return raceBook(seat)
  return safeBook(seat)
}

function probe(seat: Seat | undefined, name: string): string {
  return seat === undefined ? missingInstance(name) : missingMethod("__agnesse_lane_probe__")
}

function missingInstance(name: string): string {
  return `__EXCEPTION__:message:<<NO_INSTANCE ${name}>>`
}

function missingMethod(method: string): string {
  return `__EXCEPTION__:message:<<NO_METHOD_IN_CLASS ${method}>>`
}

function move(seat: Seat | undefined, needed: string, next: string): string {
  if (seat === undefined || seat.phase !== needed) return "refused"
  seat.phase = next
  return "ok"
}

function storeWord(seat: Seat | undefined, word: string): string {
  if (seat === undefined) return "OK"
  seat.word = word
  return "OK"
}

function wordOf(seat: Seat | undefined): string {
  if (seat === undefined || seat.word === "") return "OK"
  return seat.word
}

function made(row: Cell[], instances: Map<string, Seat>): string {
  const name = textAt(row, 2)
  if (name === "") return "OK"
  instances.set(name, opened(textAt(row, 4)))
  return "OK"
}

function opened(mark: string): Seat {
  const seat = { phase: "new", seats: undefined as number | undefined, race: "", word: "", tail: Promise.resolve() }
  if (mark === "racy" || mark === "safe") {
    seat.phase = ""
    seat.seats = 1
    seat.race = mark
  }
  return seat
}

async function raceBook(seat: Seat): Promise<string> {
  const seen = seat.seats
  if (seen === undefined || seen <= 0) return "full"
  await delay(50)
  seat.seats = seen - 1
  return "booked"
}

async function safeBook(seat: Seat): Promise<string> {
  const previous = seat.tail
  let release = (): void => {}
  seat.tail = new Promise((done) => { release = done })
  await previous
  const seen = seat.seats
  if (seen === undefined || seen <= 0) {
    release()
    return "full"
  }
  const result = take(seat, seen)
  release()
  return result
}

function take(seat: Seat, seen: number): string {
  seat.seats = seen - 1
  return "booked"
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function textAt(row: Cell[], index: number): string {
  const value = row[index]
  return typeof value === "string" ? value : ""
}

function argsFrom(row: Cell[], index: number): Cell[] {
  return row.slice(index)
}

function expandAll(args: Cell[], symbols: Map<string, string>): Cell[] {
  return args.map((arg) => expand(arg, symbols))
}

function expand(value: Cell, symbols: Map<string, string>): Cell {
  if (typeof value === "string") return expandString(value, symbols)
  return value.map((item) => expand(item, symbols))
}

export function expandString(text: string, symbols: Map<string, string>): string {
  let out = ""
  for (let i = 0; i < text.length; i++) {
    const found = symbolAt(text, i)
    if (found === undefined) {
      out += text[i]
      continue
    }
    const known = symbols.get(found.name)
    out += known === undefined ? text.slice(i, found.next) : known
    i = found.next - 1
  }
  return out
}

function symbolAt(text: string, i: number): { name: string, next: number } | undefined {
  if (text[i] !== "$") return undefined
  const match = /^[A-Za-z][A-Za-z0-9_]*/.exec(text.slice(i + 1))
  if (match === null) return undefined
  return { name: match[0], next: i + 1 + match[0].length }
}

export function parse(text: string): Cell[] {
  return readList(text)
}

function readList(text: string): Cell[] {
  if (!text.startsWith("[")) return []
  const count = readLength(text, 1)
  let index = count.next
  const items: Cell[] = []
  for (let n = 0; n < count.number; n++) {
    const field = readField(text, index)
    if (field === undefined) break
    index = field.next
    items.push(field.raw.startsWith("[") ? readList(field.raw) : field.raw)
  }
  return items
}

function readField(text: string, index: number): { raw: string, next: number } | undefined {
  const length = readLength(text, index)
  if (length.next + length.number > text.length) return undefined
  return { raw: text.slice(length.next, length.next + length.number), next: length.next + length.number + 1 }
}

export function readLength(text: string, index: number): { number: number, next: number } {
  const colon = text.indexOf(":", index)
  if (colon < 0) return { number: 0, next: text.length }
  const raw = text.slice(index, colon)
  if (!/^\d+$/.test(raw)) return { number: 0, next: colon + 1 }
  return { number: Number(raw), next: colon + 1 }
}

export function serialize(value: Cell): string {
  if (!Array.isArray(value)) return String(value)
  let body = `[${pad(value.length)}:`
  for (const part of value) {
    const text = serialize(part)
    body += `${pad(text.length)}:${text}:`
  }
  return `${body}]`
}

function pad(length: number): string {
  return String(length).padStart(6, "0")
}

if (process.argv[1]?.endsWith("slim.ts")) {
  run(process.argv.slice(2)).then((code) => process.exit(code))
}
