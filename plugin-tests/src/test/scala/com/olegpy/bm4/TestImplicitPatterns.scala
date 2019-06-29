package com.olegpy.bm4

import org.scalatest.FreeSpec

object CTTest {
  def foo[A]: Mo[A] = for {
    implicit0(a: A) <- Mo.delay(null.asInstanceOf[A])
  } yield implicitly[A]
}

class TestImplicitPatterns extends FreeSpec {
  case class ImplicitTest(id: String)
  def typed[A](a: A) = ()
  case class ImplicitTest2(id: String)

  // Make IDE happy
//  object implicit0 { def unapply[A](a: A) = Some(a) }

  "Implicit patterns support" - {
    "for-comprehensions with plain types" - {
      "with type ascription" - {
        "as <- binding" in {
          for {
            x <- Option(42)
            implicit0(it: ImplicitTest) <- Option(ImplicitTest("eggs"))
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
              implicit0(it: ImplicitTest) = ImplicitTest("eggs")
              _ = "dummy"
              _ = assert(implicitly[ImplicitTest] eq it)
            } yield "ok"
          }

          "followed by <- bindings then = bindings" in {
            for {
              x <- Option(42)
              _ <- Option("dummy")
              implicit0(it: ImplicitTest) = ImplicitTest("eggs")
              _ <- Option("dummy")
              _ = assert(implicitly[ImplicitTest] eq it)
            } yield "ok"
          }

          "followed by = bindings then <- bindings" in {
            for {
              x <- Option(42)
              _ <- Option("dummy")
              implicit0(it: ImplicitTest) = ImplicitTest("eggs")
              _ = assert(implicitly[ImplicitTest] eq it)
              _ <- Option("dummy")
            } yield "ok"
          }

          "with multiple implicit variables" - {
            "mixed bindings" in {
              for {
                _ <- Option("dummy")
                implicit0(it: ImplicitTest) = ImplicitTest("eggs")
                implicit0(it1: ImplicitTest2) <- Option(ImplicitTest2("42"))
                _ = assert(implicitly[ImplicitTest] eq it)
                _ = assert(implicitly[ImplicitTest2] eq it1)
              } yield "ok"
            }

            "<- bindings" in {
              for {
                implicit0(it1: ImplicitTest2) <- Option(ImplicitTest2("42"))
                implicit0(it: ImplicitTest) <- Option(ImplicitTest("eggs"))
                _ = assert(implicitly[ImplicitTest] eq it)
                _ = assert(implicitly[ImplicitTest2] eq it1)
              } yield "ok"
            }
          }
        }
      }
    }

    "match clauses" - {
      "with type ascription" in {
        (1, "foo", ImplicitTest("eggs")) match {
          case (_, "foo", implicit0(it: ImplicitTest)) =>
            assert(implicitly[ImplicitTest] eq it)
        }
      }
    }
  }
}
