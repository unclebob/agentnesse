package main

import (
	"bytes"
	"fmt"
	"io"
	"net"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"
)

func TestHelloEchoAndAStoredSymbol(t *testing.T) {
	shared := map[string]any{}
	mu := &sync.Mutex{}
	symbols := map[string]any{}
	hello := []any{"1", "call", "actor", "hello"}
	if answer([]any{hello}, shared, mu, symbols)[0].([]any)[1] != "world" {
		t.Fatal("hello")
	}
	echo := []any{"2", "call", "actor", "echo", "hi"}
	if answer([]any{echo}, shared, mu, symbols)[0].([]any)[1] != "hi" {
		t.Fatal("echo")
	}
	assigned := []any{"3", "callAndAssign", "symbol", "actor", "hello"}
	if answer([]any{assigned}, shared, mu, symbols)[0].([]any)[1] != "world" {
		t.Fatal("assign")
	}
	if symbols["symbol"] != "world" {
		t.Fatal(symbols)
	}
	again := []any{"4", "call", "actor", "echo", "$symbol"}
	if answer([]any{again}, shared, mu, symbols)[0].([]any)[1] != "world" {
		t.Fatal("symbol")
	}
	nested := []any{"5", "call", "actor", "echo", []any{"$symbol"}}
	if answer([]any{nested}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("nested")
	}
	if answer([]any{"nope"}, shared, mu, symbols) != nil && len(answer([]any{"nope"}, shared, mu, symbols)) != 0 {
		t.Fatal("skipped")
	}
	if reply([]any{"9", "note"}, shared, mu, symbols) != "OK" {
		t.Fatal("other")
	}
	remember(symbols, "", "x")
	if _, ok := symbols[""]; ok {
		t.Fatal("blank name")
	}
}

func TestComparisonsAndKind(t *testing.T) {
	shared := map[string]any{}
	mu := &sync.Mutex{}
	symbols := map[string]any{}
	cases := []struct {
		row  []any
		want string
	}{
		{[]any{"1", "call", "a", "endIsAfterStart", "0", "1"}, "yes"},
		{[]any{"2", "call", "a", "endIsAfterStart", "2", "2"}, "no"},
		{[]any{"3", "call", "a", "endIsAfterStart", "4", "1"}, "no"},
		{[]any{"4", "call", "a", "endIsAfterStart", "a", "1"}, "no"},
		{[]any{"5", "call", "a", "endIsAfterStart"}, "no"},
		{[]any{"6", "call", "a", "endIsAfterStart", "1"}, "no"},
		{[]any{"7", "call", "a", "endIsAfterStart", []any{"1"}, "2"}, "no"},
		{[]any{"8", "call", "a", "kind", `{"doc":{"kind":"id"}}`}, "id"},
		{[]any{"9", "call", "a", "kind", `{"kind":"passport"}`}, "no"},
		{[]any{"10", "call", "a", "kind"}, "no"},
		{[]any{"11", "call", "a", "kind", []any{"x"}}, "no"},
	}
	for _, item := range cases {
		got := answer([]any{item.row}, shared, mu, symbols)[0].([]any)[1]
		if got != item.want {
			t.Fatalf("%v got %s", item.row, got)
		}
	}
	if textAt([]any{"1", 2}, 1) != "" {
		t.Fatal("text")
	}
	if textAt([]any{"1"}, 3) != "" {
		t.Fatal("short")
	}
	if argsFrom([]any{"1"}, 4) != nil {
		t.Fatal("args")
	}
	if expand(3, symbols) != 3 {
		t.Fatal("expand")
	}
	if expandString("$", symbols) != "$" {
		t.Fatal("dollar")
	}
	if expandString("$1no", symbols) != "$1no" {
		t.Fatal("digit")
	}
	symbols["kept"] = "yes"
	if expandString("pre $kept tail $missing", symbols) != "pre yes tail $missing" {
		t.Fatal(expandString("pre $kept tail $missing", symbols))
	}
	for _, name := range []string{"A", "Z", "a", "z", "a0", "a9", "a1", "a_b"} {
		symbols[name] = "YES"
		if expandString("$"+name, symbols) != "YES" {
			t.Fatal(name, expandString("$"+name, symbols))
		}
	}
	number, ok := whole([]any{"x"})
	if ok || number != 0 {
		t.Fatal(number, ok)
	}
	number, ok = whole("a")
	if ok || number != 0 {
		t.Fatal(number, ok)
	}
	_, next, ok := symbolAt("$", 0)
	if ok || next != 0 {
		t.Fatal(next, ok)
	}
}

