//! A Slim 0.5 server. Port 1 is stdin and stdout. Any other port is a socket on
//! 127.0.0.1, and those connections share instances.

use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::TcpListener;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

#[derive(Clone, Debug, PartialEq)]
pub enum Value {
    Text(String),
    List(Vec<Value>),
}

struct Seat {
    phase: String,
    seats: Option<i32>,
    race: String,
    word: String,
}

struct Shared {
    instances: HashMap<String, Arc<Mutex<Seat>>>,
    active: i32,
    seen: bool,
}

impl Shared {
    fn fresh() -> Self {
        Self { instances: HashMap::new(), active: 0, seen: false }
    }
}

pub fn status(err: Result<(), String>) -> i32 {
    if err.is_err() { 1 } else { 0 }
}

pub fn run(args: &[String]) -> Result<(), String> {
    if args.is_empty() {
        return Err("port".into());
    }
    let port = args.last().unwrap();
    if port == "1" {
        let stdin = std::io::stdin();
        let stdout = std::io::stdout();
        speak(&mut stdin.lock(), &mut stdout.lock(), Arc::new(Mutex::new(Shared::fresh())));
        return Ok(());
    }
    let number: i32 = port.parse().map_err(|_| "port".to_string())?;
    if rejected(number) {
        return Err("port".into());
    }
    serve(number)
}

pub fn rejected(number: i32) -> bool {
    number < 0
}

pub fn serve(port: i32) -> Result<(), String> {
    let listener = TcpListener::bind(format!("127.0.0.1:{port}")).map_err(|err| err.to_string())?;
    listener.set_nonblocking(true).map_err(|err| err.to_string())?;
    let shared = Arc::new(Mutex::new(Shared::fresh()));
    loop {
        match listener.accept() {
            Ok((mut conn, _)) => {
                {
                    let mut gate = shared.lock().unwrap();
                    gate.active += 1;
                    gate.seen = true;
                }
                let gate = Arc::clone(&shared);
                thread::spawn(move || {
                    let mut writer = conn.try_clone().unwrap();
                    speak(&mut conn, &mut writer, Arc::clone(&gate));
                    gate.lock().unwrap().active -= 1;
                });
            }
            Err(err) if err.kind() == std::io::ErrorKind::WouldBlock => {
                let idle = {
                    let gate = shared.lock().unwrap();
                    gate.seen && gate.active == 0
                };
                if idle {
                    return Ok(());
                }
                thread::sleep(Duration::from_millis(20));
            }
            Err(_) => return Ok(()),
        }
    }
}

fn speak<R: Read, W: Write>(input: &mut R, output: &mut W, shared: Arc<Mutex<Shared>>) {
    let _ = output.write_all(b"Slim -- V0.5\n");
    let _ = output.flush();
    let mut symbols = HashMap::new();
    loop {
        let Some(message) = read_message(input) else { return };
        if message.eq_ignore_ascii_case("bye") {
            return;
        }
        let body = serialize(&answer(&parse(&message), &shared, &mut symbols));
        let _ = write!(output, "{:06}:{}", body.len(), body);
        let _ = output.flush();
    }
}

fn read_message<R: Read>(input: &mut R) -> Option<String> {
    let prefix = read_prefix(input)?;
    let length: i32 = prefix.parse().ok()?;
    if length < 0 {
        return None;
    }
    read_exact(input, length as usize)
}

fn read_prefix<R: Read>(input: &mut R) -> Option<String> {
    let mut prefix = Vec::new();
    let mut buf = [0u8; 1];
    while prefix.len() <= 10 {
        let n = input.read(&mut buf).ok()?;
        if n == 0 {
            return None;
        }
        if buf[0] == b':' {
            return Some(String::from_utf8_lossy(&prefix).into_owned());
        }
        prefix.push(buf[0]);
    }
    None
}

