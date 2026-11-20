use slim::{drive, expand_string, parse, read_length, rejected, run, serialize, serve, status, whole, Gate, Value};
use std::collections::HashMap;
use std::io::{Cursor, Read, Write};
use std::net::TcpStream;
use std::thread;
use std::time::Duration;

fn t(text: &str) -> Value { Value::Text(text.into()) }

fn call(id: &str, instance: &str, method: &str, args: &[&str]) -> Value {
    let mut row = vec![t(id), t("call"), t(instance), t(method)];
    for arg in args { row.push(t(arg)); }
    Value::List(row)
}

fn cell(value: &Value, index: usize) -> String {
    let Value::List(rows) = value else { return String::new() };
    let Value::List(pair) = &rows[index] else { return String::new() };
    match &pair[1] { Value::Text(text) => text.clone(), Value::List(_) => String::new() }
}

#[test]
fn hello_echo_symbol_and_ports() {
    let gate = Gate::new();
    assert_eq!(cell(&gate.answer(&[call("1", "actor", "hello", &[])]), 0), "world");
    assert_eq!(cell(&gate.answer(&[call("2", "actor", "echo", &["hi"])]), 0), "hi");
    let assigned = Value::List(vec![t("3"), t("callAndAssign"), t("symbol"), t("actor"), t("hello")]);
    assert_eq!(cell(&gate.answer(&[assigned]), 0), "world");
    assert_eq!(cell(&gate.answer(&[call("4", "actor", "echo", &["$symbol"])]), 0), "world");
    let nested = Value::List(vec![t("5"), t("call"), t("actor"), t("echo"), Value::List(vec![t("$symbol")])]);
    assert_eq!(cell(&gate.answer(&[nested]), 0), "OK");
    assert!(gate.answer(&[t("nope")]).eq_list_empty());
    assert_eq!(cell(&gate.answer(&[Value::List(vec![t("9"), t("note")])]), 0), "OK");
    assert_eq!(status(Ok(())), 0);
    assert_eq!(status(Err("x".into())), 1);
    assert!(run(&[]).is_err());
    assert!(run(&[String::from("slim"), String::from("nope")]).is_err());
    assert!(run(&[String::from("slim"), String::from("-1")]).is_err());
    assert!(!rejected(0) && !rejected(1) && rejected(-1));
}

trait Empty { fn eq_list_empty(&self) -> bool; }
impl Empty for Value {
    fn eq_list_empty(&self) -> bool { matches!(self, Value::List(rows) if rows.is_empty()) }
}

