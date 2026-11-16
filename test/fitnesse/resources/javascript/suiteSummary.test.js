const assert = require("node:assert/strict");
const fs = require("fs");
const path = require("path");
const test = require("node:test");
const vm = require("vm");

// Mutation workers overlay the source through the working directory.
const root = process.cwd();
const sourcePath = path.join(root, "src/fitnesse/resources/javascript/fitnesse.js");
const cssPath = path.join(root, "src/fitnesse/resources/css/agentnesse.css");

function walk(node) {
  const found = [];
  (node.children || []).forEach((child) => {
    found.push(child);
    walk(child).forEach((item) => found.push(item));
  });
  return found;
}

function matches(el, selector) {
  if (selector.charAt(0) === "#") {
    return el.id === selector.slice(1);
  }
  const dot = selector.indexOf(".");
  const tag = (dot < 0 ? selector : selector.slice(0, dot)).toUpperCase();
  const className = dot < 0 ? "" : selector.slice(dot + 1);
  if (tag && el.tagName !== tag) {
    return false;
  }
  if (className && !el.className.split(/\s+/).includes(className)) {
    return false;
  }
  return true;
}

function queryAll(start, selector) {
  let nodes = [start];
  selector.trim().split(/\s+/).forEach((part) => {
    const next = [];
    nodes.forEach((node) => {
      walk(node).forEach((el) => {
        if (matches(el, part)) {
          next.push(el);
        }
      });
    });
    nodes = next;
  });
  return nodes;
}

function createElement(tag) {
  const el = {
    tagName: String(tag).toUpperCase(),
    className: "",
    textContent: "",
    id: "",
    href: "",
    checked: false,
    style: {},
    children: [],
    parentNode: null,
    _listeners: {},
  };
  el.classList = {
    contains(name) {
      return el.className.split(/\s+/).includes(name);
    },
    add(name) {
      if (!el.classList.contains(name)) {
        el.className = `${el.className} ${name}`.trim();
      }
    },
    remove(name) {
      el.className = el.className.split(/\s+/).filter((part) => part && part !== name).join(" ");
    },
    toggle(name, force) {
      const on = force === undefined ? !el.classList.contains(name) : Boolean(force);
      if (on) {
        el.classList.add(name);
      } else {
        el.classList.remove(name);
      }
      return on;
    },
  };
  el.appendChild = (child) => {
    child.parentNode = el;
    el.children.push(child);
    return child;
  };
  el.remove = () => {
    const parent = el.parentNode;
    if (!parent) {
      return;
    }
    const at = parent.children.indexOf(el);
    if (at >= 0) {
      parent.children.splice(at, 1);
    }
    el.parentNode = null;
  };
  el.insertBefore = (child, ref) => {
    child.parentNode = el;
    const at = el.children.indexOf(ref);
    if (at < 0) {
      el.children.push(child);
    } else {
      el.children.splice(at, 0, child);
    }
    return child;
  };
  el.addEventListener = (type, fn) => {
    el._listeners[type] = el._listeners[type] || [];
    el._listeners[type].push(fn);
  };
  el.dispatchEvent = (type) => {
    (el._listeners[type] || []).forEach((fn) => fn.call(el));
  };
  el.querySelector = (selector) => queryAll(el, selector)[0] || null;
  el.querySelectorAll = (selector) => queryAll(el, selector);
  return el;
}

function page() {
  const document = createElement("document");
  const body = createElement("body");
  document.body = body;
  document.children = [body];
  document.appendChild = body.appendChild;
  const summaries = createElement("div");
  summaries.id = "test-summaries";
  const summary = createElement("div");
  summary.id = "test-summary";
  const results = createElement("div");
  results.id = "test-results";
  const hide = createElement("input");
  hide.id = "hidePassedTests";
  body.appendChild(summaries);
  body.appendChild(summary);
  body.appendChild(results);
  body.appendChild(hide);
  document.createElement = createElement;
  document.getElementById = (id) => walk(document).find((el) => el.id === id) || null;
  document.querySelectorAll = (selector) => queryAll(document, selector);
  document.addEventListener = body.addEventListener;
  document.dispatchEvent = body.dispatchEvent;
  return { document, summaries, summary, results, hide };
}

