#!/usr/bin/env python3
"""A Slim 0.5 server small enough to show the wire protocol.

Port 1 is the pipe AgNesse uses when SLIM_PORT is 1: the protocol is stdin and stdout.
Any other port is a socket on 127.0.0.1. hello returns world. echo returns its string
argument. endIsAfterStart returns yes when the second integer is greater than the first.
kind returns id when its argument contains a kind field of id. A make starts a gate at
phase new. open, block, and release move that gate, and release from blocked is refused.
bye ends the session. A socket accepts one connection per thread and those connections share instances. book on a safe seat takes it once. book on a racy seat checks, waits, then acts.
"""
import socket
import sys
import threading
import time


def main():
    port = int(sys.argv[-1])
    if port == 1:
        speak(sys.stdin.buffer, sys.stdout.buffer)
        return
    serve(port)


def serve(port):
    server = socket.socket()
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", port))
    server.listen(1)
    try:
        connection, _ = server.accept()
    finally:
        server.close()
    session(connection, {}, lambda: None)


def session(connection, instances, finished):
    try:
        speak(connection.makefile("rb"), connection.makefile("wb"), instances)
    finally:
        connection.close()
        finished()


def speak(incoming, outgoing, instances=None):
    outgoing.write(b"Slim -- V0.5\n")
    outgoing.flush()
    state = {} if instances is None else instances
    while True:
        message = read_message(incoming)
        if message is None or message.lower() == "bye":
            return
        payload = serialize(answer(parse(message), state)).encode("utf-8")
        outgoing.write(f"{len(payload):06d}:".encode("ascii") + payload)
        outgoing.flush()


def answer(instructions, instances=None):
    state = {} if instances is None else instances
    results = []
    for instruction in instructions:
        results.append([instruction[0], effect(instruction, state)])
    return results


def effect(instruction, instances):
    if command(instruction) == "lanes":
        return run_lanes(instruction, instances)
    return reply(instruction, instances)


def run_lanes(instruction, instances):
    if len(instruction) <= 2:
        return "lanes"
    counter = {"n": 0}
    counter_lock = threading.Lock()
    rows = []
    rows_lock = threading.Lock()

    def stamp():
        with counter_lock:
            counter["n"] += 1
            return str(counter["n"])

    def lane(group):
        tag = text_at(group, 0, "")
        for call in group[1]:
            if not isinstance(call, list):
                continue
            started = stamp()
            value = reply(call, instances)
            finished = stamp()
            with rows_lock:
                rows.append([tag, text_at(call, 0, ""), started, finished, value])

    threads = []
    for group in instruction[2:]:
        if not isinstance(group, list):
            continue
        if len(group) < 2:
            continue
        if not isinstance(group[1], list):
            continue
        thread = threading.Thread(target=lane, args=(group,))
        threads.append(thread)
        thread.start()
    for thread in threads:
        thread.join()
    return rows


def reply(instruction, instances):
    kind = command(instruction)
    if kind == "make":
        return made(instruction, instances)
    if kind == "call":
        return invoke(text_at(instruction, 3, ""), instruction, instances)
    if kind == "callAndAssign":
        return invoke(text_at(instruction, 4, ""), instruction, instances)
    return "OK"


def invoke(method, instruction, instances):
    if method == "":
        return "OK"
    plain = PLAIN.get(method)
    if plain is not None:
        return plain(instruction)
    needed = MOVES.get(method)
    if needed is not None:
        return move(instruction, instances, needed[0], needed[1])
    query = QUERIES.get(method)
    if query is not None:
        return query(instruction, instances)
    if method == "book":
        return book(instruction, instances)
    if method == "setWord":
        return set_word(instruction, instances)
    if method in LIFECYCLE:
        return "OK"
    state = gate(instruction, instances)
    if state is None:
        return missing_instance(instruction)
    return missing_method(method, len(arguments(instruction)), state.get("class", "slim"))


def hello(_instruction):
    return "world"


def command(instruction):
    return text_at(instruction, 1, "")


def text_at(instruction, index, fallback):
    if index < len(instruction) and isinstance(instruction[index], str):
        return instruction[index]
    return fallback


def made(instruction, instances):
    name = instance_name(instruction)
    if name:
        instances[name] = opened(instruction)
    return "OK"


def opened(instruction):
    class_name = text_at(instruction, 3, "")
    if class_name == "":
        class_name = "slim"
    mark = text_at(instruction, 4, "")
    if mark == "racy" or mark == "safe":
        return {"seats": 1, "race": mark, "class": class_name}
    return {"phase": "new", "class": class_name}


