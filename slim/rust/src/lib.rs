//! A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
//! socket on 127.0.0.1. A lanes instruction runs each lane on its own thread
//! against that connection's instances.
//!
//! String symbols support $Name with Unicode letters, $`backtick names`,
//! shorter-prefix fallback, undefined text left as written, $name= as an empty
//! argument, and nested lists. Stored values are strings, so a whole-cell
//! symbol still passes text. There is no reflection, so typed cell conversion
//! is not applied on the call path.

use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::TcpListener;
use std::sync::atomic::{AtomicU64, Ordering};
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
    class_name: String,
}

struct Shared {
    instances: HashMap<String, Arc<Mutex<Seat>>>,
}

impl Shared {
    fn fresh() -> Self {
        Self { instances: HashMap::new() }
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
    let (mut conn, _) = listener.accept().map_err(|err| err.to_string())?;
    let mut writer = conn.try_clone().map_err(|err| err.to_string())?;
    speak(&mut conn, &mut writer, Arc::new(Mutex::new(Shared::fresh())));
    Ok(())
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
        results.push(Value::List(vec![text_at(row, 0), effect(row, shared, symbols)]));
    }
    Value::List(results)
}

pub fn drive(input: &mut impl Read, output: &mut impl Write) {
    speak(input, output, Arc::new(Mutex::new(Shared::fresh())));
}

fn effect(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, String>) -> Value {
    if text_of(&text_at(row, 1)) == "lanes" {
        return run_lanes(row, shared, symbols);
    }
    Value::Text(reply(row, shared, symbols))
}

fn run_lanes(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, String>) -> Value {
    if row.len() <= 2 {
        return Value::Text("lanes".into());
    }
    let store = Arc::new(Mutex::new(std::mem::take(symbols)));
    let counter = Arc::new(AtomicU64::new(0));
    let rows = Arc::new(Mutex::new(Vec::new()));
    let mut handles = Vec::new();
    for item in row.iter().skip(2) {
        let Value::List(group) = item else { continue };
        if group.len() < 2 {
            continue;
        }
        let Value::List(calls) = &group[1] else { continue };
        let tag = text_of(&text_at(group, 0));
        let calls = calls.clone();
        let shared = Arc::clone(shared);
        let store = Arc::clone(&store);
        let counter = Arc::clone(&counter);
        let rows = Arc::clone(&rows);
        handles.push(thread::spawn(move || {
            for call in calls {
                let Value::List(instruction) = call else { continue };
                let started = counter.fetch_add(1, Ordering::SeqCst) + 1;
                let snapshot = store.lock().unwrap().clone();
                let mut local = snapshot;
                let value = reply(&instruction, &shared, &mut local);
                store.lock().unwrap().extend(local);
                let finished = counter.fetch_add(1, Ordering::SeqCst) + 1;
                rows.lock().unwrap().push(Value::List(vec![
                    Value::Text(tag.clone()),
                    text_at(&instruction, 0),
                    Value::Text(started.to_string()),
                    Value::Text(finished.to_string()),
                    Value::Text(value),
                ]));
            }
        }));
    }
    for handle in handles {
        let _ = handle.join();
    }
    *symbols = store.lock().unwrap().clone();
    let finished = rows.lock().unwrap().clone();
    Value::List(finished)
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
    on_seat(&method, &name, &first_text(&args), args.len(), seat.as_ref())
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

fn on_seat(method: &str, name: &str, arg: &str, count: usize, seat: Option<&Arc<Mutex<Seat>>>) -> String {
    if let Some(moved) = transition(method, seat) {
        return moved;
    }
    asked(method, name, arg, count, seat)
}

fn transition(method: &str, seat: Option<&Arc<Mutex<Seat>>>) -> Option<String> {
    match method {
        "open" => Some(move_phase(seat, "new", "open")),
        "block" => Some(move_phase(seat, "open", "blocked")),
        "release" => Some(move_phase(seat, "open", "released")),
        _ => None,
    }
}

fn asked(method: &str, name: &str, arg: &str, count: usize, seat: Option<&Arc<Mutex<Seat>>>) -> String {
    match method {
        "book" => book(seat, name),
        "phase" => phase_of(seat),
        "released" => released_of(seat),
        "setWord" => store_word(seat, arg),
        "table" | "beginTable" | "reset" | "execute" | "endTable" => "OK".into(),
        _ => unknown(method, name, count, seat),
    }
}

fn unknown(method: &str, name: &str, count: usize, seat: Option<&Arc<Mutex<Seat>>>) -> String {
    let Some(seat) = seat else { return missing_instance(name) };
    let mut class_name = seat.lock().unwrap().class_name.clone();
    if class_name.is_empty() {
        class_name = "slim".into();
    }
    no_method(method, count, &class_name)
}

fn no_method(method: &str, count: usize, class_name: &str) -> String {
    format!("__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method {method}[{count}] in class {class_name}.\n Available methods:\n{CATALOG}>>")
}

fn book(seat: Option<&Arc<Mutex<Seat>>>, name: &str) -> String {
    let Some(seat) = seat else { return missing_instance(name) };
    let race = seat.lock().unwrap().race.clone();
    if race.is_empty() {
        return missing_instance(name);
    }
    if race == "racy" { race_book(seat) } else { safe_book(seat) }
}

fn missing_instance(name: &str) -> String {
    format!("__EXCEPTION__:message:<<NO_INSTANCE {name}>>")
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
    shared.lock().unwrap().instances.insert(name, Arc::new(Mutex::new(opened(&text_of(&text_at(row, 3)), &text_of(&text_at(row, 4))))));
    "OK".into()
}

fn opened(class_name: &str, mark: &str) -> Seat {
    let class_name = if class_name.is_empty() { "slim".into() } else { class_name.to_string() };
    if mark == "racy" || mark == "safe" {
        return Seat { phase: String::new(), seats: Some(1), race: mark.into(), word: String::new(), class_name };
    }
    Seat { phase: "new".into(), seats: None, race: String::new(), word: String::new(), class_name }
}

const CATALOG: &str = "beginTable() -> string\nblock() -> string\nbook() -> string\necho(string) -> string\nendIsAfterStart(string, string) -> string\nendTable() -> string\nexecute() -> string\nhello() -> string\nkind(string) -> string\nopen() -> string\nphase() -> string\nrelease() -> string\nreleased() -> string\nreset() -> string\nsetWord(string) -> string\ntable(list) -> string";

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
    if assignment(text) {
        return String::new();
    }
    let mut out = String::new();
    let mut index = 0;
    while index < text.len() {
        if let Some(found) = symbol_at(text, index) {
            match resolve(&found.name, symbols) {
                Some((used, value)) => {
                    out.push_str(value);
                    index = found.start + 1 + used.len();
                }
                None => {
                    out.push_str(&text[found.start..found.end]);
                    index = found.end;
                }
            }
            continue;
        }
        let ch = text[index..].chars().next().unwrap_or('\u{0}');
        out.push(ch);
        index += ch.len_utf8();
    }
    out
}

