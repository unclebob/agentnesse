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

    def test_a_later_integer_and_a_record_kind(self):
        later = ["1", "callAndAssign", "symbol", "actor", "endIsAfterStart", "0", "1"]
        also = ["9", "call", "actor", "endIsAfterStart", "1", "2"]
        same = ["10", "call", "actor", "endIsAfterStart", "2", "2"]
        earlier = ["2", "call", "actor", "endIsAfterStart", "4", "1"]
        broken = ["3", "call", "actor", "endIsAfterStart", "a", "1"]
        missing = ["4", "call", "actor", "endIsAfterStart"]
        single = ["12", "call", "actor", "endIsAfterStart", "1"]
        person = ["5", "call", "actor", "kind", '{"doc":{"kind":"id"}}']
        assigned_kind = ["11", "callAndAssign", "symbol", "actor", "kind", '{"kind":"id"}']
        other = ["6", "callAndAssign", "symbol", "actor", "kind", '{"kind":"passport"}']
        blank = ["7", "call", "actor", "kind"]
        nested = ["8", "call", "actor", "kind", ["not-a-string"]]
        self.assertEqual("yes", self.slim.answer([later])[0][1])
        self.assertEqual("yes", self.slim.answer([also])[0][1])
        self.assertEqual("no", self.slim.answer([same])[0][1])
        self.assertEqual("no", self.slim.answer([earlier])[0][1])
        self.assertEqual("no", self.slim.answer([broken])[0][1])
        self.assertEqual("no", self.slim.answer([missing])[0][1])
        self.assertEqual("no", self.slim.answer([single])[0][1])
        self.assertEqual("", self.slim.method_name(["1", "callAndAssign"]))
        self.assertEqual("", self.slim.method_name(["1", "callAndAssign", "symbol", "actor"]))
        self.assertEqual("id", self.slim.answer([person])[0][1])
        self.assertEqual("id", self.slim.answer([assigned_kind])[0][1])
        self.assertEqual("no", self.slim.answer([other])[0][1])
        self.assertEqual("no", self.slim.answer([blank])[0][1])
        self.assertEqual("no", self.slim.answer([nested])[0][1])
        self.assertEqual([], self.slim.arguments(["only"]))
        self.assertEqual([], self.slim.arguments(["1", "make", "actor"]))
        self.assertEqual("", self.slim.command(["1"]))
        self.assertEqual("", self.slim.command(["1", ["make"]]))
        self.assertEqual("make", self.slim.command(["1", "make"]))
        self.assertEqual("OK", self.slim.text_at(["1", "make"], 4, "OK"))

    def test_a_gate_starts_new_and_a_blocked_release_is_refused(self):
        made = ["1", "make", "gate", "tiny"]
        opened = ["2", "call", "gate", "open"]
        blocked = ["3", "call", "gate", "block"]
        refused = ["4", "call", "gate", "release"]
        phase = ["5", "call", "gate", "phase"]
        still = ["6", "call", "gate", "released"]
        other = ["7", "make", "other", "tiny"]
        other_phase = ["8", "call", "other", "phase"]
        batch = [made, opened, blocked, refused, phase, still, other, other_phase]
        answered = dict(self.slim.answer(batch))
        self.assertEqual("OK", answered["1"])
        self.assertEqual("ok", answered["2"])
        self.assertEqual("ok", answered["3"])
        self.assertEqual("refused", answered["4"])
        self.assertEqual("blocked", answered["5"])
        self.assertEqual("false", answered["6"])
        self.assertEqual("new", answered["8"])

    def test_release_from_open_and_the_refused_moves(self):
        shared = {}
        self.assertEqual("OK", self.slim.answer([["1", "make", "gate"]], shared)[0][1])
        self.assertEqual("ok", self.slim.answer([["2", "call", "gate", "open"]], shared)[0][1])
        self.assertEqual("ok", self.slim.answer([["3", "call", "gate", "release"]], shared)[0][1])
        self.assertEqual("released", self.slim.answer([["4", "call", "gate", "phase"]], shared)[0][1])
        self.assertEqual("true", self.slim.answer([["5", "call", "gate", "released"]], shared)[0][1])
        self.assertEqual("refused", self.slim.answer([["6", "call", "gate", "open"]], shared)[0][1])
        self.assertEqual("refused", self.slim.answer([["7", "call", "gate", "block"]], shared)[0][1])
        self.assertEqual("refused", self.slim.answer([["8", "call", "missing", "open"]])[0][1])
        self.assertEqual("", self.slim.answer([["9", "call", "missing", "phase"]])[0][1])
        self.assertEqual("false", self.slim.answer([["10", "call", "missing", "released"]])[0][1])
        self.assertEqual("OK", self.slim.answer([["11", "make"]])[0][1])
        self.assertEqual("refused", self.slim.answer([["12", "call", "", "open"]])[0][1])
        self.assertEqual("refused", self.slim.answer([["13", "call", ["gate"], "block"]])[0][1])
        self.assertEqual("OK", self.slim.answer([["14", "make", "", "tiny"]])[0][1])
        self.assertEqual("refused", self.slim.answer([["15", "make", ""], ["16", "call", "", "open"]])[1][1])
        name = ["gate"]
        self.assertEqual("refused", self.slim.answer([["17", "make", name], ["18", "call", name, "open"]])[1][1])

    def test_a_gate_keeps_its_phase_across_messages(self):
        incoming = frame(self.slim, ["1", "make", "gate", "tiny"])
        incoming += frame(self.slim, ["2", "call", "gate", "open"])
        incoming += frame(self.slim, ["3", "call", "gate", "phase"])
        incoming += frame(self.slim, "bye")
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(incoming), outgoing)
        text = outgoing.getvalue().decode("utf-8")
        self.assertIn("ok", text)
        self.assertIn("open", text)

    def test_pipe_mode_reads_the_last_argument(self):
        incoming = frame(self.slim, ["1", "call", "actor", "hello"]) + frame(self.slim, "bye")
        stdout = io.BytesIO()
        drive_main(self, ["tiny_slim.py", "not-a-port", "1"], io.BytesIO(incoming), stdout)
        self.assertIn(b"world", stdout.getvalue())

    def test_socket_mode_answers_on_the_given_port(self):
        seen = {}
        real = self.slim.socket.socket
        reuse = socket.SO_REUSEADDR

        class Watching(real):
            def setsockopt(self, level, option, value):
                if option == reuse:
                    seen["reuse"] = value
                return super().setsockopt(level, option, value)

            def listen(self, backlog):
                seen["backlog"] = backlog
                return super().listen(backlog)

        self.slim.socket.socket = Watching
        probe = socket.socket()
        probe.bind(("127.0.0.1", 0))
        port = probe.getsockname()[1]
        probe.close()
        stdout = io.BytesIO()
        thread = threading.Thread(
            target=drive_main,
            args=(self, ["tiny_slim.py", "not-a-port", str(port)], io.BytesIO(b""), stdout),
            daemon=True)
        try:
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
            self.assertEqual(1, seen["reuse"])
            self.assertEqual(8, seen["backlog"])
        finally:
            self.slim.socket.socket = real

    def test_a_safe_seat_books_once_and_an_unknown_call_is_named(self):
        shared = {}
        self.assertEqual("OK", self.slim.answer([["1", "make", "seat", "tiny", "safe"]], shared)[0][1])
        self.assertEqual("booked", self.slim.answer([["2", "call", "seat", "book"]], shared)[0][1])
        self.assertEqual("full", self.slim.answer([["3", "call", "seat", "book"]], shared)[0][1])
        self.assertIn("NO_METHOD_IN_CLASS", self.slim.answer([["4", "call", "seat", "__agnesse_lane_probe__"]], shared)[0][1])
        self.assertIn("NO_INSTANCE", self.slim.answer([["5", "call", "other", "book"]])[0][1])
        raced = {}
        self.assertEqual("OK", self.slim.answer([["6", "make", "seat", "tiny", "racy"]], raced)[0][1])
        self.assertEqual("booked", self.slim.answer([["7", "call", "seat", "book"]], raced)[0][1])
        self.assertEqual("full", self.slim.answer([["8", "call", "seat", "book"]], raced)[0][1])

    def test_two_sockets_share_one_safe_seat(self):
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
        first = connect(port)
        second = connect(port)
        try:
            self.assertEqual(b"Slim -- V0.5\n", read_until(first, b"\n"))
            self.assertEqual(b"Slim -- V0.5\n", read_until(second, b"\n"))
            first.sendall(frame(self.slim, ["1", "make", "seat", "tiny", "safe"]))
            self.assertIn(b"OK", read_until(first, b"OK"))
            second.sendall(frame(self.slim, ["2", "call", "seat", "book"]))
            self.assertIn(b"booked", read_until(second, b"booked"))
            first.sendall(frame(self.slim, ["3", "call", "seat", "book"]))
            self.assertIn(b"full", read_until(first, b"full"))
            second.sendall(frame(self.slim, "bye"))
            first.sendall(frame(self.slim, "bye"))
        finally:
            second.close()
            first.close()
        thread.join(2)
        self.assertFalse(thread.is_alive())

    def test_overlapping_racy_books_both_succeed_and_safe_books_do_not(self):
        self.assertEqual(["booked", "booked"], sorted(both(self, "racy")))
        self.assertEqual(["booked", "full"], sorted(both(self, "safe")))

    def test_an_unreadable_stream_ends_after_the_header(self):
        for raw in (b"", b"000010:short"):
            outgoing = io.BytesIO()
            self.slim.speak(io.BytesIO(raw), outgoing)
            self.assertEqual(b"Slim -- V0.5\n", outgoing.getvalue())

    def test_a_short_read_does_not_swallow_the_next_message(self):
        incoming = frame(self.slim, ["1", "call", "actor", "hello"]) + frame(self.slim, ["2", "call", "actor", "echo", "hi"])
        outgoing = io.BytesIO()
        self.slim.speak(OneByte(incoming), outgoing)
        text = outgoing.getvalue().decode("utf-8")
        self.assertIn("world", text)
        self.assertIn("hi", text)


def both(test, mark):
    shared = {}
    test.slim.answer([["1", "make", "seat", "tiny", mark]], shared)
    found = []

    def run():
        found.append(test.slim.answer([["2", "call", "seat", "book"]], shared)[0][1])

    threads = [threading.Thread(target=run), threading.Thread(target=run)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return found


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
