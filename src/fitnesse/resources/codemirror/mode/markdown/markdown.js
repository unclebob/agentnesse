CodeMirror.defineMode("markdown", function () {
  return {
    token: function (stream) {
      if (stream.sol() && stream.match(/^#{1,6}\s/)) {
        return "header";
      }
      if (stream.sol() && stream.match(/^\s*\|/)) {
        return "keyword";
      }
      if (stream.match(/`[^`]*`/)) {
        return "comment";
      }
      stream.next();
      return null;
    }
  };
});
CodeMirror.defineMIME("text/markdown", "markdown");
