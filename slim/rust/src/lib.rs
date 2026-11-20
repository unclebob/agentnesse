//! A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
//! socket on 127.0.0.1. A lanes instruction runs each lane on its own thread
//! against that connection's instances.
//!
//! Symbols follow the Java forms. A whole-cell symbol passes the stored value.
//! A non-string class symbol becomes the instance. SlimHelperLibrary pushes,
//! pops, and clones the script actor. SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS skips
//! replacement, and an invalid pattern is ignored. plus, wide, scale, flag,
//! initial, day, shade, join, and label convert arguments from a signature
//! table, because a fixture method has no reflected parameter types.

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
    stack: Vec<Option<Arc<Mutex<Seat>>>>,
}

#[derive(Clone)]
enum Stored {
    Text(String),
    Int(i64),
    Float(f64),
    Bool(bool),
    Char(char),
    List(Vec<Stored>),
    Seat(Arc<Mutex<Seat>>),
}

impl Shared {
    fn fresh() -> Self {
        Self { instances: HashMap::new(), stack: Vec::new() }
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
    symbols: Mutex<HashMap<String, Stored>>,
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

fn answer(instructions: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, Stored>) -> Value {
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

fn effect(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, Stored>) -> Value {
    if text_of(&text_at(row, 1)) == "lanes" {
        return run_lanes(row, shared, symbols);
    }
    wire(&reply(row, shared, symbols))
}

fn run_lanes(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, Stored>) -> Value {
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
                    wire(&value),
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

fn reply(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &mut HashMap<String, Stored>) -> Stored {
    match text_of(&text_at(row, 1)).as_str() {
        "make" => Stored::Text(made(row, shared, symbols)),
        "call" => call(row, 2, 3, shared, symbols),
        "callAndAssign" => {
            let value = call(row, 3, 4, shared, symbols);
            remember(symbols, &text_of(&text_at(row, 2)), value.clone());
            value
        }
        "assign" => {
            remember(symbols, &text_of(&text_at(row, 2)), Stored::Text(text_of(&text_at(row, 3))));
            Stored::Text("OK".into())
        }
        _ => Stored::Text("OK".into()),
    }
}

fn remember(symbols: &mut HashMap<String, Stored>, name: &str, value: Stored) {
    if name.is_empty() {
        return;
    }
    symbols.insert(name.into(), value);
}

fn call(row: &[Value], instance_at: usize, method_at: usize, shared: &Arc<Mutex<Shared>>, symbols: &HashMap<String, Stored>) -> Stored {
    let method = text_of(&text_at(row, method_at));
    let name = text_of(&text_at(row, instance_at));
    let raw = args_from(row, method_at + 1);
    let args = if skips(symbols, &name, &method) {
        raw.iter().map(stored_of).collect()
    } else {
        expand_all(&raw, symbols)
    };
    let args = convert_args(&method, args);
    if name == "SlimHelperLibrary" {
        return helper(shared, &method, &args);
    }
    let seat = find(shared, &name);
    if method == "echo" {
        return Stored::Text(echo_of(seat.as_ref(), &args));
    }
    if method == "self" {
        if let Some(seat) = seat {
            return Stored::Seat(seat);
        }
    }
    if let Some(value) = plain(&method, &args) {
        return Stored::Text(value);
    }
    Stored::Text(on_seat(&method, &name, &first_text(&args), args.len(), seat.as_ref()))
}

fn plain(method: &str, args: &[Stored]) -> Option<String> {
    numbers(method, args).or_else(|| words(method, args))
}

fn numbers(method: &str, args: &[Stored]) -> Option<String> {
    match method {
        "plus" => Some(plus(args)),
        "wide" => Some(as_shown(args, Show::Wide)),
        "scale" => Some(as_shown(args, Show::Scale)),
        "flag" => Some(flag(args)),
        _ => None,
    }
}

fn words(method: &str, args: &[Stored]) -> Option<String> {
    greetings(method, args).or_else(|| shaped(method, args))
}

fn greetings(method: &str, args: &[Stored]) -> Option<String> {
    match method {
        "hello" => Some("world".into()),
        "endIsAfterStart" => Some(ordered(args)),
        "kind" => Some(kind_of(args)),
        "initial" => Some(as_shown(args, Show::Initial)),
        _ => None,
    }
}

fn shaped(method: &str, args: &[Stored]) -> Option<String> {
    match method {
        "day" => Some(day(args)),
        "shade" => Some(shade(args)),
        "join" => Some(join(args)),
        "label" => Some(label(args)),
        _ => None,
    }
}

fn echo_of(seat: Option<&Arc<Mutex<Seat>>>, args: &[Stored]) -> String {
    if args.is_empty() {
        return word_of(seat);
    }
    match &args[0] {
        Stored::Text(text) => text.clone(),
        _ => "OK".into(),
    }
}

fn ordered(args: &[Stored]) -> String {
    if args.len() < 2 {
        return "no".into();
    }
    match (whole_text(&arg_text(args, 0)), whole_text(&arg_text(args, 1))) {
        (Some(left), Some(right)) if right > left => "yes".into(),
        _ => "no".into(),
    }
}

pub fn whole(value: &Value) -> Option<i32> {
    let Value::Text(text) = value else { return None };
    text.parse().ok()
}

fn kind_of(args: &[Stored]) -> String {
    if arg_text(args, 0).contains("\"kind\":\"id\"") { "id".into() } else { "no".into() }
}

fn first_text(args: &[Stored]) -> String {
    arg_text(args, 0)
}

fn arg_text(args: &[Stored], index: usize) -> String {
    match args.get(index) {
        Some(Stored::Text(text)) => text.clone(),
        Some(other) => display(other),
        None => String::new(),
    }
}

fn whole_text(text: &str) -> Option<i32> {
    text.parse().ok()
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
    let list = if class_name == "SlimHelperLibrary" { HELPER } else { CATALOG };
    format!("__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method {method}[{count}] in class {class_name}.\n Available methods:\n{list}>>")
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

fn made(row: &[Value], shared: &Arc<Mutex<Shared>>, symbols: &HashMap<String, Stored>) -> String {
    let name = text_of(&text_at(row, 2));
    if name.is_empty() {
        return "OK".into();
    }
    if let Some(seat) = actor_from(&text_at(row, 3), symbols) {
        shared.lock().unwrap().instances.insert(name, seat);
        return "OK".into();
    }
    let class_name = display(&expand_cell(&text_of(&text_at(row, 3)), symbols));
    shared.lock().unwrap().instances.insert(name, Arc::new(Mutex::new(opened(&class_name, &text_of(&text_at(row, 4))))));
    "OK".into()
}

fn actor_from(raw: &Value, symbols: &HashMap<String, Stored>) -> Option<Arc<Mutex<Seat>>> {
    let Value::Text(text) = raw else { return None };
    let name = text.strip_prefix('$')?;
    if name.is_empty() {
        return None;
    }
    match symbols.get(name) {
        Some(Stored::Seat(seat)) => Some(Arc::clone(seat)),
        _ => None,
    }
}

fn opened(class_name: &str, mark: &str) -> Seat {
    let class_name = if class_name.is_empty() { "slim".into() } else { class_name.to_string() };
    if mark == "racy" || mark == "safe" {
        return Seat { phase: String::new(), seats: Some(1), race: mark.into(), word: String::new(), class_name };
    }
    Seat { phase: "new".into(), seats: None, race: String::new(), word: String::new(), class_name }
}

const CATALOG: &str = "beginTable() -> string\nblock() -> string\nbook() -> string\nday(date) -> string\necho(string) -> string\nendIsAfterStart(string, string) -> string\nendTable() -> string\nexecute() -> string\nflag(boolean) -> string\nhello() -> string\ninitial(char) -> string\njoin(list) -> string\nkind(string) -> string\nlabel(map) -> string\nopen() -> string\nphase() -> string\nplus(int, int) -> string\nrelease() -> string\nreleased() -> string\nreset() -> string\nscale(double) -> string\nsetWord(string) -> string\nshade(enum) -> string\ntable(list) -> string\nwide(long) -> string";
const HELPER: &str = "cloneSymbol(object) -> object\npopFixture() -> string\npushFixture() -> string";

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

fn expand_all(args: &[Value], symbols: &HashMap<String, Stored>) -> Vec<Stored> {
    args.iter().map(|arg| expand(arg, symbols)).collect()
}

fn expand(value: &Value, symbols: &HashMap<String, Stored>) -> Stored {
    match value {
        Value::Text(text) => expand_cell(text, symbols),
        Value::List(items) => Stored::List(expand_all(items, symbols)),
    }
}

pub fn expand_string(text: &str, symbols: &HashMap<String, String>) -> String {
    let stored = symbols.iter().map(|(key, value)| (key.clone(), Stored::Text(value.clone()))).collect();
    display(&expand_cell(text, &stored))
}

fn expand_cell(text: &str, symbols: &HashMap<String, Stored>) -> Stored {
    if let Some(name) = text.strip_prefix('$') {
        if !name.is_empty() {
            if let Some(value) = symbols.get(name) {
                return value.clone();
            }
        }
    }
    if assignment(text) {
        return Stored::Text(String::new());
    }
    Stored::Text(substitute(text, symbols))
}

fn substitute(text: &str, symbols: &HashMap<String, Stored>) -> String {
    let mut out = String::new();
    let mut index = 0;
    while index < text.len() {
        if let Some(found) = symbol_at(text, index) {
            match resolve(&found.name, symbols) {
                Some((used, value)) => {
                    out.push_str(&display(value));
                    index = if used == found.name { found.end } else { found.start + 1 + used.len() };
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
        return backtick(after, index);
    }
    word_symbol(after, start_char, index)
}

fn backtick(after: &str, index: usize) -> Option<Found> {
    let close = after[1..].find('`')?;
    let name = after[..=close + 1].to_string();
    Some(Found { start: index, end: index + 1 + name.len(), name })
}

fn word_symbol(after: &str, start_char: char, index: usize) -> Option<Found> {
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

fn resolve<'a>(name: &str, symbols: &'a HashMap<String, Stored>) -> Option<(String, &'a Stored)> {
    if let Some(value) = symbols.get(name) {
        return Some((name.to_string(), value));
    }
    shorter(name, symbols)
}

fn shorter<'a>(name: &str, symbols: &'a HashMap<String, Stored>) -> Option<(String, &'a Stored)> {
    if name.len() <= 1 {
        return None;
    }
    let cut = &name[..name.len() - 1];
    if let Some(value) = symbols.get(cut) {
        return Some((cut.to_string(), value));
    }
    shorter(cut, symbols)
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

fn wire(value: &Stored) -> Value {
    match value {
        Stored::List(items) => Value::List(items.iter().map(wire).collect()),
        other => Value::Text(display(other)),
    }
}

fn display(value: &Stored) -> String {
    match value {
        Stored::Text(text) => text.clone(),
        Stored::Int(number) => number.to_string(),
        Stored::Float(number) => number.to_string(),
        Stored::Bool(flag) => flag.to_string(),
        Stored::Char(ch) => ch.to_string(),
        Stored::List(items) => listed(items),
        Stored::Seat(_) => "seat".into(),
    }
}

fn listed(items: &[Stored]) -> String {
    let parts: Vec<String> = items.iter().map(display).collect();
    format!("[{}]", parts.join(" "))
}

fn stored_of(value: &Value) -> Stored {
    match value {
        Value::Text(text) => Stored::Text(text.clone()),
        Value::List(items) => Stored::List(items.iter().map(stored_of).collect()),
    }
}

fn helper(shared: &Arc<Mutex<Shared>>, method: &str, args: &[Stored]) -> Stored {
    match method {
        "pushFixture" => {
            let mut shared = shared.lock().unwrap();
            let actor = shared.instances.get("scriptTableActor").cloned();
            shared.stack.push(actor);
            Stored::Text("OK".into())
        }
        "popFixture" => pop_fixture(shared),
        "cloneSymbol" => args.first().cloned().unwrap_or(Stored::Text(String::new())),
        _ => Stored::Text(no_method(method, args.len(), "SlimHelperLibrary")),
    }
}

fn pop_fixture(shared: &Arc<Mutex<Shared>>) -> Stored {
    let mut shared = shared.lock().unwrap();
    let Some(actor) = shared.stack.pop() else {
        return Stored::Text(missing_instance("scriptTableActor"));
    };
    match actor {
        Some(seat) => { shared.instances.insert("scriptTableActor".into(), seat); }
        None => { shared.instances.remove("scriptTableActor"); }
    }
    Stored::Text("OK".into())
}

fn skips(symbols: &HashMap<String, Stored>, instance: &str, method: &str) -> bool {
    let Some(Stored::Text(pattern)) = symbols.get("SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS") else { return false };
    let Some(literal) = unescape(pattern) else { return false };
    let target = format!("{instance}.{method}");
    if let Some(prefix) = literal.strip_suffix(".*") {
        return target.starts_with(prefix);
    }
    target == literal
}

fn unescape(pattern: &str) -> Option<String> {
    if pattern.contains('[') {
        return None;
    }
    let mut out = String::new();
    let mut chars = pattern.chars();
    while let Some(ch) = chars.next() {
        if ch == '\\' {
            out.push(chars.next()?);
        } else {
            out.push(ch);
        }
    }
    Some(out)
}

fn convert_args(method: &str, args: Vec<Stored>) -> Vec<Stored> {
    let Some(kinds) = signature(method) else { return args };
    args.into_iter().enumerate().map(|(index, arg)| {
        let kind = kinds.get(index).copied().unwrap_or("string");
        convert(arg, kind)
    }).collect()
}

fn signature(method: &str) -> Option<&'static [&'static str]> {
    number_signature(method).or_else(|| word_signature(method))
}

fn number_signature(method: &str) -> Option<&'static [&'static str]> {
    match method {
        "plus" => Some(&["int", "int"]),
        "wide" => Some(&["long"]),
        "scale" => Some(&["double"]),
        "flag" => Some(&["boolean"]),
        _ => None,
    }
}

fn word_signature(method: &str) -> Option<&'static [&'static str]> {
    match method {
        "initial" => Some(&["char"]),
        "day" => Some(&["date"]),
        "shade" => Some(&["enum"]),
        "join" => Some(&["list"]),
        "label" => Some(&["map"]),
        _ => None,
    }
}

fn convert(value: Stored, kind: &str) -> Stored {
    let Stored::Text(text) = value else { return value };
    match kind {
        "int" | "long" => parse_int(&text),
        "double" => parse_float(&text),
        "boolean" => Stored::Bool(text == "true"),
        "char" => parse_char(&text),
        "list" => parse_list(&text),
        _ => Stored::Text(text),
    }
}

fn parse_int(text: &str) -> Stored {
    match text.parse::<i64>() {
        Ok(number) => Stored::Int(number),
        Err(_) => Stored::Text(text.into()),
    }
}

fn parse_float(text: &str) -> Stored {
    match text.parse::<f64>() {
        Ok(number) => Stored::Float(number),
        Err(_) => Stored::Text(text.into()),
    }
}

fn parse_char(text: &str) -> Stored {
    let mut chars = text.chars();
    match (chars.next(), chars.next()) {
        (Some(ch), None) => Stored::Char(ch),
        _ => Stored::Text(text.into()),
    }
}

fn parse_list(text: &str) -> Stored {
    if !text.starts_with('[') || !text.ends_with(']') {
        return Stored::Text(text.into());
    }
    let body = text[1..text.len() - 1].trim();
    if body.is_empty() {
        return Stored::List(Vec::new());
    }
    Stored::List(body.split(',').map(|part| Stored::Text(part.trim().into())).collect())
}

fn plus(args: &[Stored]) -> String {
    match (args.first(), args.get(1)) {
        (Some(Stored::Int(left)), Some(Stored::Int(right))) => (left + right).to_string(),
        _ => "no".into(),
    }
}

enum Show { Wide, Scale, Initial }

fn as_shown(args: &[Stored], show: Show) -> String {
    let Some(value) = args.first() else {
        return "no".into();
    };
    match (show, value) {
        (Show::Wide, Stored::Int(number)) => number.to_string(),
        (Show::Scale, Stored::Float(number)) => number.to_string(),
        (Show::Initial, Stored::Char(ch)) => ch.to_string(),
        _ => "no".into(),
    }
}

fn flag(args: &[Stored]) -> String {
    if matches!(args.first(), Some(Stored::Bool(true))) { "yes".into() } else { "no".into() }
}

fn day(args: &[Stored]) -> String {
    match args.first() {
        Some(Stored::Text(text)) if valid_date(text) => text.clone(),
        _ => "no".into(),
    }
}

fn valid_date(text: &str) -> bool {
    let bytes = text.as_bytes();
    if bytes.len() != 10 {
        return false;
    }
    if bytes[4] != b'-' {
        return false;
    }
    if bytes[7] != b'-' {
        return false;
    }
    let Ok(month) = text[5..7].parse::<u8>() else { return false };
    let Ok(day) = text[8..10].parse::<u8>() else { return false };
    let Some(limit) = month_limit(month) else { return false };
    day >= 1 && day <= limit
}

fn month_limit(month: u8) -> Option<u8> {
    match month {
        1 | 3 | 5 | 7 | 8 | 10 | 12 => Some(31),
        4 | 6 | 9 | 11 => Some(30),
        2 => Some(28),
        _ => None,
    }
}

fn shade(args: &[Stored]) -> String {
    match arg_text(args, 0).as_str() {
        "red" | "green" | "blue" => arg_text(args, 0),
        _ => "no".into(),
    }
}

fn join(args: &[Stored]) -> String {
    let Some(Stored::List(items)) = args.first() else { return "no".into() };
    items.iter().map(display).collect::<Vec<_>>().join(",")
}

fn label(args: &[Stored]) -> String {
    let Some(Stored::List(rows)) = args.first() else { return "no".into() };
    for row in rows {
        if let Stored::List(pair) = row {
            if pair.first().map(display).as_deref() == Some("name") {
                return pair.get(1).map(display).unwrap_or_default();
            }
        }
    }
    "no".into()
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
