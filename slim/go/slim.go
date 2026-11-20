// A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
// socket on 127.0.0.1. A lanes instruction runs each lane on its own goroutine
// against that one connection's instances.
//
// Symbols follow the Java forms: $Name with Unicode letters, $`backtick names`,
// shorter-prefix fallback, undefined text left as written, a whole-cell symbol
// passing the stored value, $name= arriving as an empty argument, a non-string
// class symbol becoming the instance, SlimHelperLibrary push/pop/clone, nested
// lists, and SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS. plus, wide, scale, flag,
// initial, day, shade, join, and label convert cells by the signature table.
// A stored value embedded in text uses fmt.Sprint. A map is a list of pairs.
package main

import (
	"fmt"
	"io"
	"net"
	"os"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"
	"unicode/utf8"
)

func main() {
	os.Exit(status(run(os.Args)))
}

func status(err error) int {
	if err != nil {
		return 1
	}
	return 0
}

func run(args []string) error {
	if len(args) == 0 {
		return fmt.Errorf("port")
	}
	port := args[len(args)-1]
	if port == "1" {
		speak(os.Stdin, os.Stdout, nil, nil)
		return nil
	}
	number, err := strconv.Atoi(port)
	if rejected(number, err) {
		return fmt.Errorf("port")
	}
	return serve(number)
}

func rejected(number int, err error) bool {
	return err != nil || number < 0
}

func serve(port int) error {
	listener, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", port))
	if err != nil {
		return err
	}
	defer listener.Close()
	conn, err := listener.Accept()
	if err != nil {
		return err
	}
	session(conn, map[string]any{}, &sync.Mutex{}, func() {})
	return nil
}

func session(conn net.Conn, instances map[string]any, mu *sync.Mutex, done func()) {
	defer done()
	defer conn.Close()
	speak(conn, conn, instances, mu)
}

func speak(in io.Reader, out io.Writer, instances map[string]any, mu *sync.Mutex) {
	if instances == nil {
		instances = map[string]any{}
		mu = &sync.Mutex{}
	}
	fmt.Fprint(out, "Slim -- V0.5\n")
	symbols := map[string]any{}
	for {
		message, ok := readMessage(in)
		if !ok || strings.EqualFold(message, "bye") {
			return
		}
		body := serialize(answer(parse(message), instances, mu, symbols))
		fmt.Fprintf(out, "%06d:%s", len(body), body)
	}
}

func readMessage(in io.Reader) (string, bool) {
	prefix, ok := readPrefix(in)
	if !ok {
		return "", false
	}
	length, err := strconv.Atoi(prefix)
	if err != nil || length < 0 {
		return "", false
	}
	return readExact(in, length)
}

func readPrefix(in io.Reader) (string, bool) {
	prefix := make([]byte, 0, 8)
	var buf [1]byte
	for len(prefix) <= 10 {
		n, err := in.Read(buf[:])
		if n == 0 || err != nil {
			return "", false
		}
		if buf[0] == ':' {
			return string(prefix), true
		}
		prefix = append(prefix, buf[0])
	}
	return "", false
}

func readExact(in io.Reader, length int) (string, bool) {
	data := make([]byte, length)
	got := 0
	for got < length {
		n, err := in.Read(data[got:])
		if n == 0 || err != nil {
			return "", false
		}
		got += n
	}
	return string(data), true
}

func answer(instructions []any, instances map[string]any, mu *sync.Mutex, symbols map[string]any) []any {
	ensureHelper(instances, mu)
	results := make([]any, 0, len(instructions))
	for _, item := range instructions {
		row, ok := item.([]any)
		if !ok {
			continue
		}
		results = append(results, []any{textAt(row, 0), effect(row, instances, mu, symbols)})
	}
	return results
}

func effect(row []any, instances map[string]any, mu *sync.Mutex, symbols map[string]any) any {
	if textAt(row, 1) == "lanes" {
		return runLanes(row, instances, mu, symbols)
	}
	return reply(row, instances, mu, symbols)
}

