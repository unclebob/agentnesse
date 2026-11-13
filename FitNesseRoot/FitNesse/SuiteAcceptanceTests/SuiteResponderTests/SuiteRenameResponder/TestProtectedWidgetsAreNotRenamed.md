{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Code, comments, paths, and URLs that happen to contain a page name are not rewritten. A real reference is.

| script | | | |
| --- | --- | --- | --- |
| given page | RnProtPage | with content | &#96;&#96;&#96;agentnesse<br>path RnProtTarget<br>&#96;&#96;&#96;<br><br>http://objectmentor.com/RnProtTarget<br><br>&#96;RnProtTarget&#96;<br><br>&lt;!-- RnProtTarget --&gt;<br><br>&#96;&#96;&#96;<br>RnProtTarget<br>&#96;&#96;&#96;<br><br>this link should be renamed RnProtTarget |
| given page | RnProtTarget | with content | whatever |
| check | request page | RnProtTarget?responder=renamePage&newName=RnProtNew&refactorReferences=on | 303 |
| page | RnProtPage | should contain | href="RnProtNew" |
| page | RnProtPage | should contain | http://objectmentor.com/RnProtTarget |
| page | RnProtPage | should contain | agentnesse-path |
| page | RnProtPage | should contain | <code>RnProtTarget</code> |
| page | RnProtPage | should not contain | path RnProtNew |
