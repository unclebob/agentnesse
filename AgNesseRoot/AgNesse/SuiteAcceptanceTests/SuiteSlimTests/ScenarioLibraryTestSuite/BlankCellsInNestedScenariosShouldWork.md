{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
*Lighthouse issue #162. Found by student in Oslo, October, 2009*

It looks like one-word scenario names, coupled with at least one unnamed argument causes `AgNesse` to mistake the scenario invocation for a method invocation.

In the tables below, the scenario f should be called by the second table, but instead `AgNesse` tries to find method f for the current script actor. 

This test will pass if this bug is fixed.  Otherwise you'll get `f Method f[2] not found in agnesse.fixtures.PageDriver`.


| scenario | f | a | | b |
| --- | --- | --- | --- | --- |
| start | agnesse.slim.test.TestSlim | | | |


| script | | | |
| --- | --- | --- | --- |
| f | 1 | | 2 |
| pass | | | |



