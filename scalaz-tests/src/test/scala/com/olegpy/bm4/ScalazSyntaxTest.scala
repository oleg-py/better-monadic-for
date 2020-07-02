package com.olegpy.bm4

import org.scalatest.freespec.AnyFreeSpec
import scalaz._, Scalaz._

class ScalazSyntaxTest extends AnyFreeSpec {
  implicit val scalazInstance: Bind[MapCheck] = new Bind[MapCheck] {
    def bind[A, B](fa: MapCheck[A])(f: A => MapCheck[B]): MapCheck[B] = fa.flatMap(f)

    def map[A, B](fa: MapCheck[A])(f: A => B): MapCheck[B] = fa.map(f)
  }

  "works in generic context with extension methods of scalaz 7" in {
    def sillyTest[F[_]: Bind](fa: F[Int], fb: F[Int]) =
      for {
        _ <- fa
        b <- fb
      } yield b

    sillyTest(new MapCheck(11), new MapCheck(42))
  }
}
