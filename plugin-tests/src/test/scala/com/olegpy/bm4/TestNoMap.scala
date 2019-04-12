package com.olegpy.bm4

import org.scalatest.FreeSpec

case class MapCalled() extends Exception

class MapCheck[+A](a: A) {
  def map[B](f: A => B): MapCheck[B] = throw MapCalled()
  def flatMap[B](f: A => MapCheck[B]): MapCheck[B] = f(a)
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
}
