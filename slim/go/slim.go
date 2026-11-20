// A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
// socket on 127.0.0.1. A lanes instruction runs each lane on its own goroutine
// against that one connection's instances.
package main

import (
	"fmt"
	"io"
	"net"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
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
	session(conn, map[string]*seat{}, &sync.Mutex{}, func() {})
	return nil
}

func session(conn net.Conn, instances map[string]*seat, mu *sync.Mutex, done func()) {
	defer done()
	defer conn.Close()
	speak(conn, conn, instances, mu)
}

func speak(in io.Reader, out io.Writer, instances map[string]*seat, mu *sync.Mutex) {
	if instances == nil {
		instances = map[string]*seat{}
		mu = &sync.Mutex{}
	}
	fmt.Fprint(out, "Slim -- V0.5\n")
	symbols := map[string]string{}
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

func answer(instructions []any, instances map[string]*seat, mu *sync.Mutex, symbols map[string]string) []any {
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

func effect(row []any, instances map[string]*seat, mu *sync.Mutex, symbols map[string]string) any {
	if textAt(row, 1) == "lanes" {
		return runLanes(row, instances, mu, symbols)
	}
	return reply(row, instances, mu, symbols)
}

func runLanes(row []any, instances map[string]*seat, mu *sync.Mutex, symbols map[string]string) any {
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

func reply(row []any, instances map[string]*seat, mu *sync.Mutex, symbols map[string]string) string {
	switch textAt(row, 1) {
	case "make":
		return made(row, instances, mu)
	case "call":
		return call(row, 2, 3, instances, mu, symbols)
	case "callAndAssign":
		value := call(row, 3, 4, instances, mu, symbols)
		remember(symbols, textAt(row, 2), value)
		return value
	default:
		return "OK"
	}
}

func remember(symbols map[string]string, name, value string) {
	if name == "" {
		return
	}
	symbols[name] = value
}

func call(row []any, instanceAt, methodAt int, instances map[string]*seat, mu *sync.Mutex, symbols map[string]string) string {
	method := textAt(row, methodAt)
	args := expandAll(argsFrom(row, methodAt+1), symbols)
	name := textAt(row, instanceAt)
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
	default:
		return "", false
	}
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
	return "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method " + method + "[" + strconv.Itoa(count) + "] in class " + className + ".\n Available methods:\n" + catalog + ">>"
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

func made(row []any, instances map[string]*seat, mu *sync.Mutex) string {
	name := textAt(row, 2)
	if name == "" {
		return "OK"
	}
	className := textAt(row, 3)
	if className == "" {
		className = "slim"
	}
	mu.Lock()
	instances[name] = opened(className, textAt(row, 4))
	mu.Unlock()
	return "OK"
}

func opened(className, mark string) *seat {
	if mark == "racy" || mark == "safe" {
		return &seat{seats: 1, race: mark, className: className}
	}
	return &seat{phase: "new", className: className}
}

const catalog = "beginTable() -> string\nblock() -> string\nbook() -> string\necho(string) -> string\nendIsAfterStart(string, string) -> string\nendTable() -> string\nexecute() -> string\nhello() -> string\nkind(string) -> string\nopen() -> string\nphase() -> string\nrelease() -> string\nreleased() -> string\nreset() -> string\nsetWord(string) -> string\ntable(list) -> string"

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

func find(instances map[string]*seat, mu *sync.Mutex, name string) *seat {
	if name == "" {
		return nil
	}
	mu.Lock()
	defer mu.Unlock()
	return instances[name]
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

func expandAll(args []any, symbols map[string]string) []any {
	out := make([]any, len(args))
	for i, arg := range args {
		out[i] = expand(arg, symbols)
	}
	return out
}

func expand(value any, symbols map[string]string) any {
	switch item := value.(type) {
	case string:
		return expandString(item, symbols)
	case []any:
		return expandAll(item, symbols)
	default:
		return value
	}
}

func expandString(text string, symbols map[string]string) string {
	var b strings.Builder
	for i := 0; i < len(text); i++ {
		name, next, ok := symbolAt(text, i)
		if !ok {
			b.WriteByte(text[i])
			continue
		}
		if value, known := symbols[name]; known {
			b.WriteString(value)
		} else {
			b.WriteString(text[i:next])
		}
		i = next - 1
	}
	return b.String()
}

func symbolAt(text string, i int) (string, int, bool) {
	if text[i] != '$' || i+1 >= len(text) || !nameStart(text[i+1]) {
		return "", 0, false
	}
	j := i + 2
	for j < len(text) && namePart(text[j]) {
		j++
	}
	return text[i+1 : j], j, true
}

func nameStart(c byte) bool {
	return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
}

func namePart(c byte) bool {
	return nameStart(c) || (c >= '0' && c <= '9') || c == '_'
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
