{
  "type": "test",
  "tags": ["configuration"],
  "help": null,
  "prune": false
}
## You can redefine the keyword which catches exceptions
This is intended for backward compatibility to be able to switch this feature off
Don't use in other cases

```agnesse
define SLIM_EXCEPTION_COMPARATOR
HelloEXCEPTION:
```


| import |
| --- |
| agnesse.slim.test |


| Test Slim | | | |
| --- | --- | --- | --- |
| # Scenario | throw Normal? | getStringArg? | string |
| redefined Comparator name | HelloEXCEPTION:=~/This is my exception/ | EXCEPTION:=~/Test message/ | EXCEPTION:=~/Test message/ |
