{
  "type": "test",
  "tags": [],
  "help": "",
  "prune": false
}
### 
```agnesse
contents
```


Ensure the contents widget builds links to child pages.


| script | | | |
| --- | --- | --- | --- |
| given page | ParentPage | with content | &#96;&#96;&#96;agnesse<br>contents<br>&#96;&#96;&#96; |
| given page | ParentPage.FirstChild | | |
| given page | ParentPage.SecondChild | | |
| page | ParentPage | should have link to | ParentPage.FirstChild |
| and it should have link to | ParentPage.SecondChild | | |

