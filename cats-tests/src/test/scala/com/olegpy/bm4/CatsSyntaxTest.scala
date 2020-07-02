package com.olegpy.bm4

import cats.Monad
import org.scalatest.freespec.AnyFreeSpec
import cats.implicits._

class CatsSyntaxTest extends AnyFreeSpec {
  implicit val mcCatsInstance: cats.FlatMap[MapCheck] = new cats.FlatMap[MapCheck] {
    def flatMap[A, B](fa: MapCheck[A])(f: A => MapCheck[B]): MapCheck[B] = {
      fa.flatMap(f)
    }

    def tailRecM[A, B](a: A)(f: A => MapCheck[Either[A, B]]): MapCheck[B] = {
      fail()
    }
    def map[A, B](fa: MapCheck[A])(f: A => B): MapCheck[B] = fa.map(f)
  }
  case class ImplicitTest(id: String)
  def typed[A](a: A) = ()

  "works in generic context with extension methods of cats" in {
    import cats.syntax.all._
    def sillyTest[F[_]: cats.FlatMap](fa: F[Int], fb: F[Int]) =
      for {
        _ <- fa
        b <- fb
      } yield b

    sillyTest(new MapCheck(11), new MapCheck(42))
  }

  "supports implicit0 in F-parametric methods" - {
    "in = bindings" in {
      def f[F[_]: Monad] =
        for {
          x <- 42.pure[F]
          _ <- "dummy".pure[F]
          implicit0(it: ImplicitTest) = ImplicitTest("eggs")
          str = "dummy"
          _ = typed[String](str)
        } yield assert(it eq implicitly[ImplicitTest])

      f[Option]
    }
  }
}
