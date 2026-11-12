{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| import |
| --- |
| fitnesse.slim.test |

### Slim symbols names must start with a letter, followed by letters, digits and underscores


| test slim | |
| --- | --- |
| string | getStringArg? |
| foo | $x3_= |
| $x3_ | foo |

### Referencing undefined Slim symbols names raise no error but return the symbol name


| test slim | |
| --- | --- |
| string | getStringArg? |
| $UNDEFINED | $UNDEFINED |

### Slim symbols names are case sensitive


| test slim | |
| --- | --- |
| string | getStringArg? |
| lower | $x= |
| upper | $X= |
| $x | lower |
| $X | upper |

### Slim symbols names also support UTF8 names


| test slim | |
| --- | --- |
| string | getStringArg? |
| ØÅÆØÅÆ_ØÅÆØÅÆ | $Æ_ØÅ= |
| $Æ_ØÅ | ØÅÆØÅÆ_ØÅÆØÅÆ |
