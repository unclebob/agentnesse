const summaryColumns = function () {
  return ["right", "wrong", "ignored", "exceptions"];
};

const summaryCount = function (value) {
  if (!value) {
    return 0;
  }
  return value;
};

const summaryCounts = function (counts) {
  var source = counts || {};
  var columns = summaryColumns();
  var values = [];
  for (var i = 0; i < columns.length; i++) {
    values.push(summaryCount(source[columns[i]]));
  }
  return values;
};

const leafName = function (path) {
  var parts = path.split(".");
  return parts[parts.length - 1];
};

const relativePageName = function (path, root) {
  if (!path) {
    return "";
  }
  if (!root) {
    return path;
  }
  if (path === root) {
    return "(" + leafName(path) + ")";
  }
  var prefix = root + ".";
  if (path.indexOf(prefix) === 0) {
    return path.substring(prefix.length);
  }
  return path;
};

const summaryHref = function (path) {
  if (!path) {
    return "";
  }
  return "/" + path;
};

const summaryPage = function (event) {
  return {
    counts: summaryCounts(event.counts),
    path: event.path || "",
    status: event.status || ""
  };
};

const summaryCell = function (doc, tag, className, text) {
  var node = doc.createElement(tag);
  node.className = className;
  node.textContent = text;
  return node;
};

const summaryHeader = function (doc) {
  var head = doc.createElement("thead");
  var row = doc.createElement("tr");
  var columns = summaryColumns();
  for (var i = 0; i < columns.length; i++) {
    row.appendChild(summaryCell(doc, "th", "count", columns[i]));
  }
  row.appendChild(summaryCell(doc, "th", "", "test"));
  head.appendChild(row);
  return head;
};

const ensureSummaryTable = function (doc, list) {
  var table = list.querySelector("table.suite-summary");
  if (table) {
    return table;
  }
  table = doc.createElement("table");
  table.className = "suite-summary";
  table.appendChild(summaryHeader(doc));
  table.appendChild(doc.createElement("tbody"));
  list.appendChild(table);
  return table;
};

const summaryNameCell = function (doc, path, root) {
  var cell = doc.createElement("td");
  var link = doc.createElement("a");
  link.href = summaryHref(path);
  link.textContent = relativePageName(path, root);
  cell.appendChild(link);
  return cell;
};

const appendSummaryRow = function (doc, table, event, root) {
  var done = summaryPage(event);
  var row = doc.createElement("tr");
  row.className = done.status;
  for (var i = 0; i < done.counts.length; i++) {
    row.appendChild(summaryCell(doc, "td", "count", String(done.counts[i])));
  }
  row.appendChild(summaryNameCell(doc, done.path, root));
  table.querySelector("tbody").appendChild(row);
  return row;
};