function render(events, prepare) {
  const view = page();
  const context = { document: view.document, console };
  context.window = context;
  vm.createContext(context);
  vm.runInContext(fs.readFileSync(sourcePath, "utf8"), context, { filename: sourcePath });
  if (prepare) {
    prepare(view);
  }
  view.document.dispatchEvent("DOMContentLoaded");
  events.forEach((event) => context.AgentNesseResults.apply(event));
  view.apply = (event) => context.AgentNesseResults.apply(event);
  return view;
}

function resultRow(cells) {
  const row = createElement("tr");
  cells.forEach(([className, text]) => {
    const cell = createElement("td");
    cell.className = className;
    if (text != null) {
      cell.textContent = text;
    }
    row.appendChild(cell);
  });
  return row;
}

function appendResultRows(parent) {
  return [
    [["pass", "ok"]],
    [["pass", "kept"], ["error", "boom"]],
    [["fail", "bad"]],
    [["fail", "bad"], ["pass", "kept"]],
    [["", "note"]],
    [["error", "boom"]],
  ].map((cells) => parent.appendChild(resultRow(cells)));
}

function assertResultRows(rows, passHidden) {
  assert.equal(classes(rows[0]).includes("hidden"), passHidden);
  rows.slice(1).forEach((row) => {
    assert.ok(!classes(row).includes("hidden"));
  });
}

function texts(nodes) {
  return nodes.map((node) => node.textContent);
}

function classes(el) {
  return el.className.split(/\s+/).filter(Boolean);
}

function countCells(row) {
  return row.children.filter((cell) => classes(cell).includes("count"));
}

const SUITE = "FitNesse.SuiteAcceptanceTests";
const EDIT = `${SUITE}.SuiteEditResponderTests.TestEditNewPage`;

