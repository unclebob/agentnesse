const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("fs");
const http = require("http");
const net = require("net");
const os = require("os");
const path = require("path");
const test = require("node:test");

const root = path.resolve(__dirname, "../../../..");
const scriptPath = path.join(root, "src/agnesse/resources/javascript/agnesse.js");
const cssPath = path.join(root, "src/agnesse/resources/css/agnesse.css");

function pageHtml() {
  return `<!DOCTYPE html>
<html><head><link rel="stylesheet" href="/agnesse.css"></head><body>
<div id="test-results"></div>
<script id="agnesse-events" type="text/plain">
{"event":"page-started","path":"Suite.Child"}
{"event":"table-done","path":"Suite.Child","origin":"SetUp","originPath":"Suite.SetUp","block":{"name":"import","status":"pass","rows":[{"cells":[{"status":"pass","expected":"x"}]}]}}
</script>
<details class="agnesse-section agnesse-include" id="static">
<summary><button type="button" class="disclosure" aria-expanded="false">&gt;</button><a href="Page">Page</a></summary>
<p>static</p>
</details>
<script src="/agnesse.js"></script>
</body></html>`;
}

function listen(server) {
  return new Promise((resolve) => {
    server.listen(0, "127.0.0.1", () => resolve(server.address().port));
  });
}

function startServer(dir) {
  const server = http.createServer((req, res) => {
    const name = req.url.split("?")[0];
    const file = name === "/" ? path.join(dir, "index.html") : path.join(dir, name.slice(1));
    fs.readFile(file, (error, body) => {
      if (error) {
        res.writeHead(404);
        res.end();
        return;
      }
      res.writeHead(200);
      res.end(body);
    });
  });
  return listen(server).then((port) => ({ server, port }));
}

function freePort() {
  return new Promise((resolve, reject) => {
    const probe = net.createServer();
    probe.listen(0, "127.0.0.1", () => {
      const { port } = probe.address();
      probe.close(() => resolve(port));
    });
    probe.on("error", reject);
  });
}

function waitJson(url, chrome) {
  const stderr = [];
  chrome.stderr.on("data", (chunk) => {
    stderr.push(chunk);
    if (stderr.length > 40) stderr.shift();
  });
  return new Promise((resolve, reject) => {
    const started = Date.now();
    function attempt() {
      fetch(url).then((res) => res.json()).then(resolve).catch(() => {
        if (chrome.exitCode != null || Date.now() - started > 20000) {
          const detail = Buffer.concat(stderr).toString().trim();
          reject(new Error("Chrome did not open a debugger" + (detail ? `\n${detail}` : "")));
          return;
        }
        setTimeout(attempt, 20);
      });
    }
    attempt();
  });
}

function connect(url) {
  const ws = new WebSocket(url);
  let id = 0;
  const pending = new Map();
  const methods = new Map();
  ws.addEventListener("message", (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg);
      pending.delete(msg.id);
    }
    if (msg.method && methods.has(msg.method)) {
      methods.get(msg.method)(msg);
    }
  });
  const opened = new Promise((resolve) => ws.addEventListener("open", resolve));
  function send(method, params = {}) {
    const msgId = ++id;
    return new Promise((resolve) => {
      pending.set(msgId, resolve);
      ws.send(JSON.stringify({ id: msgId, method, params }));
    });
  }
  function until(method) {
    return new Promise((resolve) => methods.set(method, resolve));
  }
  return opened.then(() => ({ ws, send, until }));
}

async function evaluate(send, expression) {
  const res = await send("Runtime.evaluate", { expression, returnByValue: true });
  return res.result && res.result.result && res.result.result.value;
}

async function press(send, key) {
  const enter = key === "Enter";
  const base = enter
    ? { key: "Enter", code: "Enter", windowsVirtualKeyCode: 13, nativeVirtualKeyCode: 13 }
    : { key: " ", code: "Space", windowsVirtualKeyCode: 32, nativeVirtualKeyCode: 32, text: " " };
  if (enter) {
    await send("Input.dispatchKeyEvent", { type: "rawKeyDown", ...base });
    await send("Input.dispatchKeyEvent", { type: "char", ...base, text: "\r" });
  } else {
    await send("Input.dispatchKeyEvent", { type: "keyDown", ...base });
  }
  await send("Input.dispatchKeyEvent", { type: "keyUp", ...base });
}

async function toggleState(send, selector) {
  return evaluate(send, `(() => {
    const marker = document.querySelector(${JSON.stringify(selector)});
    const details = marker && marker.closest("details");
    return {
      found: Boolean(marker),
      tag: marker && marker.tagName,
      open: Boolean(details && details.open),
      expanded: marker && marker.getAttribute("aria-expanded"),
    };
  })()`);
}

async function focusAndPress(send, selector, key) {
  await evaluate(send, `document.querySelector(${JSON.stringify(selector)}).focus()`);
  await press(send, key);
  return toggleState(send, selector);
}

test("enter and space on the disclosure toggle a section in Chrome", async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "agnesse-disclosure-"));
  fs.writeFileSync(path.join(dir, "index.html"), pageHtml());
  fs.copyFileSync(scriptPath, path.join(dir, "agnesse.js"));
  fs.copyFileSync(cssPath, path.join(dir, "agnesse.css"));
  const web = await startServer(dir);
  const port = await freePort();
  const chrome = spawn("google-chrome", [
    "--headless=new",
    `--remote-debugging-port=${port}`,
    "--no-sandbox",
    "--disable-gpu",
    "--disable-dev-shm-usage",
    "--no-first-run",
    `--user-data-dir=${path.join(dir, "profile")}`,
    "about:blank",
  ], { stdio: ["ignore", "ignore", "pipe"] });
  try {
    const targets = await waitJson(`http://127.0.0.1:${port}/json`, chrome);
    const page = targets.find((item) => item.type === "page");
    const { ws, send, until } = await connect(page.webSocketDebuggerUrl);
    await send("Page.enable");
    const loaded = until("Page.loadEventFired");
    await send("Page.navigate", { url: `http://127.0.0.1:${web.port}/` });
    await loaded;

    const opened = await focusAndPress(send, "#static .disclosure", "Enter");
    assert.equal(opened.open, true, "Enter expands a focused disclosure");
    assert.equal(opened.expanded, "true");
    const closed = await focusAndPress(send, "#static .disclosure", " ");
    assert.equal(closed.open, false, "Space collapses a focused disclosure");
    assert.equal(closed.expanded, "false");

    const rendered = await focusAndPress(send, "#test-results .disclosure", "Enter");
    assert.equal(rendered.tag, "BUTTON");
    assert.equal(rendered.open, true, "Enter expands a result section");
    const renderedClosed = await focusAndPress(send, "#test-results .disclosure", " ");
    assert.equal(renderedClosed.open, false);
    assert.equal(renderedClosed.expanded, "false");
    ws.close();
  } finally {
    const exited = new Promise((resolve) => chrome.once("exit", resolve));
    chrome.kill("SIGKILL");
    await exited;
    web.server.close();
    try {
      fs.rmSync(dir, { recursive: true, force: true });
    } catch (error) {
      // The profile can still be flushing after Chrome exits. The keys already ran.
    }
  }
});