func runLanes(row []any, instances map[string]any, mu *sync.Mutex, symbols map[string]any) any {
	if len(row) <= 2 {
		return "lanes"
	}
	var counter int64
	var counterMu sync.Mutex
	stamp := func() string {
		counterMu.Lock()
		counter++
		value := counter
		counterMu.Unlock()
		return strconv.FormatInt(value, 10)
	}
	rows := make([]any, 0)
	var rowsMu sync.Mutex
	var wg sync.WaitGroup
	for _, item := range row[2:] {
		group, ok := item.([]any)
		if !ok || len(group) < 2 {
			continue
		}
		calls, ok := group[1].([]any)
		if !ok {
			continue
		}
		tag := textAt(group, 0)
		wg.Add(1)
		go func(tag string, calls []any) {
			defer wg.Done()
			for _, item := range calls {
				instruction, ok := item.([]any)
				if !ok {
					continue
				}
				started := stamp()
				value := reply(instruction, instances, mu, symbols)
				finished := stamp()
				rowsMu.Lock()
				rows = append(rows, []any{tag, textAt(instruction, 0), started, finished, value})
				rowsMu.Unlock()
			}
		}(tag, calls)
	}
	wg.Wait()
	return rows
}

func reply(row []any, instances map[string]any, mu *sync.Mutex, symbols map[string]any) any {
	switch textAt(row, 1) {
	case "make":
		return made(row, instances, mu, symbols)
	case "call":
		return call(row, 2, 3, instances, mu, symbols)
	case "callAndAssign":
		value := call(row, 3, 4, instances, mu, symbols)
		remember(symbols, textAt(row, 2), value)
		noteOptOut(symbols)
		return value
	case "assign":
		remember(symbols, textAt(row, 2), cellAt(row, 3))
		noteOptOut(symbols)
		return "OK"
	default:
		return "OK"
	}
}

func remember(symbols map[string]any, name string, value any) {
	if name == "" {
		return
	}
	symbols[name] = value
}

func call(row []any, instanceAt, methodAt int, instances map[string]any, mu *sync.Mutex, symbols map[string]any) any {
	method := textAt(row, methodAt)
	name := textAt(row, instanceAt)
	raw := argsFrom(row, methodAt+1)
	args := raw
	if !skipsSymbols(symbols, name, method) {
		args = expandAll(raw, symbols)
	}
	args = convertArgs(method, args)
	if helper, ok := findAny(instances, mu, name).(*library); ok {
		return helper.invoke(method, args)
	}
	seat := find(instances, mu, name)
	if method == "echo" {
		return echoOf(seat, args)
	}
	if value, ok := plain(method, args); ok {
		return value
	}
	return onSeat(method, name, firstText(args), len(args), seat)
}

func plain(method string, args []any) (string, bool) {
	switch method {
	case "hello":
		return "world", true
	case "endIsAfterStart":
		return ordered(args), true
	case "kind":
		return kindOf(args), true
	case "plus":
		return plus(args), true
	case "wide":
		return wide(args), true
	case "scale":
		return scale(args), true
	case "flag":
		return flag(args), true
	case "initial":
		return initial(args), true
	case "day":
		return day(args), true
	case "shade":
		return shade(args), true
	case "join":
		return join(args), true
	case "label":
		return label(args), true
	default:
		return "", false
	}
}

func plus(args []any) string {
	if len(args) < 2 {
		return "no"
	}
	left, lok := args[0].(int)
	right, rok := args[1].(int)
	if !lok || !rok {
		return "no"
	}
	return strconv.Itoa(left + right)
}

func wide(args []any) string {
	number, ok := firstOf(args).(int64)
	if !ok {
		return "no"
	}
	return strconv.FormatInt(number, 10)
}

func scale(args []any) string {
	number, ok := firstOf(args).(float64)
	if !ok {
		return "no"
	}
	return strconv.FormatFloat(number, 'f', -1, 64)
}

func flag(args []any) string {
	value, ok := firstOf(args).(bool)
	if ok && value {
		return "yes"
	}
	return "no"
}

func initial(args []any) string {
	value, ok := firstOf(args).(rune)
	if !ok {
		return "no"
	}
	return string(value)
}

