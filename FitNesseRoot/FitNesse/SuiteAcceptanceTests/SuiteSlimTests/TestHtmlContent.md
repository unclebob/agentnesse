{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Slim tables can contain HTML content. If content qualifies as HTML, it is passed to the SUT as HTML.

```agentnesse
define html
!note > 50% (note) !meta < 50% (meta)
```

<details>
<summary>scenarios</summary>

```agentnesse
define no_html
< 50%
```


| scenario | Content | content | is returned as | out |
| --- | --- | --- | --- | --- |
| check | echo | @content | @out | |
| set name | @content | | | |


| scenario | And the SUT sees | substring |
| --- | --- | --- |
| ensure | name contains | @substring |

</details>


| script | | | |
| --- | --- | --- | --- |
| Content | ${html} | is returned as | ${html} |
| And the SUT sees | !note > 50% (note) | | |
| And the SUT sees | !meta < 50% (meta) | | |


| script | | | |
| --- | --- | --- | --- |
| Content | ${no_html} | is returned as | ${no_html} |
| And the SUT sees | < 50% | | |
