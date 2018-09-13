package com.olegpy.bm4

import cats.effect.IO
import org.scalatest.FreeSpec
import scalaz.Monad
import scalaz.syntax.monad._
import scalaz.std.option._

object CTTest {
  def foo[A]: IO[A] = for {
    implicit0(a: A) <- IO(null.asInstanceOf[A])
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
      "without type ascription" - {
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

    "for-comprehensions with type ctors" - {
      "= bindings" in {
        def f[F[_]: Monad] =
          for {
            x <- 42.point[F]
            _ <- "dummy".point[F]
            implicit0(it: ImplicitTest) = ImplicitTest("eggs")
            str = "dummy"
            _ = typed[String](str)
          } yield assert(it eq implicitly[ImplicitTest])

        f[Option]
      }
    }

    "match clauses" - {
      "without type ascription" in {
        (1, "foo", ImplicitTest("eggs")) match {
          case (_, "foo", implicit0(it: ImplicitTest)) =>
            assert(implicitly[ImplicitTest] eq it)
        }
      }
    }
  }
}
