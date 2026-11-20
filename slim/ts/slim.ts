// A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
// socket on 127.0.0.1.
//
// TypeScript has no real threads. A lanes instruction runs every lane as an
// async task on Node's one event loop. The tasks interleave at await. A racy
// book awaits a timer, so two lanes can both see the seat free. A safe book
// waits its turn on a promise chain. There are no worker threads.
//
// Symbols follow the Java forms: $Name with Unicode letters, $`backtick names`,
// shorter-prefix fallback, undefined text left as written, a whole-cell symbol
// passing the stored object, $name= arriving as an empty argument, symbols in a
// make class name, nested lists, and SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS.
// There are no runtime parameter types, so plus, wide, scale, flag, initial,
// day, shade, join, and label convert arguments through the signature table
// above those names. wide uses bigint. day checks an ISO date string.
import net from "node:net"
import process from "node:process"

export type Cell = string | Cell[]

interface Seat {
  phase: string
  seats: number | undefined
  race: string
  word: string
  className: string
  helper?: { stack: unknown[] }
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
  const symbols = new Map<string, unknown>()
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

export async function answer(instructions: Cell[], instances: Map<string, Seat>, symbols: Map<string, unknown>): Promise<unknown[]> {
  ensureHelper(instances)
  const results: unknown[] = []
  for (const item of instructions) {
    if (!Array.isArray(item)) continue
    results.push([textAt(item, 0), await effect(item, instances, symbols)])
  }
  return results
}

async function effect(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, unknown>): Promise<Cell> {
  if (textAt(row, 1) === "lanes") return runLanes(row, instances, symbols)
  return reply(row, instances, symbols)
}

async function runLanes(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, unknown>): Promise<Cell> {
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

async function reply(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, unknown>): Promise<unknown> {
  switch (textAt(row, 1)) {
    case "make":
      return made(row, instances, symbols)
    case "call":
      return call(row, 2, 3, instances, symbols)
    case "callAndAssign": {
      const value = await call(row, 3, 4, instances, symbols)
      remember(symbols, textAt(row, 2), value)
      noteOptOut(symbols)
      return value
    }
    case "assign":
      remember(symbols, textAt(row, 2), row[3])
      noteOptOut(symbols)
      return "OK"
    default:
      return "OK"
  }
}

function remember(symbols: Map<string, unknown>, name: string, value: unknown): void {
  if (name === "") return
  symbols.set(name, value)
}

async function call(row: Cell[], instanceAt: number, methodAt: number, instances: Map<string, Seat>, symbols: Map<string, unknown>): Promise<unknown> {
  const method = textAt(row, methodAt)
  const name = textAt(row, instanceAt)
  const raw = argsFrom(row, methodAt + 1)
  const args = skipsSymbols(symbols, name, method) ? raw : expandAll(raw, symbols)
  const converted = convertArgs(method, args)
  const seat = name === "" ? undefined : instances.get(name)
  if (seat?.helper !== undefined) return helperCall(seat, method, converted, instances)
  if (method === "echo") return echoOf(seat, converted)
  const plainValue = plain(method, converted)
  if (plainValue !== undefined) return plainValue
  return onSeat(method, name, firstText(converted), converted.length, seat)
}

function plain(method: string, args: unknown[]): string | undefined {
  const number = numbers(method, args)
  if (number !== undefined) return number
  return words(method, args)
}

function numbers(method: string, args: unknown[]): string | undefined {
  switch (method) {
    case "plus": return plus(args)
    case "wide": return wide(args)
    case "scale": return scale(args)
    case "flag": return flag(args)
    default: return undefined
  }
}

function words(method: string, args: unknown[]): string | undefined {
  const greeting = greetings(method, args)
  if (greeting !== undefined) return greeting
  return shaped(method, args)
}

function greetings(method: string, args: unknown[]): string | undefined {
  switch (method) {
    case "hello": return "world"
    case "endIsAfterStart": return ordered(args)
    case "kind": return kindOf(args)
    case "initial": return initial(args)
    default: return undefined
  }
}

function shaped(method: string, args: unknown[]): string | undefined {
  switch (method) {
    case "day": return day(args)
    case "shade": return shade(args)
    case "join": return join(args)
    case "label": return label(args)
    default: return undefined
  }
}

const signatures: Record<string, string[]> = {
  plus: ["int", "int"],
  wide: ["long"],
  scale: ["double"],
  flag: ["boolean"],
  initial: ["char"],
  day: ["date"],
  shade: ["enum"],
  join: ["list"],
  label: ["map"],
}

function skipsSymbols(symbols: Map<string, unknown>, instance: string, method: string): boolean {
  const pattern = symbols.get("\u0000opt")
  if (!(pattern instanceof RegExp)) return false
  return pattern.test(`${instance}.${method}`)
}

function noteOptOut(symbols: Map<string, unknown>): void {
  if (!symbols.has("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS")) {
    symbols.delete("\u0000opt")
    return
  }
  const value = symbols.get("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS")
  if (value === undefined || value === null) {
    symbols.delete("\u0000opt")
    return
  }
  try {
    symbols.set("\u0000opt", new RegExp(`^(?:${String(value)})$`))
  } catch {
    symbols.delete("\u0000opt")
  }
}

function convertArgs(method: string, args: unknown[]): unknown[] {
  const kinds = signatures[method]
  if (kinds === undefined) return args
  return args.map((arg, index) => convert(arg, kinds[index] ?? "string"))
}

function convert(value: unknown, kind: string): unknown {
  if (typeof value !== "string") return value
  if (kind === "int") return parsedInt(value)
  if (kind === "long") return parsedLong(value)
  if (kind === "double") return parsedDouble(value)
  if (kind === "boolean") return value === "true"
  if (kind === "char") return parsedChar(value)
  if (kind === "date") return parsedDate(value)
  if (kind === "list") return parsedList(value)
  return value
}

function parsedInt(text: string): unknown {
  return /^-?\d+$/.test(text) ? Number(text) : text
}

function parsedLong(text: string): unknown {
  return /^-?\d+$/.test(text) ? BigInt(text) : text
}

function parsedDouble(text: string): unknown {
  const number = Number(text)
  return Number.isFinite(number) && text.trim() !== "" ? number : text
}

function parsedChar(text: string): unknown {
  return [...text].length === 1 ? text : text
}

function parsedDate(text: string): unknown {
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : text
}

function parsedList(text: string): unknown {
  if (!text.startsWith("[") || !text.endsWith("]")) return text
  const body = text.slice(1, -1).trim()
  if (body === "") return []
  return body.split(",").map((part) => part.trim())
}

function plus(args: unknown[]): string {
  if (typeof args[0] !== "number" || typeof args[1] !== "number") return "no"
  return String(args[0] + args[1])
}

function wide(args: unknown[]): string {
  return typeof args[0] === "bigint" ? args[0].toString() : "no"
}

function scale(args: unknown[]): string {
  return typeof args[0] === "number" ? String(args[0]) : "no"
}

function flag(args: unknown[]): string {
  return args[0] === true ? "yes" : "no"
}

function initial(args: unknown[]): string {
  const text = typeof args[0] === "string" ? args[0] : ""
  return [...text].length === 1 ? text : "no"
}

function day(args: unknown[]): string {
  const text = typeof args[0] === "string" ? args[0] : ""
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : "no"
}

function shade(args: unknown[]): string {
  const text = typeof args[0] === "string" ? args[0] : ""
  return text === "red" || text === "green" || text === "blue" ? text : "no"
}

function join(args: unknown[]): string {
  return Array.isArray(args[0]) ? args[0].map((item) => asText(item)).join(",") : "no"
}

function label(args: unknown[]): string {
  if (!Array.isArray(args[0])) return "no"
  for (const row of args[0]) {
    if (Array.isArray(row) && asText(row[0]) === "name") return asText(row[1])
  }
  return "no"
}

function helperCall(seat: Seat, method: string, args: unknown[], instances: Map<string, Seat>): unknown {
  if (method === "pushFixture") {
    seat.helper?.stack.push(instances.get("scriptTableActor"))
    return "OK"
  }
  if (method === "popFixture") {
    const last = seat.helper?.stack.pop()
    if (last instanceof Object && "phase" in (last as Seat)) instances.set("scriptTableActor", last as Seat)
    return "OK"
  }
  if (method === "cloneSymbol") return args[0]
  return noMethod(method, args.length, "SlimHelperLibrary")
}

function echoOf(seat: Seat | undefined, args: unknown[]): string {
  if (args.length === 0) return wordOf(seat)
  return typeof args[0] === "string" ? args[0] : "OK"
}

function ordered(args: unknown[]): string {
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

function kindOf(args: unknown[]): string {
  return firstText(args).includes('"kind":"id"') ? "id" : "no"
}

function firstText(args: unknown[]): string {
  const first = args[0]
  if (typeof first !== "string") return ""
  return first
}

async function onSeat(method: string, name: string, arg: string, count: number, seat: Seat | undefined): Promise<string> {
  const moved = transition(method, seat)
  if (moved !== undefined) return moved
  return asked(method, name, arg, count, seat)
}

function transition(method: string, seat: Seat | undefined): string | undefined {
  switch (method) {
    case "open": return move(seat, "new", "open")
    case "block": return move(seat, "open", "blocked")
    case "release": return move(seat, "open", "released")
    default: return undefined
  }
}

async function asked(method: string, name: string, arg: string, count: number, seat: Seat | undefined): Promise<string> {
  const seatValue = seatValueOf(method, name, arg, seat)
  if (seatValue !== undefined) return seatValue
  if (method === "table" || method === "beginTable" || method === "reset" || method === "execute" || method === "endTable") return "OK"
  return unknown(method, name, count, seat)
}

function seatValueOf(method: string, name: string, arg: string, seat: Seat | undefined): string | undefined {
  switch (method) {
    case "book": return book(seat, name)
    case "phase": return phaseOf(seat)
    case "released": return releasedOf(seat)
    case "setWord": return storeWord(seat, arg)
    default: return undefined
  }
}

function unknown(method: string, name: string, count: number, seat: Seat | undefined): string {
  if (seat === undefined) return missingInstance(name)
  const className = seat.className === "" ? "slim" : seat.className
  return noMethod(method, count, className)
}

function noMethod(method: string, count: number, className: string): string {
  return `__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method ${method}[${count}] in class ${className}.\n Available methods:\n${CATALOG}>>`
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

function missingInstance(name: string): string {
  return `__EXCEPTION__:message:<<NO_INSTANCE ${name}>>`
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

function made(row: Cell[], instances: Map<string, Seat>, symbols: Map<string, unknown>): string {
  const name = textAt(row, 2)
  if (name === "") return "OK"
  const actor = actorFrom(row[3], symbols)
  if (actor !== undefined) {
    instances.set(name, actor)
    return "OK"
  }
  const className = asText(expandCell(textAt(row, 3), symbols))
  instances.set(name, opened(className, textAt(row, 4)))
  return "OK"
}

function actorFrom(raw: Cell | undefined, symbols: Map<string, unknown>): Seat | undefined {
  if (typeof raw !== "string" || !raw.startsWith("$")) return undefined
  const value = symbols.get(raw.slice(1))
  if (value === undefined || typeof value === "string") return undefined
  if (typeof value === "object" && value !== null && "phase" in value) return value as Seat
  return undefined
}

function ensureHelper(instances: Map<string, Seat>): void {
  if (instances.has("SlimHelperLibrary")) return
  const seat = opened("SlimHelperLibrary", "")
  seat.helper = { stack: [] }
  instances.set("SlimHelperLibrary", seat)
}

function opened(className: string, mark: string): Seat {
  const seat = { phase: "new", seats: undefined as number | undefined, race: "", word: "", className: className === "" ? "slim" : className, tail: Promise.resolve() }
  if (mark === "racy" || mark === "safe") {
    seat.phase = ""
    seat.seats = 1
    seat.race = mark
  }
  return seat
}

const CATALOG = [
  "beginTable() -> string",
  "day(date) -> string",
  "block() -> string",
  "book() -> string",
  "echo(string) -> string",
  "endIsAfterStart(string, string) -> string",
  "endTable() -> string",
  "execute() -> string",
  "flag(boolean) -> string",
  "hello() -> string",
  "initial(char) -> string",
  "join(list) -> string",
  "kind(string) -> string",
  "label(map) -> string",
  "open() -> string",
  "phase() -> string",
  "plus(int, int) -> string",
  "release() -> string",
  "released() -> string",
  "reset() -> string",
  "scale(double) -> string",
  "setWord(string) -> string",
  "shade(enum) -> string",
  "table(list) -> string",
  "wide(long) -> string",
].join("\n")

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

const symbolPattern = /\$(([A-Za-z\p{L}][\w\p{L}]*)|`([^`]+)`)/u
const assignmentPattern = /^\s*\$([A-Za-z\p{L}][\w\p{L}]*)\s*=\s*$/u

function expandAll(args: Cell[], symbols: Map<string, unknown>): unknown[] {
  return args.map((arg) => expand(arg, symbols))
}

function expand(value: Cell, symbols: Map<string, unknown>): unknown {
  if (typeof value === "string") return expandCell(value, symbols)
  return value.map((item) => expand(item, symbols))
}

export function expandString(text: string, symbols: Map<string, unknown>): string {
  return asText(expandCell(text, symbols))
}

function expandCell(text: string, symbols: Map<string, unknown>): unknown {
  const stored = wholeStored(text, symbols)
  if (stored.found) return stored.value
  if (assignmentPattern.test(text)) return ""
  return substitute(text, symbols)
}

function wholeStored(text: string, symbols: Map<string, unknown>): { found: boolean, value: unknown } {
  if (!text.startsWith("$") || text.length < 2) return { found: false, value: undefined }
  if (!symbols.has(text.slice(1))) return { found: false, value: undefined }
  return { found: true, value: symbols.get(text.slice(1)) }
}

function substitute(text: string, symbols: Map<string, unknown>): string {
  let out = ""
  let offset = 0
  while (offset <= text.length) {
    const found = symbolAt(text, offset)
    if (found === undefined) return out + text.slice(offset)
    out += text.slice(offset, found.start)
    const resolved = resolveName(found.name, symbols)
    if (!resolved.found) {
      out += text.slice(found.start, found.next)
      offset = found.next
      continue
    }
    out += asText(resolved.value)
    offset = found.start + 1 + resolved.name.length
  }
  return out
}

function resolveName(name: string, symbols: Map<string, unknown>): { found: boolean, name: string, value: unknown } {
  if (symbols.has(name)) return { found: true, name, value: symbols.get(name) }
  for (let i = name.length - 1; i > 0; i--) {
    const shorter = name.slice(0, i)
    if (symbols.has(shorter)) return { found: true, name: shorter, value: symbols.get(shorter) }
  }
  return { found: false, name: "", value: undefined }
}

function asText(value: unknown): string {
  if (typeof value === "string") return value
  if (value === undefined || value === null) return "null"
  return String(value)
}

function symbolAt(text: string, index: number): { name: string, start: number, next: number } | undefined {
  const match = symbolPattern.exec(text.slice(index))
  if (match === null) return undefined
  const start = index + match.index
  return { name: match[1] ?? "", start, next: start + match[0].length }
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
