package com.olegpy.bm4

import org.scalatest.FreeSpec

class TestImplicitPatterns extends FreeSpec {
  case class ImplicitTest(id: String)

  // Make IDE happy
  object implicit0 { def unapply[A](a: A) = Some(a) }

  "Implicit patterns support" - {
    "for-comprehensions" - {
      "without type ascription" - {
        "as <- binding" in {
          for {
            x <- Option(42)
            implicit0(it) <- Option(ImplicitTest("eggs"))
            _ <- Option("dummy")
            _ = "dummy"
            _ = assert(implicitly[ImplicitTest] eq it)
          } yield "ok"
        }

        "as = binding" - {
          "followed by multiple = bindings" in {
            for {
              x <- Option(42)
              _ <- Option("dummy")
              implicit0(it) = ImplicitTest("eggs")
              _ = "dummy"
              _ = assert(implicitly[ImplicitTest] eq it)
            } yield "ok"
          }

          "followed by <- bindings then = bindings" in {
            for {
              x <- Option(42)
              _ <- Option("dummy")
              implicit0(it) = ImplicitTest("eggs")
              _ <- Option("dummy")
              _ = assert(implicitly[ImplicitTest] eq it)
            } yield "ok"
          }

          "followed by = bindings then <- bindings" in {
            for {
              x <- Option(42)
              _ <- Option("dummy")
              implicit0(it) = ImplicitTest("eggs")
              _ = assert(implicitly[ImplicitTest] eq it)
              _ <- Option("dummy")
            } yield "ok"
          }
        }
      }
    }

    "match clauses" - {
      "without type ascription" in {
        (1, "foo", ImplicitTest("eggs")) match {
          case (_, "foo", implicit0(it)) =>
            assert(implicitly[ImplicitTest] eq it)
        }
      }
    }
  }
}
