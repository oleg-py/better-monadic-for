package com.olegpy.bm4

import org.scalatest.FreeSpec

case class MapCalled() extends Exception

class MapCheck[+A](a: A) {
  def map[B](f: A => B): MapCheck[B] = throw MapCalled()
  def flatMap[B](f: A => MapCheck[B]): MapCheck[B] = f(a)
}

object MapCheck {
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

  implicit val scalazInstance: scalaz.Bind[MapCheck] = new scalaz.Bind[MapCheck] {
    def bind[A, B](fa: MapCheck[A])(f: A => MapCheck[B]): MapCheck[B] = fa.flatMap(f)

    def map[A, B](fa: MapCheck[A])(f: A => B): MapCheck[B] = fa.map(f)
  }
}

class TestNoMap extends FreeSpec {
  "emits no map(b => b) in for-comprehension" in {
    for {
      _ <- new MapCheck(42)
      b <- new MapCheck("foo")
    } yield b
  }

  "emits no map(_ => ()) if prev. operation resulted in Unit" in {
      for {
        _ <- new MapCheck(42)
        _ <- new MapCheck(())
      } yield ()
  }

  "preserves map(_ => ()) if prev.operation did not result in Unit" in {
    intercept[MapCalled] {
      for {
        _ <- new MapCheck(42)
        _ <- new MapCheck("Foo")
      } yield ()
    }
  }

  "removes map(b: Ascribed => b) if it's provable identity" in {
    for {
      _ <- new MapCheck(42)
      b: Long <- new MapCheck(0L)
    } yield b
  }

  "preserves map(b: Ascribed => b) if widening" in {
    intercept[MapCalled] {
      for {
        _ <- new MapCheck(42)
        b: Any <- new MapCheck("foo")
      } yield b
    }
  }

  "preserves map if called manually" in {
    intercept[MapCalled] {
      new MapCheck(()).flatMap(y => new MapCheck(()).map(x => x))
    }
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

  "works in generic context with extension methods of scalaz 7" in {
    import scalaz._, Scalaz._
    def sillyTest[F[_]: Bind](fa: F[Int], fb: F[Int]) =
      for {
        _ <- fa
        b <- fb
      } yield b

    sillyTest(new MapCheck(11), new MapCheck(42))
  }
}
