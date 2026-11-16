"""The guide's Slim script answers hello, and a short instruction is not a call."""
import importlib.util
import io
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / "AgNesseRoot" / "files" / "agnesse" / "tiny_slim.py"


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
        self.assertEqual("OK", self.slim.answer([instructions[1]])[0][1])

    def test_an_empty_stream_ends_after_the_header(self):
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(b""), outgoing)
        self.assertEqual(b"Slim -- V0.5\n", outgoing.getvalue())

    def test_a_truncated_body_ends_the_session(self):
        outgoing = io.BytesIO()
        self.slim.speak(io.BytesIO(b"000010:short"), outgoing)
        self.assertEqual(b"Slim -- V0.5\n", outgoing.getvalue())


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
    result = unittest.main(exit=False)
    sys.settrace(None)
    write_lcov(hits)
    sys.exit(0 if result.result.wasSuccessful() else 1)
