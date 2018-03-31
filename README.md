# better-monadic-for
[![Gitter](https://img.shields.io/gitter/room/better-monadic-for/Lobby.svg?style=flat-square)](https://gitter.im/better-monadic-for/Lobby)
[![Waffle.io - Columns and their card count](https://badge.waffle.io/oleg-py/better-monadic-for.svg?style=flat-square&columns=backlog,gathering%20opinions)](https://waffle.io/oleg-py/better-monadic-for)


A Scala compiler plugin to give patterns and for-comprehensions the love they deserve

## Getting started
Currently builds are distributed using JitPack
```
resolvers += "JitPack" at "https://jitpack.io"
addCompilerPlugin("com.github.oleg-py" %% "better-monadic-for" % "0.1.0")
```
Supports Scala 2.11 and 2.12.

# Quick taste
## Destructuring `Either` / `IO` / `Task` / `FlatMap[F]`

This plugin lets you do:
```
import cats.implicits._
import cats.effect.IO

def getCounts: IO[(Int, Int)] = ???

for {
  (x, y) <- getCounts
} yield x + y
```

With regular Scala, this desugars to:
```
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
```
getCounts
  .map(_ match { case (x, y) => x + y })
```
Removing both `withFilter` and `unchecked` on generated `map`. So the code just works.

As an added bonus, type ascriptions on left-hand side do not become an `isInstanceOf` check.

```
def getThing: IO[String] = ???

for {
  x: String <- getCounts
} yield s"Count was $x"
```

desugars directly to

```
getCounts.map((x: String) => s"Count was $x")
```

This also works with `flatMap` and `foreach`, of course.

## No silent truncation of data

This example is taken from [Scala warts post](http://www.lihaoyi.com/post/WartsoftheScalaProgrammingLanguage.html#conflating-total-destructuring-with-partial-pattern-matching) by @lihaoyi
```
// Truncates 5
for((a, b) <- Seq(1 -> 2, 3 -> 4, 5)) yield a + " " +  b

// Throws MatchError
Seq(1 -> 2, 3 -> 4, 5).map{case (a, b) => a + " " + b}
```

With the plugin, both versions are equivalent and result in `MatchError`

## Match warnings
Generators will now show exhaustivity warnings now whenever regular pattern matches would:

```
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

# Notes
- This plugin introduces no extra identifiers. It only affects the behavior of for-comprehension.
- Regular `if` guards are not affected, only generator arrows.



# License
MIT
