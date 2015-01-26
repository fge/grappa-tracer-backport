## Read me first

This project is licensed under both LGPLv3 and ASL 2.0. See
file LICENSE for more details.

Requires Java 7 or better.

The current version is **1.1.0**:

```groovy
dependencies {
    compile(group: "com.github.fge", name: "grappa-tracer-backport",
        version: "1.1.0");
}
```

## What is it

This is a backport of the event-listening parse runner in grappa 2.0.x for
grappa 1.0.x.

Included is a tracing listener which uou can use it to run your grappa
1.0.x/parboiled1 parsers, which you can then analyze using the [GUI
debugger](https://github.com/fge/grappa-debugger).

## Usage

An important precision first: **you do NOT need to annotate your parser with
`@BuildParseTree` in order for this tracing parser to work**.

You need a path to a zip file, which must **not** exist prior to running the
parser. In the example below:

* your parser is called `MyParser`;
* its parameter type is `String`;
* the path to the zip file is `/tmp/trace.zip`.

Code:

```java
final MyParser parser = Parboiled.createParser(MyParser.class);

final Path zipPath = Paths.get("/tmp/trace.zip");
final TracingParseRunnerListener<String> listener
    = new TracingParseRunnerListener<>(zipPath);
final ParseRunner<String> runner
    = new EventBasedParseRunner(parser.theRule());

runner.registerListener(listener);
runner.run(someInput);
```

After the run, the zip file will have been created. It will contain both the
input text which you used (which can be very large if you used
[largetext](https://github.com/fge/largetext)) and a JSON file containing all
the trace events.

You can then use the GUI debugger to load this zip file.