fn read_exact<R: Read>(input: &mut R, length: usize) -> Option<String> {
    let mut data = vec![0u8; length];
    let mut got = 0;
    while got < length {
        let n = input.read(&mut data[got..]).ok()?;
        if n == 0 {
            return None;
        }
        got += n;
    }
    Some(String::from_utf8_lossy(&data).into_owned())
}

pub struct Gate {
    shared: Arc<Mutex<Shared>>,
    symbols: Mutex<HashMap<String, String>>,
}

impl Gate {
    pub fn new() -> Self {
        Self { shared: Arc::new(Mutex::new(Shared::fresh())), symbols: Mutex::new(HashMap::new()) }
    }

    pub fn answer(&self, instructions: &[Value]) -> Value {
        let mut symbols = self.symbols.lock().unwrap().clone();
        let value = answer(instructions, &self.shared, &mut symbols);
        *self.symbols.lock().unwrap() = symbols;
        value
    }
}

fn answer(instructions: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, String>) -> Value {
    let mut results = Vec::new();
    for item in instructions {
        let Value::List(row) = item else { continue };
        results.push(Value::List(vec![text_at(row, 0), Value::Text(reply(row, shared, symbols))]));
    }
    Value::List(results)
}

pub fn drive(input: &mut impl Read, output: &mut impl Write) {
    speak(input, output, Arc::new(Mutex::new(Shared::fresh())));
}

fn reply(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, String>) -> String {
    match text_of(&text_at(row, 1)).as_str() {
        "make" => made(row, shared),
        "call" => call(row, 2, 3, shared, symbols),
        "callAndAssign" => {
            let value = call(row, 3, 4, shared, symbols);
            remember(symbols, &text_of(&text_at(row, 2)), &value);
            value
        }
        _ => "OK".into(),
    }
}

fn remember(symbols: &mut HashMap<String, String>, name: &str, value: &str) {
    if name.is_empty() {
        return;
    }
    symbols.insert(name.into(), value.into());
}

fn call(row: &[Value], instance_at: usize, method_at: usize, shared: &Arc<Mutex<Shared>>, symbols: &HashMap<String, String>) -> String {
    let method = text_of(&text_at(row, method_at));
    let args = expand_all(&args_from(row, method_at + 1), symbols);
    let name = text_of(&text_at(row, instance_at));
    let seat = find(shared, &name);
    if method == "echo" {
        return echo_of(seat.as_ref(), &args);
    }
    if let Some(value) = plain(&method, &args) {
        return value;
    }
    on_seat(&method, &name, &first_text(&args), seat.as_ref())
}

fn plain(method: &str, args: &[Value]) -> Option<String> {
    match method {
        "hello" => Some("world".into()),
        "endIsAfterStart" => Some(ordered(args)),
        "kind" => Some(kind_of(args)),
        _ => None,
    }
}

fn echo_of(seat: Option<&Arc<Mutex<Seat>>>, args: &[Value]) -> String {
    if args.is_empty() {
        return word_of(seat);
    }
    match &args[0] {
        Value::Text(text) => text.clone(),
        Value::List(_) => "OK".into(),
    }
}

fn ordered(args: &[Value]) -> String {
    if args.len() < 2 {
        return "no".into();
    }
    match (whole(&args[0]), whole(&args[1])) {
        (Some(left), Some(right)) if right > left => "yes".into(),
        _ => "no".into(),
    }
}

pub fn whole(value: &Value) -> Option<i32> {
    let Value::Text(text) = value else { return None };
    text.parse().ok()
}

fn kind_of(args: &[Value]) -> String {
    if first_text(args).contains("\"kind\":\"id\"") { "id".into() } else { "no".into() }
}

fn first_text(args: &[Value]) -> String {
    if args.is_empty() {
        return String::new();
    }
    match &args[0] {
        Value::Text(text) => text.clone(),
        Value::List(_) => String::new(),
    }
}

fn on_seat(method: &str, name: &str, arg: &str, seat: Option<&Arc<Mutex<Seat>>>) -> String {
    if let Some(moved) = transition(method, seat) {
        return moved;
    }
    asked(method, name, arg, seat)
}

