package com.olegpy.bm4

import com.olegpy.bm4.TestNoTuples.TupleChecker
import org.scalatest.FreeSpec


class TestNoTuples extends FreeSpec {
  "Plugin removes tuples produced in binding" - {
    "for single definition" in {
      for {
        a <- new TupleChecker(4)
        b = 42
      } yield a + b
    }

    "for multiple generators" in {
      for {
        a <- new TupleChecker(4)
        b = 42
        c <- new TupleChecker(6)
      } yield a + b + c
    }

    "for multiple bindings" in {
      for {
        a <- new TupleChecker(4)
        b0 = 42
        b1 = 42
        b2 = 42
        b3 = 42
        b4 = 42
        c <- new TupleChecker(6)
      } yield a + b0 + c + b4
    }

    "for too many bindings" in {
      for {
        _ <- new TupleChecker(4)
        a = 0
        b = 1
        c = 2
        d = 3
        e = 4
        f = 5
        g = 6
        h = 7
        i = 8
        j = 9
        k = 10
        l = 11
        m = 12
        n = 13
        o = 14
        p = 15
        q = 16
        r = 17
        s = 18
        t = 19
        u = 20
        v = 21
        w = 22
        x = 23
        y = 24
        z = 25
      } yield a + b
    }
  }
}

object TestNoTuples {
  case class TupleFound() extends Exception("Got a tuple!") {
    throw this
  }

  class TupleChecker[A](val a: A) {
    def map[B](f: A => B): TupleChecker[B] = {
      val b = f(a)
      if (b.getClass.getCanonicalName.startsWith("scala.Tuple")) {
        TupleFound()
      }
      new TupleChecker(b)
    }

    def flatMap[B](f: A => TupleChecker[B]): TupleChecker[B] = {
      f(a)
    }
  }
}