def book(instruction, instances):
    state = gate(instruction, instances)
    if state is None or "seats" not in state:
        return missing_instance(instruction)
    if state.get("race") == "racy":
        return race_book(state)
    return safe_book(state)


def race_book(state):
    seen = state["seats"]
    if seen <= 0:
        return "full"
    time.sleep(0.05)
    state["seats"] = seen - 1
    return "booked"


def safe_book(state):
    with BOOK:
        if state["seats"] <= 0:
            return "full"
        state["seats"] -= 1
        return "booked"


def missing_instance(instruction):
    return "__EXCEPTION__:message:<<NO_INSTANCE " + instance_name(instruction) + ">>"


def missing_method(method, count, class_name):
    if class_name == "":
        class_name = "slim"
    return (
        "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method "
        + method + "[" + str(count) + "] in class " + class_name
        + ".\n Available methods:\n" + CATALOG + ">>"
    )


def set_word(instruction, instances):
    state = gate(instruction, instances)
    if state is None:
        return "OK"
    values = arguments(instruction)
    if values and isinstance(values[0], str):
        state["word"] = values[0]
    return "OK"


BOOK = threading.Lock()


def move(instruction, instances, needed, nxt):
    state = gate(instruction, instances)
    if state is None or state.get("phase") != needed:
        return "refused"
    state["phase"] = nxt
    return "ok"


def phase(instruction, instances):
    state = gate(instruction, instances)
    if state is None:
        return ""
    return state["phase"]


def released(instruction, instances):
    state = gate(instruction, instances)
    if state is not None and state.get("phase") == "released":
        return "true"
    return "false"


def gate(instruction, instances):
    name = instance_name(instruction)
    if not name:
        return None
    return instances.get(name)


def instance_name(instruction):
    if len(instruction) < 3:
        return ""
    name = instruction[2]
    if not isinstance(name, str) or not name:
        return ""
    return name


def echoed(instruction):
    return text_at(instruction, 4, "OK")


def ordered(instruction):
    values = arguments(instruction)
    if len(values) > 1:
        try:
            if int(values[1]) > int(values[0]):
                return "yes"
        except ValueError:
            return "no"
    return "no"


def kind_of(instruction):
    values = arguments(instruction)
    if values and isinstance(values[0], str) and '"kind":"id"' in values[0]:
        return "id"
    return "no"


def arguments(instruction):
    command = text_at(instruction, 1, "")
    if command == "callAndAssign":
        return instruction[5:]
    if command == "call":
        return instruction[4:]
    return []


def method_name(instruction):
    command = instruction[1]
    if command == "call" and len(instruction) > 3:
        return instruction[3]
    if command == "callAndAssign" and len(instruction) > 4:
        return instruction[4]
    return ""


def parse(text):
    return read_list(text)


def read_list(text):
    count, index = read_length(text, 1)
    items = []
    for _ in range(count):
        length, index = read_length(text, index)
        raw = text[index:index + length]
        index += length + 1
        items.append(read_list(raw) if raw.startswith("[") else raw)
    return items


def read_length(text, index):
    end = text.index(":", index)
    return int(text[index:end]), end + 1


def serialize(value):
    if not isinstance(value, list):
        return str(value)
    return "[" + f"{len(value):06d}:" + "".join(field(part) for part in value) + "]"


def field(value):
    text = serialize(value)
    return f"{len(text):06d}:{text}:"


PLAIN = {
    "hello": hello,
    "echo": echoed,
    "endIsAfterStart": ordered,
    "kind": kind_of,
}

MOVES = {
    "open": ("new", "open"),
    "block": ("open", "blocked"),
    "release": ("open", "released"),
}

QUERIES = {
    "phase": phase,
    "released": released,
}

LIFECYCLE = {"table", "beginTable", "reset", "execute", "endTable"}

CATALOG = "\n".join([
    "beginTable() -> string",
    "block() -> string",
    "book() -> string",
    "echo(string) -> string",
    "endIsAfterStart(string, string) -> string",
    "endTable() -> string",
    "execute() -> string",
    "hello() -> string",
    "kind(string) -> string",
    "open() -> string",
    "phase() -> string",
    "release() -> string",
    "released() -> string",
    "reset() -> string",
    "setWord(string) -> string",
    "table(list) -> string",
])


def read_message(incoming):
    prefix = b""
    while b":" not in prefix:
        byte = incoming.read(1)
        if not byte:
            return None
        prefix += byte
    length = int(prefix[:-1])
    data = b""
    remaining = length
    while remaining != 0:
        chunk = incoming.read(remaining)
        if not chunk:
            return None
        data += chunk
        remaining = remaining - len(chunk)
    return data.decode("utf-8")


if __name__ == "__main__":
    main()