func day(args []any) string {
	value, ok := firstOf(args).(time.Time)
	if !ok {
		return "no"
	}
	return value.Format("2006-01-02")
}

func shade(args []any) string {
	text := asText(firstOf(args))
	switch text {
	case "red", "green", "blue":
		return text
	default:
		return "no"
	}
}

func join(args []any) string {
	items, ok := firstOf(args).([]any)
	if !ok {
		return "no"
	}
	parts := make([]string, len(items))
	for i, item := range items {
		parts[i] = asText(item)
	}
	return strings.Join(parts, ",")
}

func label(args []any) string {
	rows, ok := firstOf(args).([]any)
	if !ok {
		return "no"
	}
	for _, row := range rows {
		pair, ok := row.([]any)
		if ok && len(pair) > 1 && asText(pair[0]) == "name" {
			return asText(pair[1])
		}
	}
	return "no"
}

func firstOf(args []any) any {
	if len(args) == 0 {
		return nil
	}
	return args[0]
}

func echoOf(seat *seat, args []any) string {
	if len(args) == 0 {
		return wordOf(seat)
	}
	text, ok := args[0].(string)
	if !ok {
		return "OK"
	}
	return text
}

func ordered(args []any) string {
	if len(args) < 2 {
		return "no"
	}
	left, lok := whole(args[0])
	right, rok := whole(args[1])
	if lok && rok && right > left {
		return "yes"
	}
	return "no"
}

func whole(value any) (int, bool) {
	text, ok := value.(string)
	if !ok {
		return 0, false
	}
	number, err := strconv.Atoi(text)
	if err != nil {
		return 0, false
	}
	return number, true
}

func kindOf(args []any) string {
	text := firstText(args)
	if strings.Contains(text, `"kind":"id"`) {
		return "id"
	}
	return "no"
}

func firstText(args []any) string {
	if len(args) == 0 {
		return ""
	}
	text, ok := args[0].(string)
	if !ok {
		return ""
	}
	return text
}

func onSeat(method, name, arg string, count int, seat *seat) string {
	if moved, ok := transition(method, seat); ok {
		return moved
	}
	return asked(method, name, arg, count, seat)
}

func transition(method string, seat *seat) (string, bool) {
	switch method {
	case "open":
		return move(seat, "new", "open"), true
	case "block":
		return move(seat, "open", "blocked"), true
	case "release":
		return move(seat, "open", "released"), true
	default:
		return "", false
	}
}

func asked(method, name, arg string, count int, seat *seat) string {
	switch method {
	case "book":
		return book(seat, name)
	case "phase":
		return phaseOf(seat)
	case "released":
		return releasedOf(seat)
	case "setWord":
		return storeWord(seat, arg)
	case "table", "beginTable", "reset", "execute", "endTable":
		return "OK"
	default:
		return unknown(method, name, count, seat)
	}
}

func unknown(method, name string, count int, seat *seat) string {
	if seat == nil {
		return missingInstance(name)
	}
	className := seat.className
	if className == "" {
		className = "slim"
	}
	return noMethod(method, count, className)
}

func noMethod(method string, count int, className string) string {
	list := catalog
	if className == "SlimHelperLibrary" {
		list = helperCatalog
	}
	return "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method " + method + "[" + strconv.Itoa(count) + "] in class " + className + ".\n Available methods:\n" + list + ">>"
}

func book(seat *seat, name string) string {
	if seat == nil || seat.race == "" {
		return missingInstance(name)
	}
	if seat.race == "racy" {
		return raceBook(seat)
	}
	return safeBook(seat)
}

func missingInstance(name string) string {
	return "__EXCEPTION__:message:<<NO_INSTANCE " + name + ">>"
}

func move(seat *seat, needed, next string) string {
	if seat == nil || seat.phase != needed {
		return "refused"
	}
	seat.phase = next
	return "ok"
}

func phaseOf(seat *seat) string {
	if seat == nil {
		return ""
	}
	return seat.phase
}

func releasedOf(seat *seat) string {
	if seat != nil && seat.phase == "released" {
		return "true"
	}
	return "false"
}