struct Found { start: usize, end: usize, name: String }

fn assignment(text: &str) -> bool {
    let trimmed = text.trim();
    let Some(body) = trimmed.strip_prefix('$') else { return false };
    let Some(name) = body.strip_suffix('=') else { return false };
    let name = name.trim();
    let mut chars = name.chars();
    matches!(chars.next(), Some(ch) if ch.is_alphabetic()) && chars.all(name_part)
}

fn symbol_at(text: &str, index: usize) -> Option<Found> {
    let rest = text.get(index..)?;
    if !rest.starts_with('$') {
        return None;
    }
    let after = rest.get(1..)?;
    let start_char = after.chars().next()?;
    if start_char == '`' {
        let close = after[1..].find('`')?;
        let name = after[..=close + 1].to_string();
        return Some(Found { start: index, end: index + 1 + name.len(), name });
    }
    if !start_char.is_alphabetic() {
        return None;
    }
    let mut end = start_char.len_utf8();
    for ch in after[end..].chars() {
        if !name_part(ch) { break }
        end += ch.len_utf8();
    }
    Some(Found { start: index, end: index + 1 + end, name: after[..end].to_string() })
}

fn resolve<'a>(name: &str, symbols: &'a HashMap<String, String>) -> Option<(String, &'a str)> {
    if let Some(value) = symbols.get(name) {
        return Some((name.to_string(), value));
    }
    let mut shorter = name.to_string();
    while shorter.pop().is_some() && !shorter.is_empty() {
        if let Some(value) = symbols.get(&shorter) {
            return Some((shorter, value));
        }
    }
    None
}

fn name_part(ch: char) -> bool {
    ch.is_alphanumeric() || ch == '_'
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
