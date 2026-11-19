#!/usr/bin/env python3
"""A Slim 0.5 server small enough to show the wire protocol.

Port 1 is the pipe AgNesse uses when SLIM_PORT is 1: the protocol is stdin and stdout.
Any other port is a socket on 127.0.0.1. hello returns world. echo returns its string
argument. bye ends the session.
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
    while True:
        message = read_message(incoming)
        if message is None or message.lower() == "bye":
            return
        payload = serialize(answer(parse(message))).encode("utf-8")
        outgoing.write(f"{len(payload):06d}:".encode("ascii") + payload)
        outgoing.flush()


def answer(instructions):
    results = []
    for instruction in instructions:
        results.append([instruction[0], reply(instruction)])
    return results


def reply(instruction):
    method = method_name(instruction)
    if method == "hello":
        return "world"
    if method == "echo":
        return echoed(instruction)
    return "OK"


def echoed(instruction):
    if len(instruction) > 4 and isinstance(instruction[4], str):
        return instruction[4]
    return "OK"


def method_name(instruction):
    command = instruction[1]
    if command == "call" and len(instruction) > 3:
        return instruction[3]
    if command == "callAndAssign" and len(instruction) > 4:
        return instruction[4]
    return ""


def parse(text):
    value, _ = read_list(text, 0)
    return value


def read_list(text, index):
    index += 1
    count, index = read_length(text, index)
    items = []
    for _ in range(count):
        length, index = read_length(text, index)
        raw = text[index:index + length]
        index += length + 1
        items.append(read_list(raw, 0)[0] if raw.startswith("[") else raw)
    if index < len(text) and text[index] == "]":
        index += 1
    return items, index


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