func TestJavaSymbolForms(t *testing.T) {
	symbols := map[string]any{"ab": "YES", "Äpfel": "fruit", "`hello world`": "hi"}
	if expandString("$abc", symbols) != "YESc" {
		t.Fatal(expandString("$abc", symbols))
	}
	if expandString("$Äpfel!", symbols) != "fruit!" {
		t.Fatal("unicode")
	}
	if expandString("$`hello world`!", symbols) != "hi!" {
		t.Fatal("backtick")
	}
	if expandString("$missing", symbols) != "$missing" {
		t.Fatal("undefined")
	}
	symbols["box"] = []any{"a", "b"}
	if got := expandCell("$box", symbols); fmt.Sprint(got) != "[a b]" {
		t.Fatal(got)
	}
	if expandString("see $box", symbols) != "see [a b]" {
		t.Fatal("embedded object")
	}
	if expandCell("  $name=  ", symbols) != "" {
		t.Fatal("assignment")
	}
	if expand([]any{"$ab"}, symbols).([]any)[0] != "YES" {
		t.Fatal("nested")
	}
	shared := map[string]any{}
	mu := &sync.Mutex{}
	answer([]any{[]any{"1", "make", "gate", "slim"}}, shared, mu, symbols)
	symbols["actor"] = shared["gate"]
	if answer([]any{[]any{"3", "callAndAssign", "copy", "SlimHelperLibrary", "cloneSymbol", "$actor"}}, shared, mu, symbols)[0].([]any)[1] == nil {
		t.Fatal("clone")
	}
	if _, ok := symbols["copy"].(*seat); !ok {
		t.Fatal(symbols["copy"])
	}
	answer([]any{[]any{"4", "make", "again", "$copy"}}, shared, mu, symbols)
	if shared["again"] != symbols["copy"] {
		t.Fatal("actor identity")
	}
	answer([]any{[]any{"4b", "make", "stringy", "$ab"}}, shared, mu, symbols)
	if answer([]any{[]any{"4c", "call", "stringy", "phase"}}, shared, mu, symbols)[0].([]any)[1] != "new" {
		t.Fatal("string class")
	}
	if _, ok := actorFrom("$", symbols); ok {
		t.Fatal("short symbol")
	}
	if _, ok := actorFrom(3, symbols); ok {
		t.Fatal("non string class")
	}
	answer([]any{[]any{"6", "make", "scriptTableActor", "slim"}}, shared, mu, symbols)
	answer([]any{[]any{"7", "call", "SlimHelperLibrary", "pushFixture"}}, shared, mu, symbols)
	answer([]any{[]any{"8", "make", "scriptTableActor", "slim", "safe"}}, shared, mu, symbols)
	answer([]any{[]any{"8b", "call", "SlimHelperLibrary", "popFixture"}}, shared, mu, symbols)
	if answer([]any{[]any{"9", "call", "scriptTableActor", "phase"}}, shared, mu, symbols)[0].([]any)[1] != "new" {
		t.Fatal("pop")
	}
	answer([]any{[]any{"10", "assign", "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", `again\.echo`}}, shared, mu, symbols)
	if answer([]any{[]any{"11", "call", "again", "echo", "$ab"}}, shared, mu, symbols)[0].([]any)[1] != "$ab" {
		t.Fatal("opt out")
	}
	answer([]any{[]any{"12", "assign", "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS", "["}}, shared, mu, symbols)
	if answer([]any{[]any{"13", "call", "again", "echo", "$ab"}}, shared, mu, symbols)[0].([]any)[1] != "YES" {
		t.Fatal("bad regex")
	}
	if answer([]any{[]any{"14", "call", "again", "plus", "2", "3"}}, shared, mu, symbols)[0].([]any)[1] != "5" {
		t.Fatal("int")
	}
	if answer([]any{[]any{"14b", "call", "again", "plus", "2"}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("plus short")
	}
	if answer([]any{[]any{"14c", "call", "again", "plus", "a", "1"}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("plus text")
	}
	if answer([]any{[]any{"14d", "call", "again", "flag", "false"}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("flag")
	}
	if answer([]any{[]any{"14e", "call", "again", "join", "red"}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("join plain")
	}
	if answer([]any{[]any{"14f", "call", "again", "join", "[]"}}, shared, mu, symbols)[0].([]any)[1] != "" {
		t.Fatal("join empty")
	}
	if answer([]any{[]any{"14g", "call", "again", "label", []any{[]any{"other", "Ada"}}}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("label miss")
	}
	if answer([]any{[]any{"14h", "call", "again", "scale", "nope"}}, shared, mu, symbols)[0].([]any)[1] != "no" {
		t.Fatal("scale")
	}
	if cellAt([]any{"only"}, 3) != "" {
		t.Fatal("cell")
	}
	if answer([]any{[]any{"15", "call", "again", "wide", "4"}}, shared, mu, symbols)[0].([]any)[1] != "4" {
		t.Fatal("long")
	}
	if answer([]any{[]any{"16", "call", "again", "scale", "1.5"}}, shared, mu, symbols)[0].([]any)[1] != "1.5" {
		t.Fatal("double")
	}
	if answer([]any{[]any{"17", "call", "again", "flag", "true"}}, shared, mu, symbols)[0].([]any)[1] != "yes" {
		t.Fatal("bool")
	}
	if answer([]any{[]any{"18", "call", "again", "initial", "Q"}}, shared, mu, symbols)[0].([]any)[1] != "Q" {
		t.Fatal("char")
	}
	if answer([]any{[]any{"19", "call", "again", "day", "2020-01-02"}}, shared, mu, symbols)[0].([]any)[1] != "2020-01-02" {
		t.Fatal("date")
	}
	if answer([]any{[]any{"20", "call", "again", "shade", "green"}}, shared, mu, symbols)[0].([]any)[1] != "green" {
		t.Fatal("enum")
	}
	if answer([]any{[]any{"21", "call", "again", "join", "[red, blue]"}}, shared, mu, symbols)[0].([]any)[1] != "red,blue" {
		t.Fatal("list")
	}
	if answer([]any{[]any{"22", "call", "again", "label", []any{[]any{"name", "Ada"}}}}, shared, mu, symbols)[0].([]any)[1] != "Ada" {
		t.Fatal("map")
	}
}

func TestAGateAndADecisionWord(t *testing.T) {
	shared := map[string]any{}
	mu := &sync.Mutex{}
	symbols := map[string]any{}
	batch := []any{
		[]any{"1", "make", "gate", "slim"},
		[]any{"2", "call", "gate", "open"},
		[]any{"3", "call", "gate", "block"},
		[]any{"4", "call", "gate", "release"},
		[]any{"5", "call", "gate", "phase"},
		[]any{"6", "call", "gate", "released"},
		[]any{"7", "make", "other", "slim"},
		[]any{"8", "call", "other", "phase"},
		[]any{"9", "call", "gate", "setWord", "red"},
		[]any{"10", "call", "gate", "echo"},
		[]any{"11", "call", "gate", "hello"},
	}
	got := map[string]any{}
	for _, row := range answer(batch, shared, mu, symbols) {
		pair := row.([]any)
		got[pair[0].(string)] = pair[1].(string)
	}
	wants := map[string]string{
		"1": "OK", "2": "ok", "3": "ok", "4": "refused", "5": "blocked",
		"6": "false", "8": "new", "9": "OK", "10": "red", "11": "world",
	}
	for id, want := range wants {
		if got[id] != want {
			t.Fatalf("%s got %s", id, got[id])
		}
	}
	if answer([]any{[]any{"12", "call", "gate", "open"}}, shared, mu, symbols)[0].([]any)[1] != "refused" {
		t.Fatal("reopen")
	}
	opened := map[string]any{}
	openMu := &sync.Mutex{}
	answer([]any{[]any{"1", "make", "gate", "slim"}}, opened, openMu, map[string]any{})
	answer([]any{[]any{"2", "call", "gate", "open"}}, opened, openMu, map[string]any{})
	if answer([]any{[]any{"3", "call", "gate", "release"}}, opened, openMu, map[string]any{})[0].([]any)[1] != "ok" {
		t.Fatal("release")
	}
	if answer([]any{[]any{"4", "call", "gate", "phase"}}, opened, openMu, map[string]any{})[0].([]any)[1] != "released" {
		t.Fatal("phase")
	}
	if answer([]any{[]any{"5", "call", "gate", "released"}}, opened, openMu, map[string]any{})[0].([]any)[1] != "true" {
		t.Fatal("flag")
	}
	if answer([]any{[]any{"6", "call", "missing", "open"}}, shared, mu, symbols)[0].([]any)[1] != "refused" {
		t.Fatal("missing open")
	}
	if answer([]any{[]any{"7", "call", "missing", "phase"}}, shared, mu, symbols)[0].([]any)[1] != "" {
		t.Fatal("missing phase")
	}
	if answer([]any{[]any{"8", "call", "missing", "released"}}, shared, mu, symbols)[0].([]any)[1] != "false" {
		t.Fatal("missing released")
	}
	if answer([]any{[]any{"9", "call", "missing", "echo"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("missing echo")
	}
	if answer([]any{[]any{"10", "call", "missing", "setWord", "x"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("missing set")
	}
	if answer([]any{[]any{"11", "make"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("make")
	}
	if answer([]any{[]any{"12", "make", "", "slim"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("blank make")
	}
	if answer([]any{[]any{"13", "call", "", "open"}}, shared, mu, symbols)[0].([]any)[1] != "refused" {
		t.Fatal("blank call")
	}
	if answer([]any{[]any{"14", "make", []any{"gate"}}, []any{"15", "call", []any{"gate"}, "open"}}, shared, mu, symbols)[1].([]any)[1] != "refused" {
		t.Fatal("list name")
	}
	unknown := answer([]any{[]any{"16", "call", "gate", "noSuch"}}, shared, mu, symbols)[0].([]any)[1].(string)
	if !strings.Contains(unknown, "No Method noSuch[0] in class slim") || !strings.Contains(unknown, "Available methods:") || !strings.Contains(unknown, "hello() -> string") {
		t.Fatal(unknown)
	}
	withArg := answer([]any{[]any{"17", "call", "gate", "noSuch", "x"}}, shared, mu, symbols)[0].([]any)[1].(string)
	if !strings.Contains(withArg, "No Method noSuch[1] in class slim") {
		t.Fatal(withArg)
	}
	missing := answer([]any{[]any{"18", "call", "absent", "noSuch"}}, shared, mu, symbols)[0].([]any)[1].(string)
	if !strings.Contains(missing, "NO_INSTANCE absent") {
		t.Fatal(missing)
	}
	if answer([]any{[]any{"19", "call", "gate", "beginTable"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("begin")
	}
	if answer([]any{[]any{"20", "call", "gate", "execute"}}, shared, mu, symbols)[0].([]any)[1] != "OK" {
		t.Fatal("execute")
	}
}

func TestBooksAndTheProbe(t *testing.T) {
	shared := map[string]any{}
	mu := &sync.Mutex{}
	symbols := map[string]any{}
	answer([]any{[]any{"1", "make", "seat", "slim", "safe"}}, shared, mu, symbols)
	if answer([]any{[]any{"2", "call", "seat", "book"}}, shared, mu, symbols)[0].([]any)[1] != "booked" {
		t.Fatal("first")
	}
	if answer([]any{[]any{"3", "call", "seat", "book"}}, shared, mu, symbols)[0].([]any)[1] != "full" {
		t.Fatal("second")
	}
	probed := answer([]any{[]any{"4", "lanes"}}, shared, mu, symbols)[0].([]any)[1]
	if probed != "lanes" {
		t.Fatal(probed)
	}
	missing := answer([]any{[]any{"5", "call", "other", "book"}}, shared, mu, symbols)[0].([]any)[1].(string)
	if !strings.Contains(missing, "NO_INSTANCE other") {
		t.Fatal(missing)
	}
	bare := answer([]any{[]any{"6", "lanes", "not-a-group"}}, map[string]any{}, &sync.Mutex{}, symbols)[0].([]any)[1].([]any)
	if len(bare) != 0 {
		t.Fatal(bare)
	}
	raced := map[string]any{}
	answer([]any{[]any{"1", "make", "seat", "slim", "racy"}}, raced, mu, symbols)
	if answer([]any{[]any{"2", "call", "seat", "book"}}, raced, mu, symbols)[0].([]any)[1] != "booked" {
		t.Fatal("racy")
	}
	if answer([]any{[]any{"3", "call", "seat", "book"}}, raced, mu, symbols)[0].([]any)[1] != "full" {
		t.Fatal("racy full")
	}
	gate := map[string]any{}
	answer([]any{[]any{"1", "make", "gate", "slim"}}, gate, mu, symbols)
	booked := answer([]any{[]any{"2", "call", "gate", "book"}}, gate, mu, symbols)[0].([]any)[1].(string)
	if !strings.Contains(booked, "NO_INSTANCE") {
		t.Fatal(booked)
	}
}

func TestOverlappingBooks(t *testing.T) {
	racy := both("racy")
	if !same(racy.results, "booked", "booked") || !racy.overlap {
		t.Fatal(racy)
	}
	safe := both("safe")
	if !same(safe.results, "booked", "full") {
		t.Fatal(safe)
	}
}

type laneHit struct {
	results []string
	overlap bool
}

func both(mark string) laneHit {
	shared := map[string]any{}
	mu := &sync.Mutex{}
	answer([]any{[]any{"1", "make", "seat", "slim", mark}}, shared, mu, map[string]any{})
	got := answer([]any{[]any{"9", "lanes",
		[]any{"t1", []any{[]any{"a", "call", "seat", "book"}}},
		[]any{"t2", []any{[]any{"b", "call", "seat", "book"}}},
	}}, shared, mu, map[string]any{})
	rows := got[0].([]any)[1].([]any)
	return laneHit{[]string{cell(rows[0]), cell(rows[1])}, overlaps(rows)}
}

func cell(row any) string {
	return row.([]any)[4].(string)
}

func overlaps(rows []any) bool {
	left := rows[0].([]any)
	right := rows[1].([]any)
	return before(left[2], right[3]) && before(right[2], left[3])
}

func before(early, late any) bool {
	start, err := strconv.Atoi(early.(string))
	if err != nil {
		return false
	}
	finish, err := strconv.Atoi(late.(string))
	if err != nil {
		return false
	}
	return start < finish
}

func same(got []string, a, b string) bool {
	return (got[0] == a && got[1] == b) || (got[0] == b && got[1] == a)
}

func TestPipeAndSocket(t *testing.T) {
	incoming := frame([]any{"1", "call", "actor", "hello"}) + frame([]any{"2", "call", "actor", "echo", "hi"}) + frame("bye")
	var out bytes.Buffer
	speak(&oneByte{raw: []byte(incoming)}, &out, nil, nil)
	text := out.String()
	if !strings.HasPrefix(text, "Slim -- V0.5\n") || !strings.Contains(text, "world") || !strings.Contains(text, "hi") {
		t.Fatal(text)
	}
	var empty bytes.Buffer
	speak(bytes.NewReader(nil), &empty, nil, nil)
	if empty.String() != "Slim -- V0.5\n" {
		t.Fatal(empty.String())
	}
	var short bytes.Buffer
	speak(bytes.NewReader([]byte("000010:short")), &short, nil, nil)
	if short.String() != "Slim -- V0.5\n" {
		t.Fatal(short.String())
	}
	var bye bytes.Buffer
	speak(bytes.NewReader([]byte(frame("BYE"))), &bye, nil, nil)
	if !strings.Contains(bye.String(), "Slim -- V0.5\n") || strings.Contains(bye.String(), "world") {
		t.Fatal(bye.String())
	}
	if _, ok := readPrefix(&zeroReader{}); ok {
		t.Fatal("zero")
	}
	if _, ok := readPrefix(bytes.NewReader([]byte("000000000000"))); ok {
		t.Fatal("long")
	}
	if _, ok := readMessage(bytes.NewReader([]byte("abc:"))); ok {
		t.Fatal("bad length")
	}
	if _, ok := readMessage(bytes.NewReader([]byte("-1:"))); ok {
		t.Fatal("negative")
	}
	if _, ok := readExact(bytes.NewReader([]byte("ab")), 4); ok {
		t.Fatal("exact")
	}
	if run([]string{}) == nil || run([]string{"slim", "nope"}) == nil || run([]string{"slim", "-1"}) == nil {
		t.Fatal("ports")
	}
	if status(nil) != 0 || status(fmt.Errorf("port")) != 1 {
		t.Fatal("status")
	}
	if rejected(0, nil) || !rejected(-1, nil) || rejected(1, nil) || !rejected(1, fmt.Errorf("x")) {
		t.Fatal("rejected")
	}
	var emptyMessage bytes.Buffer
	speak(bytes.NewReader([]byte("000000:"+frame("bye"))), &emptyMessage, nil, nil)
	if !strings.Contains(emptyMessage.String(), "[000000:]") {
		t.Fatal(emptyMessage.String())
	}
	if _, ok := readMessage(bytes.NewReader([]byte("0000000006:hello!"))); !ok {
		t.Fatal("ten digits")
	}
	if _, ok := readPrefix(&zeroThenColon{}); ok {
		t.Fatal("zero read continues")
	}
	if _, ok := readExact(&zeroThenByte{}, 1); ok {
		t.Fatal("exact continues")
	}
}

func TestSocketSharesASeat(t *testing.T) {
	port := freePort(t)
	done := make(chan error, 1)
	go func() { done <- serve(port) }()
	first := dial(t, port)
	defer first.Close()
	if readUntil(t, first, "\n") != "Slim -- V0.5\n" {
		t.Fatal("header")
	}
	send(t, first, []any{"1", "make", "seat", "slim", "safe"})
	if !strings.Contains(readUntil(t, first, "OK"), "OK") {
		t.Fatal("make")
	}
	send(t, first, []any{"2", "call", "seat", "book"})
	if !strings.Contains(readUntil(t, first, "booked"), "booked") {
		t.Fatal("booked")
	}
	send(t, first, []any{"3", "call", "seat", "book"})
	if !strings.Contains(readUntil(t, first, "full"), "full") {
		t.Fatal("full")
	}
	sendRaw(t, first, "bye")
	first.Close()
	select {
	case err := <-done:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("server stayed up")
	}
}

func TestIdleAndAClosedListener(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	if serve(listener.Addr().(*net.TCPAddr).Port) == nil {
		t.Fatal("expected bind failure")
	}
}

func TestServerWaitsForTheFirstConnection(t *testing.T) {
	port := freePort(t)
	done := make(chan error, 1)
	go func() { done <- serve(port) }()
	select {
	case err := <-done:
		t.Fatal("exited before a client", err)
	case <-time.After(500 * time.Millisecond):
	}
	conn := dial(t, port)
	defer conn.Close()
	readUntil(t, conn, "\n")
	sendRaw(t, conn, "bye")
	conn.Close()
	select {
	case err := <-done:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("server stayed up")
	}
}

func TestParseEdges(t *testing.T) {
	items, index := readList("nope")
	if items != nil || index != 0 {
		t.Fatal(items, index)
	}
	if parse("nope") != nil {
		t.Fatal(parse("nope"))
	}
	counted := parse("[000001:000001:a:000001:b:]")
	if len(counted) != 1 || counted[0] != "a" {
		t.Fatal(counted)
	}
	exact := parse("[000001:000001:z")
	if len(exact) != 1 || exact[0] != "z" {
		t.Fatal(exact)
	}
	if tail := argsFrom([]any{"1"}, 1); tail != nil {
		t.Fatal(tail)
	}
	if _, at := readLength(":12", 0); at != 1 {
		t.Fatal(at)
	}
	if _, at := readLength("abc:", 0); at != 4 {
		t.Fatal(at)
	}
	if number, at := readLength("xx000001:", 2); number != 1 || at != 9 {
		t.Fatal(number, at)
	}
	if parse("[000001:000003:ab") != nil && len(parse("[000001:000003:ab")) != 0 {
		t.Fatal("truncated")
	}
	nestedItems := parse(serialize([]any{[]any{"z"}}))
	nested, ok := nestedItems[0].([]any)
	if !ok || nested[0] != "z" {
		t.Fatal(nestedItems)
	}
	if got, at := readLength("", 0); got != 0 || at != 0 {
		t.Fatal("empty length")
	}
	if got, index := readLength("abc", 0); got != 0 || index != 3 {
		t.Fatal("no colon")
	}
	if got, _ := readLength("no:", 0); got != 0 {
		t.Fatal("bad number")
	}
	if serialize("world") != "world" {
		t.Fatal(serialize("world"))
	}
}


type zeroReader struct{}

func (zeroReader) Read([]byte) (int, error) { return 0, nil }

type zeroThenColon struct {
	reads int
}

func (r *zeroThenColon) Read(p []byte) (int, error) {
	if len(p) == 0 {
		return 0, nil
	}
	r.reads++
	if r.reads == 1 {
		return 0, nil
	}
	p[0] = ':'
	return 1, nil
}

type zeroThenByte struct {
	reads int
}

func (r *zeroThenByte) Read(p []byte) (int, error) {
	r.reads++
	if r.reads == 1 || len(p) == 0 {
		return 0, nil
	}
	p[0] = 'x'
	return 1, nil
}

type oneByte struct {
	raw     []byte
	at      int
	shorted bool
}

func (r *oneByte) Read(p []byte) (int, error) {
	if len(p) == 0 || r.at >= len(r.raw) {
		return 0, io.EOF
	}
	n := len(p)
	if n > 1 && !r.shorted {
		r.shorted = true
		n = 1
	}
	if n > len(r.raw)-r.at {
		n = len(r.raw) - r.at
	}
	copy(p, r.raw[r.at:r.at+n])
	r.at += n
	return n, nil
}

func frame(value any) string {
	if text, ok := value.(string); ok && strings.EqualFold(text, "bye") {
		return fmt.Sprintf("%06d:%s", len(text), text)
	}
	body := serialize([]any{value})
	return fmt.Sprintf("%06d:%s", len(body), body)
}

func freePort(t *testing.T) int {
	t.Helper()
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer ln.Close()
	return ln.Addr().(*net.TCPAddr).Port
}

func dial(t *testing.T, port int) net.Conn {
	t.Helper()
	var last error
	for i := 0; i < 50; i++ {
		conn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 50*time.Millisecond)
		if err == nil {
			return conn
		}
		last = err
		time.Sleep(20 * time.Millisecond)
	}
	t.Fatal(last)
	return nil
}

func send(t *testing.T, conn net.Conn, row []any) {
	t.Helper()
	sendRaw(t, conn, frame(row))
}

func sendRaw(t *testing.T, conn net.Conn, payload string) {
	t.Helper()
	if _, err := io.WriteString(conn, payload); err != nil {
		t.Fatal(err)
	}
}

func readUntil(t *testing.T, conn net.Conn, marker string) string {
	t.Helper()
	_ = conn.SetReadDeadline(time.Now().Add(2 * time.Second))
	buf := make([]byte, 0, 128)
	tmp := make([]byte, 64)
	for !strings.Contains(string(buf), marker) {
		n, err := conn.Read(tmp)
		buf = append(buf, tmp[:n]...)
		if err != nil {
			t.Fatal(err, string(buf))
		}
	}
	return string(buf)
}