func storeWord(seat *seat, word string) string {
	if seat == nil {
		return "OK"
	}
	seat.word = word
	return "OK"
}

func wordOf(seat *seat) string {
	if seat == nil || seat.word == "" {
		return "OK"
	}
	return seat.word
}

type seat struct {
	mu        sync.Mutex
	phase     string
	seats     int
	race      string
	word      string
	className string
}

func made(row []any, instances map[string]any, mu *sync.Mutex, symbols map[string]any) string {
	name := textAt(row, 2)
	if name == "" {
		return "OK"
	}
	if actor, ok := actorFrom(cellAt(row, 3), symbols); ok {
		mu.Lock()
		instances[name] = actor
		mu.Unlock()
		return "OK"
	}
	className := asText(expandCell(textAt(row, 3), symbols))
	if className == "" {
		className = "slim"
	}
	mu.Lock()
	instances[name] = opened(className, textAt(row, 4))
	mu.Unlock()
	return "OK"
}

func actorFrom(raw any, symbols map[string]any) (any, bool) {
	text, ok := raw.(string)
	if !ok || len(text) < 2 || text[0] != '$' {
		return nil, false
	}
	value, known := symbols[text[1:]]
	if !known {
		return nil, false
	}
	if _, isString := value.(string); isString {
		return nil, false
	}
	return value, true
}

func cellAt(row []any, index int) any {
	if index >= len(row) {
		return ""
	}
	return row[index]
}

func findAny(instances map[string]any, mu *sync.Mutex, name string) any {
	if name == "" {
		return nil
	}
	mu.Lock()
	defer mu.Unlock()
	return instances[name]
}

func opened(className, mark string) *seat {
	if mark == "racy" || mark == "safe" {
		return &seat{seats: 1, race: mark, className: className}
	}
	return &seat{phase: "new", className: className}
}

const catalog = "beginTable() -> string\nblock() -> string\nbook() -> string\nday(date) -> string\necho(string) -> string\nendIsAfterStart(string, string) -> string\nendTable() -> string\nexecute() -> string\nflag(boolean) -> string\nhello() -> string\ninitial(char) -> string\njoin(list) -> string\nkind(string) -> string\nlabel(map) -> string\nopen() -> string\nphase() -> string\nplus(int, int) -> string\nrelease() -> string\nreleased() -> string\nreset() -> string\nscale(double) -> string\nsetWord(string) -> string\nshade(enum) -> string\ntable(list) -> string\nwide(long) -> string"

const helperCatalog = "cloneSymbol(object) -> object\npopFixture() -> string\npushFixture() -> string"

const handlesSymbols = "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS"
const optKey = "\x00opt"

var symbolPattern = regexp.MustCompile(`\$(([A-Za-z\p{L}][\w\p{L}]*)|` + "`([^`]+)`" + `)`)
var assignmentPattern = regexp.MustCompile(`\A\s*\$([A-Za-z\p{L}][\w\p{L}]*)\s*=\s*\z`)

var signatures = map[string][]string{
	"plus":    {"int", "int"},
	"wide":    {"long"},
	"scale":   {"double"},
	"flag":    {"boolean"},
	"initial": {"char"},
	"day":     {"date"},
	"shade":   {"enum"},
	"join":    {"list"},
	"label":   {"map"},
	"setWord": {"string"},
	"echo":    {"string"},
}

func raceBook(seat *seat) string {
	seat.mu.Lock()
	seen := seat.seats
	seat.mu.Unlock()
	if seen <= 0 {
		return "full"
	}
	time.Sleep(50 * time.Millisecond)
	seat.mu.Lock()
	seat.seats = seen - 1
	seat.mu.Unlock()
	return "booked"
}

func safeBook(seat *seat) string {
	seat.mu.Lock()
	defer seat.mu.Unlock()
	if seat.seats <= 0 {
		return "full"
	}
	seat.seats--
	return "booked"
}

type library struct {
	instances map[string]any
	mu        *sync.Mutex
	stack     []any
}

