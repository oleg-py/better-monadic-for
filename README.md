# better-monadic-for
[![Gitter](https://img.shields.io/gitter/room/better-monadic-for/Lobby.svg?style=flat-square)](https://gitter.im/better-monadic-for/Lobby)
[![Waffle.io - Columns and their card count](https://badge.waffle.io/oleg-py/better-monadic-for.svg?style=flat-square&columns=backlog,gathering%20opinions)](https://waffle.io/oleg-py/better-monadic-for)
![Maven central](https://img.shields.io/maven-central/v/com.olegpy/better-monadic-for_2.12.svg?style=flat-square)

A Scala compiler plugin to give patterns and for-comprehensions the love they deserve

## Getting started
The plugin is available on Maven Central.
```sbt
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
```
To add into pants project 

Add into your root `BUILD` file 

```ini
jar_library(name='scalac-plugin-dep',
            jars=[
                scala_jar(org='com.olegpy', name='better-monadic-for', rev='0.3.1')
            ])
```

Add to your `pants.ini`

```ini
[scala]
scalac_plugins: ['bm4']
```

Supports Scala 2.11, 2.12, and 2.13.1

<details>
<summary><strong>Available plugin options</strong></summary>
  
---
All options have form of `-P:bm4:$feature:$flag`

  
| Feature                           | Flag (default)
|-----------------------------------|------------------------
| Desugaring without withFilter     | `-P:bm4:no-filtering:y`
| Elimination of identity map       | `-P:bm4:no-map-id:y`
| Elimination of tuples in bindings | `-P:bm4:no-tupling:y`
| Implicit definining patterns      | `-P:bm4:implicit-patterns:y`
  
Supported values for flags:
  - Disabling: `n`, `no`, `0`, `false`
  - Enabling: `y`, `yes`, `1`, `true`
  
---
  
</details>

<details>
<summary><strong>Changelog</strong></summary>

---

| Version | Changes
|---------|-------------------------------------------------------------------------------------------
| 0.3.1   | Fix issues with wartremover, implicit patterns with = binds & Xplugin-list flag
| 0.3.0-M4| Fix anonymous variables in Scala 2.12.7+
| M2, M3  | Fixes for implicit patterns
| 0.3.0-M1| Initial implementation of implicit patterns
| 0.2.4   | Fixed: incompatibility with [Dsl.scala](https://github.com/ThoughtWorksInc/Dsl.scala)
| 0.2.3   | Fixed: if-guards were broken when using untupling
| 0.2.2   | Fixed: destructuring within for bindings `(bar, baz) = foo`
| 0.2.1   | Fixed: untupling with `-Ywarn-unused:locals` causing warnings on e.g. `_ = println()`.
| 0.2.0   | Added optimizations: map elimination & untupling. Added plugin options.
| 0.1.0   | Initial version featuring for desugaring without `withFilter`s.

---

</details>

# Features
## Desugaring `for` patterns without `withFilter`s
### Destructuring `Either` / `IO` / `Task` / `FlatMap[F]`

This plugin lets you do:
```scala
import cats.implicits._
import cats.effect.IO

def getCounts: IO[(Int, Int)] = ???

for {
  (x, y) <- getCounts
} yield x + y
```

With regular Scala, this desugars to:
```scala
getCounts
  .withFilter((@unchecked _) match {
     case (x, y) => true
     case _ => false
  }
  .map((@unchecked _) match {
    case (x, y) => x + y
  }
```

Which fails to compile, because `IO` does not define `withFilter`

This plugin changes it to:
```scala
getCounts
  .map(_ match { case (x, y) => x + y })
```
Removing both `withFilter` and `unchecked` on generated `map`. So the code just works.

<details>
<summary><b>Additional Effects</b></summary>

### Type ascriptions on LHS

Type ascriptions on left-hand side do not become an `isInstanceOf` check - which they do by default. E.g.

```scala
def getThing: IO[String] = ???

for {
  x: String <- getCounts
} yield s"Count was $x"
```

would desugar directly to

```scala
getCounts.map((x: String) => s"Count was $x")
```

This also works with `flatMap` and `foreach`, of course.

### No silent truncation of data

This example is taken from [Scala warts post](http://www.lihaoyi.com/post/WartsoftheScalaProgrammingLanguage.html#conflating-total-destructuring-with-partial-pattern-matching) by @lihaoyi
```scala
// Truncates 5
for((a, b) <- Seq(1 -> 2, 3 -> 4, 5)) yield a + " " +  b

// Throws MatchError
Seq(1 -> 2, 3 -> 4, 5).map{case (a, b) => a + " " + b}
```

With the plugin, both versions are equivalent and result in `MatchError`

### Match warnings
Generators will now show exhaustivity warnings now whenever regular pattern matches would:

```scala
        import cats.syntax.option._

        for (Some(x) <- IO(none[Int])) yield x
```

```
D:\Code\better-monadic-for\src\test\scala\com\olegpy\TestFor.scala:66
:22: match may not be exhaustive.
[warn] It would fail on the following input: None
[warn]         for (Some(x) <- IO(none[Int])) yield x
[warn]                      ^
```

</details>

## Final map optimization

Eliminate calls to `.map` in comprehensions like this:

```scala
for {
  x <- xs
  y <- getYs(x)
} yield y
```

Standard desugaring is

```scala
xs.flatMap(x => getYs(x).map(y => y))
```

This plugin simplifies it to

```scala
xs.flatMap(x => getYs(x))
```

## Desugar bindings as vals instead of tuples

Direct fix for [lampepfl/dotty#2573](https://github.com/lampepfl/dotty/issues/2573).
If the binding is not used in follow-up `withFilter`, it is desugared as
plain `val`s, saving on allocations and primitive boxing.

## Define implicits in for-comprehensions or matches

Since version 0.3.0-M1, it is possible to define implicit values inside for-comprehensions using a new keyword `implicit0`:

```scala
case class ImplicitTest(id: String)

for {
  x <- Option(42)
  implicit0(it: ImplicitTest) <- Option(ImplicitTest("eggs"))
  _ <- Option("dummy")
  _ = "dummy"
  _ = assert(implicitly[ImplicitTest] eq it)
} yield "ok"
```

In current version (0.3.0) it's required to specify a type annotation in a pattern with `implicit0`.

It also works in regular match clauses:
```scala
(1, "foo", ImplicitTest("eggs")) match {
  case (_, "foo", implicit0(it: ImplicitTest)) => assert(implicitly[ImplicitTest] eq it)
}
```

# Notes
- This plugin reserves one extra keyword, `implicit0`, if corresponding option for implicit patterns is enabled (which is by default).
- Regular `if` guards are not affected, only generator arrows.

# License
MIT