fn transition(method: &str, seat: Option<&Arc<Mutex<Seat>>>) -> Option<String> {
    match method {
        "open" => Some(move_phase(seat, "new", "open")),
        "block" => Some(move_phase(seat, "open", "blocked")),
        "release" => Some(move_phase(seat, "open", "released")),
        _ => None,
    }
}

fn asked(method: &str, name: &str, arg: &str, seat: Option<&Arc<Mutex<Seat>>>) -> String {
    match method {
        "book" => book(seat, name),
        "__agnesse_lane_probe__" => probe(seat, name),
        "phase" => phase_of(seat),
        "released" => released_of(seat),
        "setWord" => store_word(seat, arg),
        _ => "OK".into(),
    }
}

fn book(seat: Option<&Arc<Mutex<Seat>>>, name: &str) -> String {
    let Some(seat) = seat else { return missing_instance(name) };
    let race = seat.lock().unwrap().race.clone();
    if race.is_empty() {
        return missing_instance(name);
    }
    if race == "racy" { race_book(seat) } else { safe_book(seat) }
}

fn probe(seat: Option<&Arc<Mutex<Seat>>>, name: &str) -> String {
    if seat.is_none() { missing_instance(name) } else { missing_method("__agnesse_lane_probe__") }
}

fn missing_instance(name: &str) -> String {
    format!("__EXCEPTION__:message:<<NO_INSTANCE {name}>>")
}

fn missing_method(method: &str) -> String {
    format!("__EXCEPTION__:message:<<NO_METHOD_IN_CLASS {method}>>")
}

fn move_phase(seat: Option<&Arc<Mutex<Seat>>>, needed: &str, next: &str) -> String {
    let Some(seat) = seat else { return "refused".into() };
    let mut seat = seat.lock().unwrap();
    if seat.phase != needed {
        return "refused".into();
    }
    seat.phase = next.into();
    "ok".into()
}

fn phase_of(seat: Option<&Arc<Mutex<Seat>>>) -> String {
    seat.map(|seat| seat.lock().unwrap().phase.clone()).unwrap_or_default()
}

fn released_of(seat: Option<&Arc<Mutex<Seat>>>) -> String {
    if phase_of(seat) == "released" { "true".into() } else { "false".into() }
}

fn store_word(seat: Option<&Arc<Mutex<Seat>>>, word: &str) -> String {
    let Some(seat) = seat else { return "OK".into() };
    seat.lock().unwrap().word = word.into();
    "OK".into()
}

fn word_of(seat: Option<&Arc<Mutex<Seat>>>) -> String {
    let Some(seat) = seat else { return "OK".into() };
    let word = seat.lock().unwrap().word.clone();
    if word.is_empty() { "OK".into() } else { word }
}

fn made(row: &[Value], shared: &Arc<Mutex<Shared>>) -> String {
    let name = text_of(&text_at(row, 2));
    if name.is_empty() {
        return "OK".into();
    }
    shared.lock().unwrap().instances.insert(name, Arc::new(Mutex::new(opened(&text_of(&text_at(row, 4))))));
    "OK".into()
}

fn opened(mark: &str) -> Seat {
    if mark == "racy" || mark == "safe" {
        return Seat { phase: String::new(), seats: Some(1), race: mark.into(), word: String::new() };
    }
    Seat { phase: "new".into(), seats: None, race: String::new(), word: String::new() }
}

fn race_book(seat: &Arc<Mutex<Seat>>) -> String {
    let seen = match seat.lock().unwrap().seats {
        Some(n) => n,
        None => return "full".into(),
    };
    if seen <= 0 {
        return "full".into();
    }
    thread::sleep(Duration::from_millis(50));
    seat.lock().unwrap().seats = Some(seen - 1);
    "booked".into()
}

