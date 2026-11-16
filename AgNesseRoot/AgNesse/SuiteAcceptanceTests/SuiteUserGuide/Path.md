{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Path

A `path` block adds a classpath entry. Entries on this page come before entries on ancestor pages. The Slim process sees them on `java.class.path`.

```agnesse
path
guide-path-marker
```

| script | java properties |
| --- | --- |
| ensure | property should match; | java.class.path | `.*guide-path-marker.*` |
