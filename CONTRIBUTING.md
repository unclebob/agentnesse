# Contributing to AgNesse

Report bugs and feature requests in [GitHub issues](https://github.com/unclebob/agnesse/issues).

## Coding style

Java and Groovy use 2-space indentation, spaces only, and K&R braces. The rules that an editor can enforce are in `.editorconfig`. Match the source already in the tree.

```
  void f() {
    //
  }
```

## Before you commit

Run the unit tests and the acceptance suite:

```
./gradlew test
./gradlew acceptanceTest
```

Robert commits directly to master.

Questions go to [Uncle Bob](https://github.com/unclebob).