func (lib *library) invoke(method string, args []any) any {
	switch method {
	case "pushFixture":
		lib.stack = append(lib.stack, findAny(lib.instances, lib.mu, "scriptTableActor"))
		return "OK"
	case "popFixture":
		if len(lib.stack) == 0 {
			return missingInstance("scriptTableActor")
		}
		last := lib.stack[len(lib.stack)-1]
		lib.stack = lib.stack[:len(lib.stack)-1]
		lib.mu.Lock()
		lib.instances["scriptTableActor"] = last
		lib.mu.Unlock()
		return "OK"
	case "cloneSymbol":
		return firstOf(args)
	default:
		return noMethod(method, len(args), "SlimHelperLibrary")
	}
}

func ensureHelper(instances map[string]any, mu *sync.Mutex) {
	if _, ok := instances["SlimHelperLibrary"].(*library); ok {
		return
	}
	instances["SlimHelperLibrary"] = &library{instances: instances, mu: mu}
}

func find(instances map[string]any, mu *sync.Mutex, name string) *seat {
	if name == "" {
		return nil
	}
	mu.Lock()
	defer mu.Unlock()
	seat, _ := instances[name].(*seat)
	return seat
}

func textAt(row []any, index int) string {
	if index >= len(row) {
		return ""
	}
	text, ok := row[index].(string)
	if !ok {
		return ""
	}
	return text
}

func argsFrom(row []any, index int) []any {
	if index >= len(row) {
		return nil
	}
	return row[index:]
}

func expandAll(args []any, symbols map[string]any) []any {
	out := make([]any, len(args))
	for i, arg := range args {
		out[i] = expand(arg, symbols)
	}
	return out
}

func expand(value any, symbols map[string]any) any {
	switch item := value.(type) {
	case string:
		return expandCell(item, symbols)
	case []any:
		return expandAll(item, symbols)
	default:
		return value
	}
}

func expandString(text string, symbols map[string]any) string {
	return asText(expandCell(text, symbols))
}

func symbolAt(text string, index int) (string, int, bool) {
	if index < 0 || index >= len(text) {
		return "", 0, false
	}
	loc := symbolPattern.FindStringSubmatchIndex(text[index:])
	if loc == nil || loc[0] != 0 {
		return "", 0, false
	}
	return text[index+loc[2] : index+loc[3]], index + loc[1], true
}

func expandCell(text string, symbols map[string]any) any {
	if value, ok := wholeStored(text, symbols); ok {
		return value
	}
	if assignmentPattern.MatchString(text) {
		return ""
	}
	return substitute(text, symbols)
}

func wholeStored(text string, symbols map[string]any) (any, bool) {
	if len(text) < 2 || text[0] != '$' {
		return nil, false
	}
	value, ok := symbols[text[1:]]
	return value, ok
}

func substitute(text string, symbols map[string]any) string {
	var b strings.Builder
	offset := 0
	for offset <= len(text) {
		loc := symbolPattern.FindStringSubmatchIndex(text[offset:])
		if loc == nil {
			b.WriteString(text[offset:])
			return b.String()
		}
		start := offset + loc[0]
		name := text[offset+loc[2] : offset+loc[3]]
		b.WriteString(text[offset:start])
		used, value, ok := resolve(name, symbols)
		if !ok {
			b.WriteString(text[start : offset+loc[1]])
			offset += loc[1]
			continue
		}
		b.WriteString(asText(value))
		offset = start + 1 + len(used)
	}
	return b.String()
}

func resolve(name string, symbols map[string]any) (string, any, bool) {
	if value, ok := symbols[name]; ok {
		return name, value, true
	}
	for i := len(name) - 1; i > 0; i-- {
		shorter := name[:i]
		if value, ok := symbols[shorter]; ok {
			return shorter, value, true
		}
	}
	return "", nil, false
}

func asText(value any) string {
	switch item := value.(type) {
	case string:
		return item
	case nil:
		return "null"
	default:
		return fmt.Sprint(item)
	}
}

func noteOptOut(symbols map[string]any) {
	value, ok := symbols[handlesSymbols]
	if !ok || value == nil {
		delete(symbols, optKey)
		return
	}
	compiled, err := regexp.Compile(`\A(?:` + fmt.Sprint(value) + `)\z`)
	if err != nil {
		delete(symbols, optKey)
		return
	}
	symbols[optKey] = compiled
}

