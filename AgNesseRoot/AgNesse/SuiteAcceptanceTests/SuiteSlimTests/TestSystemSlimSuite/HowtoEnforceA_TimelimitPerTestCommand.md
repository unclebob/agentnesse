{
  "type": "static",
  "tags": [],
  "help": null,
  "prune": true
}

# How to enforce that a test command takes only a limited amount of time during testing

The slim client needs an extra parrameter -s timeoutLimit but this is not passed by default. The limit is a whole number of seconds, as in `-s 1`, or a number of milliseconds with an `ms` suffix, as in `-s 500ms`. The failure reports that limit in seconds, so `500ms` is reported as `0.5`.

Please run each page seperately. If you run this as a suite the Slim Server is started only once taking the slim_flags from the first page created. 
The results from the second page are then misleading.

```agnesse
contents
```
