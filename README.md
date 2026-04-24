[![Build](https://github.com/Connellite/ExtraLib/actions/workflows/ci.yml/badge.svg)](https://github.com/Connellite/ExtraLib/actions/workflows/ci.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.connellite/ExtraLib)](https://mvnrepository.com/artifact/io.github.connellite/ExtraLib)
![Endpoint Badge](https://img.shields.io/endpoint?url=https%3A%2F%2Fghloc.vercel.app%2Fapi%2FConnellite%2FExtraLib%2Fbadge%3Fbranch%3Dmaster%26filter%3Djava)


# ExtraLib

Small Java 17 utility library: JDBC helpers, collections, string/date/UUID utilities, `Fmt` formatting, matching, logging, and more.

## Fmt

[`Fmt`](src/main/java/io/github/connellite/format/Fmt.java): `{}` placeholders, optional `String.format`-like specs (with strftime-style `%` for dates), named fields, nested `{}` for dynamic width/precision, and null-safe readable array output—see Javadoc for flags and `%` codes.

```java
// simple substitution
Fmt.println("Hello, {}!", "world");

// locale + dynamic precision
String s = Fmt.format(Locale.US, "{:.{}f}", 3.14, 1); // "3.1"

// named fields
String user = Fmt.format(
        "User {name}, id={id}",
        Fmt.arg("name", "Ann"),
        Fmt.arg("id", 42));

// compile once, format many times
CompiledFormat pattern = Fmt.compile("x={} y={}");
Fmt.println(pattern, 1, 2);

// write to Appendable without an intermediate String
StringBuilder sb = new StringBuilder();
Fmt.formatTo(sb, "count={}", 10);
```

## Other packages

| Package | What it offers |
|--------|----------------|
| `io.github.connellite.jdbc` | `ResultSet` iterators/streams, query and DB metadata helpers. |
| `io.github.connellite.collections` | Null-skipping collections, case-insensitive `Map`/`Set`, `UniqueArrayList`, delegating wrappers. |
| `io.github.connellite.util` | Date/UUID parsing, strings, networking, process runner, reflection, ASCII conversion, and more. |
| `io.github.connellite.match` | Substring search: KMP, Boyer–Moore–Horspool, wildcard patterns. |
| `io.github.connellite.concurrent` | Small `ThreadPool` implementation. |
| `io.github.connellite.cloner` | Cloning via reflection or serialization. |


## Requirements

- JDK 17+
- Maven 3.9+

## Dependency

```xml
<dependency>
    <groupId>io.github.connellite</groupId>
    <artifactId>ExtraLib</artifactId>
    <version>0.34</version>
</dependency>
```
