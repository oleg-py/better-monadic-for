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
      val r = for {
        a <- new TupleChecker(4)
        b = 42
        c <- new TupleChecker(6)
      } yield a + b + c

      assert(r.a == 52)
    }

//    "for multiple bindings" in {
//      val r = for {
//        a <- new TupleChecker(4)
//        b0 = 42
//        b1 = 42
//        b2 = 42
//        b3 = 42
//        b4 = 42
//        c <- new TupleChecker(6)
//      } yield a + b0 + c + b4
//      assert(r.a == 94)
//    }
//
//    "for too many bindings" in {
//      val r = for {
//        _ <- new TupleChecker(4)
//        a = 0
//        b = 1
//        c = 2
//        d = 3
//        e = 4
//        f = 5
//        g = 6
//        h = 7
//        i = 8
//        j = 9
//        k = 10
//        l = 11
//        m = 12
//        n = 13
//        o = 14
//        p = 15
//        q = 16
//        r = 17
//        s = 18
//        t = 19
//        u = 20
//        v = 21
//        w = 22
//        x = 23
//        y = 24
//        z = 25
//        a0 = "0"
//        b0 = "1"
//        c0 = "2"
//        d0 = "3"
//        e0 = "4"
//        f0 = "5"
//        g0 = "6"
//        h0 = "7"
//        i0 = "8"
//        j0 = "9"
//        k0 = "10"
//        l0 = "11"
//        m0 = "12"
//        n0 = "13"
//        o0 = "14"
//        p0 = "15"
//        q0 = "16"
//        r0 = "17"
//        s0 = "18"
//        t0 = "19"
//        u0 = "20"
//        v0 = "21"
//        w0 = "22"
//        x0 = "23"
//        y0 = "24"
//        z0 = "25"
//      } yield s"$a$z$z0"
//
//      assert(r.a == "02525")
//    }

    "for Either in 2.11" in {
      def mkRight(a: Int): Either[String, Int] = Right(a)
      for {
        x <- mkRight(4).right
        tmp = x * x
        y <- mkRight(1).right
      } yield tmp + y
    }
  }

  "not break destructuring in bindings" in {
    val opt = for {
      tmp <- Option((1, 2))
      (x, _) = tmp
    } yield x
    assert(opt.contains(1))
  }

  "not break if guards" in {
    val ns = for {
      n <- List(1,2,3)
      plusOne = n + 1
      if plusOne - 1 == 2
    } yield plusOne

    val res = for {
      bool <- List(true, false, false, true)
      bool2 = !bool
      if bool2
    } yield bool2
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