func skipsSymbols(symbols map[string]any, instance, method string) bool {
	compiled, _ := symbols[optKey].(*regexp.Regexp)
	if compiled == nil {
		return false
	}
	return compiled.MatchString(instance + "." + method)
}

func convertArgs(method string, args []any) []any {
	kinds, ok := signatures[method]
	if !ok {
		return args
	}
	out := make([]any, len(args))
	for i, arg := range args {
		kind := "string"
		if i < len(kinds) {
			kind = kinds[i]
		}
		out[i] = convert(arg, kind)
	}
	return out
}

func convert(value any, kind string) any {
	text, ok := value.(string)
	if !ok {
		return value
	}
	switch kind {
	case "int":
		return parsedInt(text, value)
	case "long":
		return parsedLong(text, value)
	case "double":
		return parsedDouble(text, value)
	case "boolean":
		return text == "true"
	case "char":
		return parsedChar(text, value)
	case "date":
		return parsedDate(text, value)
	case "list":
		return parsedList(text)
	default:
		return text
	}
}

func parsedInt(text string, fallback any) any {
	number, err := strconv.Atoi(text)
	if err != nil {
		return fallback
	}
	return number
}

func parsedLong(text string, fallback any) any {
	number, err := strconv.ParseInt(text, 10, 64)
	if err != nil {
		return fallback
	}
	return number
}

func parsedDouble(text string, fallback any) any {
	number, err := strconv.ParseFloat(text, 64)
	if err != nil {
		return fallback
	}
	return number
}

func parsedChar(text string, fallback any) any {
	if utf8.RuneCountInString(text) != 1 {
		return fallback
	}
	runeValue, _ := utf8.DecodeRuneInString(text)
	return runeValue
}

func parsedDate(text string, fallback any) any {
	parsed, err := time.Parse("2006-01-02", text)
	if err != nil {
		return fallback
	}
	return parsed
}

func parsedList(text string) any {
	if len(text) < 2 || text[0] != '[' || text[len(text)-1] != ']' {
		return text
	}
	body := strings.TrimSpace(text[1 : len(text)-1])
	if body == "" {
		return []any{}
	}
	parts := strings.Split(body, ",")
	items := make([]any, len(parts))
	for i, part := range parts {
		items[i] = strings.TrimSpace(part)
	}
	return items
}

func parse(text string) []any {
	items, _ := readList(text)
	return items
}

func readList(text string) ([]any, int) {
	if !strings.HasPrefix(text, "[") {
		return nil, 0
	}
	count, index := readLength(text, 1)
	items := make([]any, 0, count)
	for n := 0; n < count; n++ {
		raw, next, ok := readField(text, index)
		if !ok {
			break
		}
		index = next
		items = append(items, fieldValue(raw))
	}
	return items, index
}

func fieldValue(raw string) any {
	if strings.HasPrefix(raw, "[") {
		nested, _ := readList(raw)
		return nested
	}
	return raw
}

func readField(text string, index int) (string, int, bool) {
	length, next := readLength(text, index)
	if next+length > len(text) {
		return "", index, false
	}
	return text[next : next+length], next + length + 1, true
}

func readLength(text string, index int) (int, int) {
	end := strings.IndexByte(text[index:], ':')
	if end < 0 {
		return 0, len(text)
	}
	end += index
	number, err := strconv.Atoi(text[index:end])
	if err != nil {
		return 0, end + 1
	}
	return number, end + 1
}

func serialize(value any) string {
	list, ok := value.([]any)
	if !ok {
		return fmt.Sprint(value)
	}
	var b strings.Builder
	b.WriteByte('[')
	fmt.Fprintf(&b, "%06d:", len(list))
	for _, part := range list {
		writeField(&b, serialize(part))
	}
	b.WriteByte(']')
	return b.String()
}

func writeField(b *strings.Builder, text string) {
	fmt.Fprintf(b, "%06d:%s:", len(text), text)
}
