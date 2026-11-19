"""The guide's Slim script answers hello, echo returns its string, and a short instruction is not a call."""
import importlib.util
import io
import socket
import sys
import threading
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]


def locate():
    """The copy in the working directory wins, so a mutation worker is the one under test."""
    candidate = Path.cwd() / "AgNesseRoot" / "files" / "agnesse" / "tiny_slim.py"
    if candidate.is_file():
        return candidate
    return ROOT / "AgNesseRoot" / "files" / "agnesse" / "tiny_slim.py"


SCRIPT = locate()


def load():
    spec = importlib.util.spec_from_file_location("tiny_slim", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class TinySlimTest(unittest.TestCase):
    def setUp(self):
        self.slim = load()

    def test_hello_and_the_other_calls(self):
        instructions = [
            ["1", "call", "actor", "hello"],
            ["2", "call", "actor", "echo", "hi"],
            ["3", "callAndAssign", "symbol", "actor", "hello"],
            ["4", "call", "actor"],
            ["5", "make", "instance", "Division"],
            ["6", "call", "actor", "echo", ["a", "b"]],
        ]
        incoming = b"".join(frame(self.slim, row) for row in instructions) + frame(self.slim, "bye")
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(incoming), outgoing)
        text = outgoing.getvalue().decode("utf-8")
        self.assertTrue(text.startswith("Slim -- V0.5\n"))
        self.assertIn("world", text)
        self.assertIn("OK", text)
        self.assertEqual("world", self.slim.answer([instructions[0]])[0][1])
        self.assertEqual("hi", self.slim.answer([instructions[1]])[0][1])
        self.assertEqual("OK", self.slim.answer([instructions[5]])[0][1])
        self.assertEqual("OK", self.slim.answer([["7", "call", "actor", "echo"]])[0][1])

    def test_pipe_mode_reads_the_last_argument(self):
        incoming = frame(self.slim, ["1", "call", "actor", "hello"]) + frame(self.slim, "bye")
        stdout = io.BytesIO()
        drive_main(self, ["tiny_slim.py", "not-a-port", "1"], io.BytesIO(incoming), stdout)
        self.assertIn(b"world", stdout.getvalue())

    def test_socket_mode_answers_on_the_given_port(self):
        probe = socket.socket()
        probe.bind(("127.0.0.1", 0))
        port = probe.getsockname()[1]
        probe.close()
        stdout = io.BytesIO()
        thread = threading.Thread(
            target=drive_main,
            args=(self, ["tiny_slim.py", "not-a-port", str(port)], io.BytesIO(b""), stdout),
            daemon=True)
        thread.start()
        client = connect(port)
        try:
            header = read_until(client, b"\n")
            self.assertEqual(b"Slim -- V0.5\n", header)
            client.sendall(frame(self.slim, ["1", "call", "actor", "hello"]))
            body = read_until(client, b"world")
            self.assertIn(b"world", body)
            client.sendall(frame(self.slim, "bye"))
        finally:
            client.close()
        thread.join(2)
        self.assertFalse(thread.is_alive())

    def test_an_empty_stream_ends_after_the_header(self):
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(b""), outgoing)
        self.assertEqual(b"Slim -- V0.5\n", outgoing.getvalue())

    def test_a_short_read_does_not_swallow_the_next_message(self):
        incoming = frame(self.slim, ["1", "call", "actor", "hello"]) + frame(self.slim, ["2", "call", "actor", "echo", "hi"])
        outgoing = io.BytesIO()
        self.slim.speak(OneByte(incoming), outgoing)
        text = outgoing.getvalue().decode("utf-8")
        self.assertIn("world", text)
        self.assertIn("hi", text)

    def test_a_truncated_body_ends_the_session(self):
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(b"000010:short"), outgoing)
        self.assertEqual(b"Slim -- V0.5\n", outgoing.getvalue())


def connect(port):
    last = None
    for _ in range(50):
        try:
            return socket.create_connection(("127.0.0.1", port), timeout=0.05)
        except OSError as err:
            last = err
    raise last


def read_until(sock, marker):
    sock.settimeout(2)
    data = b""
    while marker not in data:
        piece = sock.recv(4096)
        if not piece:
            break
        data += piece
    return data


class OneByte:
    """The first long read is one byte. A later read returns every byte it asks for."""

    def __init__(self, raw):
        self.raw = raw
        self.at = 0
        self.shorted = False

    def read(self, count):
        if count < 1 or self.at >= len(self.raw):
            return b""
        take = count
        if count > 1 and not self.shorted:
            self.shorted = True
            take = 1
        take = min(take, len(self.raw) - self.at)
        chunk = self.raw[self.at:self.at + take]
        self.at += take
        return chunk


class Buffer:
    def __init__(self, raw):
        self.buffer = raw


def drive_main(self, argv, stdin, stdout):
    saved = sys.argv, sys.stdin, sys.stdout
    try:
        sys.argv = argv
        sys.stdin = Buffer(stdin)
        sys.stdout = Buffer(stdout)
        self.slim.main()
    finally:
        sys.argv, sys.stdin, sys.stdout = saved


def frame(module, value):
    payload = b"bye" if value == "bye" else module.serialize([value]).encode("utf-8")
    return f"{len(payload):06d}:".encode("ascii") + payload


def write_lcov(hits):
    out = ROOT / "target" / "coverage" / "python" / "lcov.info"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = SCRIPT.read_text(encoding="utf-8").splitlines()
    body = ["SF:" + str(SCRIPT)]
    for number, _ in enumerate(lines, start=1):
        body.append(f"DA:{number},{1 if number in hits else 0}")
    body.append("end_of_record")
    out.write_text("\n".join(body) + "\n", encoding="utf-8")


if __name__ == "__main__":
    hits = set()

    def tracer(frame, event, arg):
        if event == "line" and Path(frame.f_code.co_filename).resolve() == SCRIPT.resolve():
            hits.add(frame.f_lineno)
        return tracer

    sys.settrace(tracer)
    threading.settrace(tracer)
    result = unittest.main(exit=False)
    sys.settrace(None)
    threading.settrace(None)
    if "mutation-workers" not in Path.cwd().as_posix():
        write_lcov(hits)
    sys.exit(0 if result.result.wasSuccessful() else 1)