test("summary rows put counts before a name relative to the suite", () => {
  const view = render([
    { event: "run-started", pages: 2, root: SUITE },
    {
      event: "page-done",
      path: EDIT,
      status: "pass",
      counts: { right: 5, wrong: 0, ignored: 2, exceptions: 1 },
      index: 1,
      pages: 2,
    },
    {
      event: "page-done",
      path: "XrefOne",
      status: "fail",
      counts: { right: 0, wrong: 3, ignored: 0, exceptions: 0 },
      index: 2,
      pages: 2,
    },
  ]);

  const css = fs.readFileSync(cssPath, "utf8");
  assert.match(css, /#test-summaries \.suite-summary \.count \{[^}]*text-align:\s*right/);

  const table = view.summaries.querySelector("table.suite-summary");
  assert.ok(table, "summary list is a table");
  const headers = table.querySelector("thead").querySelector("tr").children;
  assert.equal(table.querySelector("thead").children.length, 1);
  assert.deepEqual(texts(headers), ["right", "wrong", "ignored", "exceptions", "test"]);
  assert.deepEqual(headers.map((cell) => cell.className), ["count", "count", "count", "count", ""]);

  const rows = table.querySelector("tbody").children;
  assert.equal(rows.length, 2);

  assert.deepEqual(texts(countCells(rows[0])), ["5", "0", "2", "1"]);
  assert.deepEqual(rows[0].children.map((cell) => cell.className), ["count", "count", "count", "count", ""]);
  assert.ok(classes(rows[0]).includes("pass"));
  const link = rows[0].querySelector("a");
  assert.equal(link.textContent, "SuiteEditResponderTests.TestEditNewPage");
  assert.equal(link.href, `/${EDIT}`);

  assert.deepEqual(texts(countCells(rows[1])), ["0", "3", "0", "0"]);
  assert.ok(classes(rows[1]).includes("fail"));
  assert.equal(rows[1].querySelector("a").textContent, "XrefOne");
  assert.equal(rows[1].querySelector("a").href, "/XrefOne");
  assert.equal(view.summary.textContent, "5 right, 3 wrong, 1 exceptions");
  assert.equal(view.document.getElementById("progressBar").textContent, "Running tests ... (2/2)");
});

test("a page outside the suite keeps its path, and the suite page names itself", () => {
  const view = render([
    { event: "run-started", pages: 3, root: SUITE },
    {
      event: "page-done",
      path: SUITE,
      status: "pass",
      counts: { right: 1, wrong: 0, ignored: 0, exceptions: 0 },
      index: 1,
      pages: 3,
    },
    {
      event: "page-done",
      path: "FitNesse.SuiteAcceptanceTestsExtra.Child",
      status: "fail",
      counts: { right: 0, wrong: 1, ignored: 0, exceptions: 0 },
      index: 2,
      pages: 3,
    },
    { event: "page-done", path: `${SUITE}.NoCounts`, status: "empty", index: 3, pages: 3 },
  ]);
  const rows = view.summaries.querySelector("tbody").children;
  assert.equal(rows[0].querySelector("a").textContent, "(SuiteAcceptanceTests)");
  assert.equal(rows[0].querySelector("a").href, `/${SUITE}`);
  assert.equal(rows[1].querySelector("a").textContent, "FitNesse.SuiteAcceptanceTestsExtra.Child");
  assert.deepEqual(texts(countCells(rows[2])), ["0", "0", "0", "0"]);
  assert.equal(rows[2].querySelector("a").textContent, "NoCounts");
  assert.equal(rows[2].querySelector("a").href, `/${SUITE}.NoCounts`);

  const unnamed = render([
    { event: "run-started", pages: 1 },
    {
      event: "page-done",
      path: `${SUITE}.Test`,
      status: "pass",
      counts: { right: 1, wrong: 0, ignored: 0, exceptions: 0 },
      index: 1,
      pages: 1,
    },
    { event: "page-done", status: "error", counts: { right: 0, wrong: 0, ignored: 0, exceptions: 4 } },
    { event: "page-done", path: "Bare" },
  ]);
  const loose = unnamed.summaries.querySelector("tbody").children;
  assert.equal(loose[0].querySelector("a").textContent, `${SUITE}.Test`);
  assert.equal(loose[1].querySelector("a").textContent, "");
  assert.equal(loose[1].querySelector("a").href, "");
  assert.deepEqual(texts(countCells(loose[1])), ["0", "0", "0", "4"]);
  assert.ok(classes(loose[1]).includes("error"));
  assert.equal(loose[2].querySelector("a").textContent, "Bare");
  assert.equal(loose[2].className, "");
  assert.deepEqual(texts(countCells(loose[2])), ["0", "0", "0", "0"]);
});

test("hide passed still hides passed result rows and leaves the summary list in place", () => {
  const view = render([
    { event: "run-started", pages: 2, root: "SuitePage" },
    {
      event: "page-done",
      path: "SuitePage.Passing",
      status: "pass",
      counts: { right: 1, wrong: 0, ignored: 0, exceptions: 0 },
      index: 1,
      pages: 2,
    },
    {
      event: "page-done",
      path: "SuitePage.Failing",
      status: "fail",
      counts: { right: 0, wrong: 1, ignored: 0, exceptions: 0 },
      index: 2,
      pages: 2,
    },
  ]);
  const rows = appendResultRows(view.results);
  const summaryRows = () => view.summaries.querySelector("tbody").children;

  assert.ok(!rows.some((row) => classes(row).includes("hidden")));
  assert.ok(![...summaryRows()].some((row) => classes(row).includes("hidden")));

  view.hide.checked = true;
  view.hide.dispatchEvent("change");
  assertResultRows(rows, true);
  assert.ok(!classes(summaryRows()[0]).includes("hidden"));
  assert.ok(!classes(summaryRows()[1]).includes("hidden"));

  view.apply({
    event: "page-done",
    path: "SuitePage.Later",
    status: "pass",
    counts: { right: 2, wrong: 0, ignored: 0, exceptions: 0 },
    index: 3,
    pages: 4,
  });
  view.apply({
    event: "page-done",
    path: "SuitePage.LaterFail",
    status: "fail",
    counts: { right: 0, wrong: 3, ignored: 0, exceptions: 0 },
    index: 4,
    pages: 4,
  });
  assert.equal(summaryRows().length, 4);
  assert.ok(!classes(summaryRows()[2]).includes("hidden"));
  assert.ok(!classes(summaryRows()[3]).includes("hidden"));
  assert.equal(summaryRows()[2].querySelector("a").textContent, "Later");
  assert.equal(summaryRows()[3].querySelector("a").textContent, "LaterFail");

  view.hide.checked = false;
  view.hide.dispatchEvent("change");
  assert.ok(!rows.some((row) => classes(row).includes("hidden")));
  assert.ok(![...summaryRows()].some((row) => classes(row).includes("hidden")));
});

test("the progress line shows no pages done when the run starts", () => {
  const view = render([]);
  view.apply({ event: "run-started", pages: 4, root: SUITE });
  assert.equal(view.document.getElementById("progressBar").textContent, "Running tests ... (0/4)");
});

test("a table that never ran stays out of the exceptions column", () => {
  const view = render([
    { event: "run-started", pages: 2, root: SUITE },
    {
      event: "page-done",
      path: `${SUITE}.Boom`,
      status: "error",
      counts: { right: 0, wrong: 0, ignored: 0, exceptions: 0, tableFailures: 2 },
      index: 1,
      pages: 2,
    },
    {
      event: "page-done",
      path: `${SUITE}.Skipped`,
      status: "ignore",
      counts: { right: 0, wrong: 0, ignored: 3, exceptions: 0 },
      index: 2,
      pages: 2,
    },
  ]);
  const rows = view.summaries.querySelector("tbody").children;
  assert.ok(classes(rows[0]).includes("error"));
  assert.deepEqual(texts(countCells(rows[0])), ["0", "0", "0", "0"]);
  assert.equal(rows[0].querySelector("a").textContent, "Boom");
  assert.ok(classes(rows[1]).includes("ignore"));
  assert.equal(texts(countCells(rows[1]))[2], "3");
  assert.equal(rows[1].querySelector("a").textContent, "Skipped");
});

test("a run with no summary list still finishes the page", () => {
  const view = render([
    {
      event: "page-done",
      path: EDIT,
      status: "pass",
      counts: { right: 1, wrong: 0, ignored: 0, exceptions: 0 },
      index: 1,
      pages: 1,
    },
  ], (next) => {
    next.summaries.remove();
  });
  assert.equal(view.document.getElementById("test-summaries"), null);
  assert.equal(view.document.querySelectorAll("table.suite-summary").length, 0);
  assert.match(view.summary.textContent, /1 right/);
});

test("a summary row stays visible when the hide control is absent", () => {
  const view = render([
    { event: "run-started", pages: 1, root: SUITE },
    {
      event: "page-done",
      path: EDIT,
      status: "pass",
      counts: { right: 1, wrong: 0, ignored: 0, exceptions: 0 },
      index: 1,
      pages: 1,
    },
  ], (next) => {
    next.hide.remove();
  });
  const row = view.summaries.querySelector("tbody").children[0];
  assert.ok(!classes(row).includes("hidden"));
  assert.equal(view.document.getElementById("hidePassedTests"), null);
});

test("hide passed still hides result rows when the page has no summary table", () => {
  const view = render([]);
  view.summaries.remove();
  const rows = appendResultRows(view.results);
  view.hide.checked = true;
  view.hide.dispatchEvent("change");
  assertResultRows(rows, true);
  assert.equal(view.document.querySelectorAll("table.suite-summary").length, 0);
});

test("the page heading in the output stays the full path", () => {
  const view = render([{ event: "page-started", path: EDIT }]);
  assert.equal(view.results.querySelector("h2").textContent, EDIT);
});