#[test]
fn comparisons_kind_and_symbols() {
    let gate = Gate::new();
    let cases = [
        (call("1", "a", "endIsAfterStart", &["0", "1"]), "yes"),
        (call("2", "a", "endIsAfterStart", &["2", "2"]), "no"),
        (call("3", "a", "endIsAfterStart", &["4", "1"]), "no"),
        (call("4", "a", "endIsAfterStart", &["a", "1"]), "no"),
        (call("5", "a", "endIsAfterStart", &[]), "no"),
        (call("6", "a", "endIsAfterStart", &["1"]), "no"),
        (call("8", "a", "kind", &[r#"{"doc":{"kind":"id"}}"#]), "id"),
        (call("9", "a", "kind", &[r#"{"kind":"passport"}"#]), "no"),
        (call("10", "a", "kind", &[]), "no"),
    ];
    for (row, want) in cases {
        assert_eq!(cell(&gate.answer(&[row]), 0), want);
    }
    let nested = Value::List(vec![t("7"), t("call"), t("a"), t("endIsAfterStart"), Value::List(vec![t("1")]), t("2")]);
    assert_eq!(cell(&gate.answer(&[nested]), 0), "no");
    let list_kind = Value::List(vec![t("11"), t("call"), t("a"), t("kind"), Value::List(vec![t("x")])]);
    assert_eq!(cell(&gate.answer(&[list_kind]), 0), "no");
    assert_eq!(whole(&Value::List(vec![t("1")])), None);
    assert_eq!(whole(&t("a")), None);
    assert_eq!(whole(&t("4")), Some(4));
    let mut symbols = HashMap::new();
    assert_eq!(expand_string("$", &symbols), "$");
    assert_eq!(expand_string("$1no", &symbols), "$1no");
    symbols.insert("kept".into(), "yes".into());
    assert_eq!(expand_string("pre $kept tail $missing", &symbols), "pre yes tail $missing");
    for name in ["A", "Z", "a", "z", "a0", "a9", "a1", "a_b"] {
        symbols.insert(name.into(), "YES".into());
        assert_eq!(expand_string(&format!("${name}"), &symbols), "YES", "{name}");
    }
}

#[test]
fn a_gate_and_a_word() {
    let gate = Gate::new();
    let batch = vec![
        Value::List(vec![t("1"), t("make"), t("gate"), t("slim")]),
        call("2", "gate", "open", &[]),
        call("3", "gate", "block", &[]),
        call("4", "gate", "release", &[]),
        call("5", "gate", "phase", &[]),
        call("6", "gate", "released", &[]),
        Value::List(vec![t("7"), t("make"), t("other"), t("slim")]),
        call("8", "other", "phase", &[]),
        call("9", "gate", "setWord", &["red"]),
        call("10", "gate", "echo", &[]),
        call("11", "gate", "hello", &[]),
    ];
    let got = gate.answer(&batch);
    let wants = [("1", "OK"), ("2", "ok"), ("3", "ok"), ("4", "refused"), ("5", "blocked"), ("6", "false"), ("8", "new"), ("9", "OK"), ("10", "red"), ("11", "world")];
    for (index, want) in wants {
        assert_eq!(find_id(&got, index), want, "{index}");
    }
    assert_eq!(cell(&gate.answer(&[call("12", "gate", "open", &[])]), 0), "refused");
    let open = Gate::new();
    open.answer(&[Value::List(vec![t("1"), t("make"), t("gate"), t("slim")])]);
    open.answer(&[call("2", "gate", "open", &[])]);
    assert_eq!(cell(&open.answer(&[call("3", "gate", "release", &[])]), 0), "ok");
    assert_eq!(cell(&open.answer(&[call("4", "gate", "phase", &[])]), 0), "released");
    assert_eq!(cell(&open.answer(&[call("5", "gate", "released", &[])]), 0), "true");
    assert_eq!(cell(&gate.answer(&[call("13", "missing", "open", &[])]), 0), "refused");
    assert_eq!(cell(&gate.answer(&[call("14", "missing", "phase", &[])]), 0), "");
    assert_eq!(cell(&gate.answer(&[call("15", "missing", "released", &[])]), 0), "false");
    assert_eq!(cell(&gate.answer(&[call("16", "missing", "echo", &[])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[call("17", "missing", "setWord", &["x"])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[Value::List(vec![t("18"), t("make")])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[Value::List(vec![t("19"), t("make"), t(""), t("slim")])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[call("20", "", "open", &[])]), 0), "refused");
    let unknown = cell(&gate.answer(&[call("21", "gate", "noSuch", &[])]), 0);
    assert!(unknown.contains("No Method noSuch[0] in class slim"), "{unknown}");
    assert!(unknown.contains("Available methods:") && unknown.contains("hello() -> string"));
    assert!(cell(&gate.answer(&[call("22", "gate", "noSuch", &["x"])]), 0).contains("No Method noSuch[1] in class slim"));
    assert!(cell(&gate.answer(&[call("23", "absent", "noSuch", &[])]), 0).contains("NO_INSTANCE absent"));
    assert_eq!(cell(&gate.answer(&[call("24", "gate", "beginTable", &[])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[call("25", "gate", "execute", &[])]), 0), "OK");
    assert_eq!(cell(&gate.answer(&[call("22", "gate", "block", &[])]), 0), "refused");
}

fn find_id(value: &Value, id: &str) -> String {
    let Value::List(rows) = value else { return String::new() };
    for row in rows {
        let Value::List(pair) = row else { continue };
        if text(&pair[0]) == id { return text(&pair[1]); }
    }
    String::new()
}

fn text(value: &Value) -> String {
    match value { Value::Text(text) => text.clone(), Value::List(_) => String::new() }
}

#[test]
fn symbol_prefixes_unicode_backticks_and_assignments() {
    let mut symbols = std::collections::HashMap::new();
    symbols.insert("ab".into(), "YES".into());
    symbols.insert("Äpfel".into(), "fruit".into());
    symbols.insert("`hello world`".into(), "hi".into());
    assert_eq!(expand_string("$abc", &symbols), "YESc");
    assert_eq!(expand_string("$Äpfel!", &symbols), "fruit!");
    assert_eq!(expand_string("$`hello world`!", &symbols), "hi!");
    assert_eq!(expand_string("$missing", &symbols), "$missing");
    assert_eq!(expand_string("  $name=  ", &symbols), "");
}

#[test]
fn objects_actors_opt_out_and_typed_arguments() {
    let gate = Gate::new();
    gate.answer(&[Value::List(vec![t("1"), t("make"), t("gate"), t("slim")])]);
    gate.answer(&[Value::List(vec![t("2"), t("callAndAssign"), t("actor"), t("gate"), t("self")])]);
    gate.answer(&[Value::List(vec![t("3"), t("make"), t("again"), t("$actor")])]);
    gate.answer(&[Value::List(vec![t("4"), t("call"), t("gate"), t("open")])]);
    assert_eq!(cell(&gate.answer(&[call("5", "again", "phase", &[])]), 0), "open");
    gate.answer(&[Value::List(vec![t("6"), t("make"), t("scriptTableActor"), t("slim")])]);
    gate.answer(&[call("7", "SlimHelperLibrary", "pushFixture", &[])]);
    gate.answer(&[Value::List(vec![t("8"), t("make"), t("scriptTableActor"), t("slim"), t("safe")])]);
    gate.answer(&[call("9", "SlimHelperLibrary", "popFixture", &[])]);
    assert_eq!(cell(&gate.answer(&[call("10", "scriptTableActor", "phase", &[])]), 0), "new");
    gate.answer(&[Value::List(vec![t("11"), t("assign"), t("ab"), t("YES")])]);
    gate.answer(&[Value::List(vec![t("12"), t("assign"), t("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS"), t("again\\.echo")])]);
    assert_eq!(cell(&gate.answer(&[call("13", "again", "echo", &["$ab"])]), 0), "$ab");
    gate.answer(&[Value::List(vec![t("14"), t("assign"), t("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS"), t("[")])]);
    assert_eq!(cell(&gate.answer(&[call("15", "again", "echo", &["$ab"])]), 0), "YES");
    assert_eq!(cell(&gate.answer(&[call("16", "again", "plus", &["2", "3"])]), 0), "5");
    assert_eq!(cell(&gate.answer(&[call("17", "again", "flag", &["true"])]), 0), "yes");
    assert_eq!(cell(&gate.answer(&[call("18", "again", "join", &["[red, blue]"])]), 0), "red,blue");
    assert_eq!(cell(&gate.answer(&[call("19", "again", "day", &["2020-01-02"])]), 0), "2020-01-02");
    assert_eq!(cell(&gate.answer(&[call("20", "again", "shade", &["green"])]), 0), "green");
}

#[test]
fn books_and_the_probe() {
    let gate = Gate::new();
    gate.answer(&[Value::List(vec![t("1"), t("make"), t("seat"), t("slim"), t("safe")])]);
    assert_eq!(cell(&gate.answer(&[call("2", "seat", "book", &[])]), 0), "booked");
    assert_eq!(cell(&gate.answer(&[call("3", "seat", "book", &[])]), 0), "full");
    assert_eq!(cell(&gate.answer(&[Value::List(vec![t("4"), t("lanes")])]), 0), "lanes");
    let skipped = gate.answer(&[Value::List(vec![t("6"), t("lanes"), t("not-a-group")])]);
    assert_eq!(list_cell(&skipped), Vec::<Value>::new());
    assert!(cell(&gate.answer(&[call("5", "other", "book", &[])]), 0).contains("NO_INSTANCE other"));
    let raced = Gate::new();
    raced.answer(&[Value::List(vec![t("1"), t("make"), t("seat"), t("slim"), t("racy")])]);
    assert_eq!(cell(&raced.answer(&[call("2", "seat", "book", &[])]), 0), "booked");
    assert_eq!(cell(&raced.answer(&[call("3", "seat", "book", &[])]), 0), "full");
    let plain = Gate::new();
    plain.answer(&[Value::List(vec![t("1"), t("make"), t("gate"), t("slim")])]);
    assert!(cell(&plain.answer(&[call("2", "gate", "book", &[])]), 0).contains("NO_INSTANCE"));
}

#[test]
fn overlapping_books() {
    let racy = both("racy");
    assert!(same(&racy.results, "booked", "booked") && racy.overlap);
    let safe = both("safe");
    assert!(same(&safe.results, "booked", "full"));
}

struct LaneHit { results: [String; 2], overlap: bool }

fn both(mark: &str) -> LaneHit {
    let gate = Gate::new();
    gate.answer(&[Value::List(vec![t("1"), t("make"), t("seat"), t("slim"), t(mark)])]);
    let got = gate.answer(&[Value::List(vec![
        t("9"), t("lanes"),
        Value::List(vec![t("t1"), Value::List(vec![call("a", "seat", "book", &[])])]),
        Value::List(vec![t("t2"), Value::List(vec![call("b", "seat", "book", &[])])]),
    ])]);
    let rows = list_cell(&got);
    LaneHit { results: [hit(&rows[0]), hit(&rows[1])], overlap: overlaps(&rows) }
}

fn list_cell(value: &Value) -> Vec<Value> {
    let Value::List(results) = value else { return Vec::new() };
    let Some(Value::List(row)) = results.first() else { return Vec::new() };
    match row.get(1) { Some(Value::List(items)) => items.clone(), _ => Vec::new() }
}

fn hit(value: &Value) -> String {
    let Value::List(row) = value else { return String::new() };
    text(row.get(4).unwrap_or(&t("")))
}

fn overlaps(rows: &[Value]) -> bool {
    let Value::List(left) = &rows[0] else { return false };
    let Value::List(right) = &rows[1] else { return false };
    before(left.get(2), right.get(3)) && before(right.get(2), left.get(3))
}

fn before(early: Option<&Value>, late: Option<&Value>) -> bool {
    let Some(Value::Text(start)) = early else { return false };
    let Some(Value::Text(finish)) = late else { return false };
    match (start.parse::<u64>(), finish.parse::<u64>()) {
        (Ok(start), Ok(finish)) => start < finish,
        _ => false,
    }
}

fn same(got: &[String; 2], a: &str, b: &str) -> bool {
    (got[0] == a && got[1] == b) || (got[0] == b && got[1] == a)
}

#[test]
fn pipe_frames_and_lengths() {
    let hello = frame(&call("1", "actor", "hello", &[]));
    let echo = frame(&call("2", "actor", "echo", &["hi"]));
    let mut out = Vec::new();
    drive(&mut OneByte::new(format!("{hello}{echo}000003:bye").into_bytes()), &mut out);
    let text = String::from_utf8(out).unwrap();
    assert!(text.starts_with("Slim -- V0.5\n") && text.contains("world") && text.contains("hi"));
    let mut empty = Vec::new();
    drive(&mut Cursor::new(Vec::<u8>::new()), &mut empty);
    assert_eq!(empty, b"Slim -- V0.5\n");
    let mut zero = Vec::new();
    drive(&mut Cursor::new(b"000000:000003:bye".to_vec()), &mut zero);
    assert!(String::from_utf8(zero).unwrap().contains("[000000:]"));
    assert!(read_message_ok("0000000006:hello!"));
    assert!(!read_message_ok("00000000000:"));
    assert_eq!(parse("nope"), Vec::<Value>::new());
    let counted = parse("[000001:000001:a:000001:b:]");
    assert_eq!(counted, vec![t("a")]);
    assert_eq!(parse("[000001:000001:z"), vec![t("z")]);
    let nested = parse(&serialize(&Value::List(vec![Value::List(vec![t("z")])]))) ;
    assert_eq!(nested, vec![Value::List(vec![t("z")])]);
    assert_eq!(read_length(":12", 0).1, 1);
    assert_eq!(read_length("abc:", 0), (0, 4));
    assert_eq!(read_length("xx000001:", 2), (1, 9));
    assert_eq!(read_length("", 0), (0, 0));
    assert_eq!(serialize(&t("world")), "world");
}

fn read_message_ok(raw: &str) -> bool {
    let mut out = Vec::new();
    drive(&mut Cursor::new(format!("{raw}000003:bye").into_bytes()), &mut out);
    String::from_utf8(out).unwrap().contains("[")
}

struct OneByte { raw: Vec<u8>, at: usize, shorted: bool }
impl OneByte {
    fn new(raw: Vec<u8>) -> Self { Self { raw, at: 0, shorted: false } }
}
impl Read for OneByte {
    fn read(&mut self, buf: &mut [u8]) -> std::io::Result<usize> {
        if buf.is_empty() || self.at >= self.raw.len() { return Ok(0); }
        let mut n = buf.len();
        if n > 1 && !self.shorted { self.shorted = true; n = 1; }
        n = n.min(self.raw.len() - self.at);
        buf[..n].copy_from_slice(&self.raw[self.at..self.at + n]);
        self.at += n;
        Ok(n)
    }
}

fn frame(value: &Value) -> String {
    let body = serialize(&Value::List(vec![value.clone()]));
    format!("{:06}:{}", body.len(), body)
}

#[test]
fn a_busy_port_is_refused() {
    let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
    let port = listener.local_addr().unwrap().port() as i32;
    assert!(serve(port).is_err());
}

#[test]
fn socket_shares_a_seat_and_waits() {
    let port = free_port();
    let bound = port;
    let done = thread::spawn(move || serve(bound));
    thread::sleep(Duration::from_millis(300));
    assert!(!done.is_finished());
    let mut first = dial(port);
    assert_eq!(read_until(&mut first, b"\n"), "Slim -- V0.5\n");
    thread::sleep(Duration::from_millis(100));
    assert!(!done.is_finished(), "server left before the client finished");
    write_frame(&mut first, &Value::List(vec![t("1"), t("make"), t("seat"), t("slim"), t("safe")]));
    assert!(read_until(&mut first, b"OK").contains("OK"));
    write_frame(&mut first, &call("2", "seat", "book", &[]));
    assert!(read_until(&mut first, b"booked").contains("booked"));
    write_frame(&mut first, &call("3", "seat", "book", &[]));
    assert!(read_until(&mut first, b"full").contains("full"));
    let _ = first.write_all(b"000003:bye");
    drop(first);
    let finished = done.join().expect("server thread");
    assert!(finished.is_ok());
}

fn free_port() -> i32 {
    let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
    listener.local_addr().unwrap().port() as i32
}

fn dial(port: i32) -> TcpStream {
    for _ in 0..50 {
        if let Ok(stream) = TcpStream::connect(format!("127.0.0.1:{port}")) { return stream; }
        thread::sleep(Duration::from_millis(20));
    }
    panic!("connect");
}

fn write_frame(stream: &mut TcpStream, value: &Value) {
    stream.write_all(frame(value).as_bytes()).unwrap();
}

fn read_until(stream: &mut TcpStream, marker: &[u8]) -> String {
    stream.set_read_timeout(Some(Duration::from_secs(2))).unwrap();
    let mut buf = Vec::new();
    let mut tmp = [0u8; 64];
    while !buf.windows(marker.len()).any(|window| window == marker) {
        let n = stream.read(&mut tmp).unwrap();
        if n == 0 { break; }
        buf.extend_from_slice(&tmp[..n]);
    }
    String::from_utf8(buf).unwrap()
}