(function () {
  var SECTIONS = { SetUp: 1, TearDown: 1, SuiteSetUp: 1, SuiteTearDown: 1, ScenarioLibrary: 1 };
  var pageRoot = null;
  var title = "";
  var details = null;
  var blocks = [];
  var totals = { right: 0, wrong: 0, exceptions: 0, ignored: 0 };
  var worst = "pass";
  var pagesTotal = 0;
  var pagesFinished = 0;
  var barFailed = false;
  var suiteRoot = "";

  function shouldOpen(list) {
    for (var i = 0; i < list.length; i++) {
      if (trouble(list[i])) return true;
    }
    return false;
  }

  function trouble(block) {
    var status = block.status || "";
    if (status === "fail" || status === "error" || status === "table-failure") return true;
    var rows = block.rows || [];
    for (var r = 0; r < rows.length; r++) {
      var cells = rows[r].cells || [];
      for (var c = 0; c < cells.length; c++) {
        var cell = cells[c];
        if (cell.status === "fail" || cell.status === "error" || cell.error) return true;
      }
    }
    return false;
  }

  function text(cell) {
    if (cell.symbol) {
      var secret = cell.symbol.indexOf("SECRET_") === 0;
      return "$" + cell.symbol + (secret ? "->[*****]" : "<-[" + (cell.value || "") + "]");
    }
    var body;
    if (cell.value) body = cell.value;
    else {
      var expected = cell.expected || "";
      var actual = cell.actual || "";
      body = (actual && actual !== expected) ? actual : (expected || actual || "");
    }
    if (cell.error && cell.error.message) return (body ? body + " " : "") + cell.error.message;
    return body;
  }

  function el(name, className, textValue) {
    var node = document.createElement(name);
    if (className) node.className = className;
    if (textValue != null) node.textContent = textValue;
    return node;
  }

  function renderBlock(block) {
    var type = block.name ? " " + block.name : "";
    var table = el("table", "agnesse-table" + type);
    (block.rows || []).forEach(function (row) {
      var tr = document.createElement("tr");
      (row.cells || []).forEach(function (cell) {
        var status = cell.status || (cell.error ? "error" : "");
        tr.appendChild(el("td", status, text(cell)));
      });
      table.appendChild(tr);
    });
    return table;
  }

  function sectionHref(event, title) {
    if (title !== event.origin) return "";
    return event.originPath;
  }

  function sectionSummary(event, title) {
    var summary = el("summary");
    var marker = el("span", "disclosure", ">");
    marker.setAttribute("aria-hidden", "true");
    summary.appendChild(marker);
    var href = sectionHref(event, title);
    if (href) {
      var link = el("a", "", title);
      link.href = href;
      summary.appendChild(link);
    } else {
      summary.appendChild(document.createTextNode(title));
    }
    return summary;
  }

  function sectionTitle(event) {
    var origin = event.origin || "";
    var name = (event.block && event.block.name) || "";
    if (SECTIONS[origin]) return origin;
    if (name.toLowerCase() === "import") return "import";
    var leaf = (event.path || "").split(".").pop();
    if (origin && origin !== leaf) return origin;
    return "";
  }

  function closeSection() {
    title = "";
    details = null;
    blocks = [];
  }

  function root() {
    return document.getElementById("test-results");
  }

  function worse(status) {
    if (status === "error" || worst === "error") return "error";
    if (status === "fail" || worst === "fail") return "fail";
    return worst || "pass";
  }

  function summaryText() {
    return totals.right + " right, " + totals.wrong + " wrong, "
      + totals.exceptions + " exceptions";
  }

  function updateSummary(event) {
    var counts = event.counts || {};
    totals.right += counts.right || 0;
    totals.wrong += counts.wrong || 0;
    totals.exceptions += counts.exceptions || 0;
    totals.ignored += counts.ignored || 0;
    worst = worse(event.status || "");
    var summary = document.getElementById("test-summary");
    if (summary) {
      summary.textContent = summaryText();
      summary.className = worst;
    }
    var list = document.getElementById("test-summaries");
    if (list) {
      appendSummaryRow(document, ensureSummaryTable(document, list), event, suiteRoot);
    }
  }

  function progressBar() {
    var bar = document.getElementById("progressBar");
    if (bar) return bar;
    bar = el("div", "pass", "");
    bar.id = "progressBar";
    var summary = document.getElementById("test-summary");
    if (summary && summary.parentNode) summary.parentNode.insertBefore(bar, summary);
    return bar;
  }

  function paintProgress() {
    var bar = progressBar();
    if (!bar) return;
    var total = pagesTotal > 0 ? pagesTotal : 0;
    var percent = total === 0 ? 0 : (100 * pagesFinished / total);
    bar.style.width = percent + "%";
    bar.className = barFailed ? "fail" : "pass";
    bar.textContent = "Running tests ... (" + pagesFinished + "/" + (total || 0) + ")";
  }

  function notePageFinished(event) {
    pagesFinished = event.index || pagesFinished + 1;
    if (event.pages) pagesTotal = event.pages;
    if (event.status === "fail" || event.status === "error") barFailed = true;
    paintProgress();
  }

  function apply(event) {
    if (!event || !event.event) return;
    if (event.event === "run-started") {
      suiteRoot = event.root || "";
      pagesTotal = event.pages || 0;
      paintProgress();
    } else if (event.event === "page-started") {
      closeSection();
      var results = root();
      if (!results) return;
      pageRoot = el("section", "page-result");
      pageRoot.appendChild(el("h2", "", event.path || ""));
      results.appendChild(pageRoot);
      document.body.classList.add("test-running");
    } else if (event.event === "table-done" && pageRoot) {
      var next = sectionTitle(event);
      if (next !== title) {
        closeSection();
        title = next;
        if (title) {
          details = el("details", "agnesse-section agnesse-include");
          details.appendChild(sectionSummary(event, title));
          pageRoot.appendChild(details);
        }
      }
      var block = event.block || {};
      blocks.push(block);
      (details || pageRoot).appendChild(renderBlock(block));
      if (details && shouldOpen(blocks)) details.open = true;
    } else if (event.event === "output") {
      var note = el("p", "error", event.text || "");
      if (pageRoot) pageRoot.appendChild(note);
      else if (root()) root().appendChild(note);
    } else if (event.event === "page-done") {
      closeSection();
      notePageFinished(event);
      updateSummary(event);
    } else if (event.event === "run-done") {
      closeSection();
      if (event.summary) {
        var done = document.getElementById("test-summary");
        if (done) {
          done.textContent = event.summary;
          if (event.status) done.className = event.status;
        }
      }
      document.body.classList.remove("test-running");
      var menu = document.getElementById("test-menu");
      if (menu) menu.classList.remove("hidden");
      initErrorNav();
    }
  }

  function renderEvents(events, target) {
    if (target && target.id !== "test-results") {
      target.id = "test-results";
    }
    events.forEach(apply);
  }

  function initErrorNav() {
    var errors = Array.prototype.slice.call(document.querySelectorAll("#test-results td.fail, #test-results td.error"));
    var max = document.getElementById("error-nav-max");
    var field = document.getElementById("error-nav-text");
    var nav = document.getElementById("error-nav");
    if (!field || errors.length === 0) return;
    if (max) max.textContent = String(errors.length);
    if (nav) nav.classList.remove("hidden");
    var index = 0;
    function show(next) {
      if (errors[index]) errors[index].classList.remove("selected-error");
      index = (next + errors.length) % errors.length;
      var cell = errors[index];
      cell.classList.add("selected-error");
      var opened = cell.closest("details");
      if (opened) opened.open = true;
      field.value = String(index + 1);
      cell.scrollIntoView({ block: "center" });
    }
    var prev = document.getElementById("error-nav-prev");
    var nextButton = document.getElementById("error-nav-next");
    if (prev) prev.onclick = function () { show(index - 1); };
    if (nextButton) nextButton.onclick = function () { show(index + 1); };
    show(0);
  }

  function rowPassed(row) {
    var cells = row.children;
    for (var i = 0; i < cells.length; i++) {
      if (cells[i].classList.contains("fail") || cells[i].classList.contains("error")) return false;
    }
    return true;
  }

  window.onbeforeunload = function () {
    if (document.body.classList.contains("test-running")) {
      return "There is a test or suite currently running.\nAre you sure you want to navigate away from this page?";
    }
  };

  document.addEventListener("DOMContentLoaded", function () {
    var source = document.getElementById("agnesse-events");
    if (source && source.textContent.trim()) {
      source.textContent.split("\n").forEach(function (line) {
        if (line.trim()) apply(JSON.parse(line));
      });
    }
    var hide = document.getElementById("hidePassedTests");
    if (hide) {
      hide.addEventListener("change", function () {
        document.querySelectorAll("#test-results tr").forEach(function (row) {
          var passed = row.querySelector("td.pass") && rowPassed(row);
          row.classList.toggle("hidden", hide.checked && passed);
        });
      });
    }
  });

  window.AgNesseResults = { shouldOpen: shouldOpen, apply: apply, renderEvents: renderEvents };
})();
