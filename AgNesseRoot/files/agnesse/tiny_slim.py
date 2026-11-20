#!/usr/bin/env python3
"""A Slim 0.5 server small enough to show the wire protocol.

Port 1 is the pipe AgNesse uses when SLIM_PORT is 1: the protocol is stdin and stdout.
Any other port is a socket on 127.0.0.1. hello returns world. echo returns its string
argument. endIsAfterStart returns yes when the second integer is greater than the first.
kind returns id when its argument contains a kind field of id. A make starts a gate at
phase new. open, block, and release move that gate, and release from blocked is refused.
bye ends the session.
"""
import socket
import sys


def main():
    port = int(sys.argv[-1])
    if port == 1:
        speak(sys.stdin.buffer, sys.stdout.buffer)
        return
    server = socket.socket()
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", port))
    server.listen(1)
    connection, _ = server.accept()
    try:
        speak(connection.makefile("rb"), connection.makefile("wb"))
    finally:
        connection.close()
        server.close()


def speak(incoming, outgoing):
    outgoing.write(b"Slim -- V0.5\n")
    outgoing.flush()
    instances = {}
    while True:
        message = read_message(incoming)
        if message is None or message.lower() == "bye":
            return
        payload = serialize(answer(parse(message), instances)).encode("utf-8")
        outgoing.write(f"{len(payload):06d}:".encode("ascii") + payload)
        outgoing.flush()


def answer(instructions, instances=None):
    state = {} if instances is None else instances
    results = []
    for instruction in instructions:
        results.append([instruction[0], reply(instruction, state)])
    return results


def reply(instruction, instances):
    if command(instruction) == "make":
        return made(instruction, instances)
    return invoke(method_name(instruction), instruction, instances)


def invoke(method, instruction, instances):
    plain = PLAIN.get(method)
    if plain is not None:
        return plain(instruction)
    needed = MOVES.get(method)
    if needed is not None:
        return move(instruction, instances, needed[0], needed[1])
    query = QUERIES.get(method)
    if query is not None:
        return query(instruction, instances)
    return "OK"


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
        instances[name] = {"phase": "new"}
    return "OK"


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
