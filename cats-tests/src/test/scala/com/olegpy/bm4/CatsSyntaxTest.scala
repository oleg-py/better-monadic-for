package com.olegpy.bm4

import org.scalatest.FreeSpec

class CatsSyntaxTest extends FreeSpec {
  implicit val catsInstance: cats.FlatMap[MapCheck] = new cats.FlatMap[MapCheck] {
    def flatMap[A, B](fa: MapCheck[A])(f: A => MapCheck[B]): MapCheck[B] = {
      fa.flatMap(f)
    }

    def tailRecM[A, B](a: A)(f: A => MapCheck[Either[A, B]]): MapCheck[B] = {
      assert(false)
      null
    }
    def map[A, B](fa: MapCheck[A])(f: A => B): MapCheck[B] = fa.map(f)
  }

  "works in generic context with extension methods of cats" in {
    import cats.syntax.all._
    def sillyTest[F[_]: cats.FlatMap](fa: F[Int], fb: F[Int]) =
      for {
        _ <- fa
        b <- fb
      } yield b

    sillyTest(new MapCheck(11), new MapCheck(42))
  }
}