fn safe_book(seat: &Arc<Mutex<Seat>>) -> String {
    let mut seat = seat.lock().unwrap();
    let Some(seen) = seat.seats else { return "full".into() };
    if seen <= 0 {
        return "full".into();
    }
    seat.seats = Some(seen - 1);
    "booked".into()
}

fn find(shared: &Arc<Mutex<Shared>>, name: &str) -> Option<Arc<Mutex<Seat>>> {
    if name.is_empty() {
        return None;
    }
    shared.lock().unwrap().instances.get(name).cloned()
}

fn text_at(row: &[Value], index: usize) -> Value {
    row.get(index).cloned().unwrap_or(Value::Text(String::new()))
}

fn text_of(value: &Value) -> String {
    match value {
        Value::Text(text) => text.clone(),
        Value::List(_) => String::new(),
    }
}

fn args_from(row: &[Value], index: usize) -> Vec<Value> {
    row.get(index..).unwrap_or(&[]).to_vec()
}

fn expand_all(args: &[Value], symbols: &HashMap<String, String>) -> Vec<Value> {
    args.iter().map(|arg| expand(arg, symbols)).collect()
}

fn expand(value: &Value, symbols: &HashMap<String, String>) -> Value {
    match value {
        Value::Text(text) => Value::Text(expand_string(text, symbols)),
        Value::List(items) => Value::List(expand_all(items, symbols)),
    }
}

pub fn expand_string(text: &str, symbols: &HashMap<String, String>) -> String {
    let bytes = text.as_bytes();
    let mut out = String::new();
    let mut i = 0;
    while i < bytes.len() {
        if let Some((name, next)) = symbol_at(bytes, i) {
            if let Some(value) = symbols.get(name) {
                out.push_str(value);
            } else {
                out.push_str(&text[i..next]);
            }
            i = next;
            continue;
        }
        out.push(bytes[i] as char);
        i += 1;
    }
    out
}

fn symbol_at(text: &[u8], i: usize) -> Option<(&str, usize)> {
    if text[i] != b'$' || i + 1 >= text.len() || !name_start(text[i + 1]) {
        return None;
    }
    let mut j = i + 2;
    while j < text.len() && name_part(text[j]) {
        j += 1;
    }
    Some((std::str::from_utf8(&text[i + 1..j]).unwrap_or(""), j))
}

fn name_start(c: u8) -> bool {
    c.is_ascii_uppercase() || c.is_ascii_lowercase()
}

fn name_part(c: u8) -> bool {
    name_start(c) || c.is_ascii_digit() || c == b'_'
}

pub fn parse(text: &str) -> Vec<Value> {
    read_list(text)
}

fn read_list(text: &str) -> Vec<Value> {
    if !text.starts_with('[') {
        return Vec::new();
    }
    let (count, mut index) = read_length(text, 1);
    let mut items = Vec::new();
    for _ in 0..count {
        let Some((raw, next)) = read_field(text, index) else { break };
        index = next;
        items.push(field_value(&raw));
    }
    items
}

fn field_value(raw: &str) -> Value {
    if raw.starts_with('[') {
        return Value::List(read_list(raw));
    }
    Value::Text(raw.into())
}

fn read_field(text: &str, index: usize) -> Option<(String, usize)> {
    let (length, next) = read_length(text, index);
    if next + length > text.len() {
        return None;
    }
    Some((text[next..next + length].into(), next + length + 1))
}

pub fn read_length(text: &str, index: usize) -> (usize, usize) {
    let rest = text.get(index..).unwrap_or("");
    let Some(end) = rest.find(':') else { return (0, text.len()) };
    let Ok(number) = rest[..end].parse::<usize>() else {
        return (0, index + end + 1);
    };
    (number, index + end + 1)
}

pub fn serialize(value: &Value) -> String {
    let Value::List(list) = value else { return text_of(value) };
    let mut body = format!("[{:06}:", list.len());
    for part in list {
        let text = serialize(part);
        body.push_str(&format!("{:06}:{}:", text.len(), text));
    }
    body.push(']');
    body
}
