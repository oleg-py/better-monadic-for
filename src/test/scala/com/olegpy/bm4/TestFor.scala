package com.olegpy.bm4

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.scalatest.{FreeSpec, FunSuite}

/** Mo is a lazy monad without `withFilter` */
sealed trait Mo[A] {
  import Mo._
  def map[B](f: A => B): Mo[B] = Return(() => f(value))
  def flatMap[B](f: A => Mo[B]): Mo[B] = Suspend(() => f(value))
  def value: A = this match {
    case Return(a) => a()
    case Suspend(fa) => fa().value
  }
}
object Mo {
  case class Return[A](a: () => A) extends Mo[A]
  case class Suspend[A](fa: () => Mo[A]) extends Mo[A]

  def apply[A](a: A): Mo[A] = delay(a)
  def delay[A](a: => A): Mo[A] = Return(() => a)
  def zip2[A, B](la: Mo[A], lb: Mo[B]): Mo[(A, B)] =
    la.flatMap(a => lb.flatMap(b => Mo((a, b))))
  val unit: Mo[Unit] = Mo(())
}

class TestFor extends FreeSpec {

  "Plugin allows" - {
    "destructuring for monads without withFilter" in {
      val mo = for {
        (a, b) <- Mo.zip2(Mo("Hello"), Mo("there"))
      } yield s"$a $b"

      assert(mo.value == "Hello there")
    }

    "also with lots of generators inside" - {
      val mo = for {
        _ <- Mo.unit
        (a, b) <- Mo((1, 2))
        _ <- Mo.unit
        s <- Mo("Output")
        Some(sep) <- Mo(Some(": "))
      } yield s"$s$sep${a + b}"
      assert(mo.value == "Output: 3")
    }

    "and with nested for-s" in {
      val mo = for {
        (a, b) <- Mo.zip2(Mo("Hello"), Mo("there"))
        txt = s"$a $b!"
        m = for ((c, d) <- Mo(("General", "Kenobi"))) yield s"$c $d!"
        txt2 <- m
      } yield s"$txt $txt2"

      assert(mo.value == "Hello there! General Kenobi!")
    }

    "easy type patterns on left hand side" - {
      "for one-liners" in (for (x: Int <- Mo(42)) yield x)
      "for multiple lines" in {
        for {
          x: Int <- Mo(42)
          s: String <- Mo("Foo")
        } yield s + x
      }
    }

    "traversing through deeply nested definitions" in {
      object A {
        class B {
          val c: Unit = {
            def localMethod = for (x: Int <- Mo(42)) yield x
            ()
          }
        }
      }
    }
  }

  // TODO: utest compileError does not use plugin
  "exhaustiveness checks" - {
    "option" in {
      for (Some(x) <- Mo.delay(Option.empty[Int])) yield x
    }
    "lists & singletons" in {
      object Singleton
      for (Singleton <- Mo(Singleton); (a, Nil) <- Mo((1, List(1)))) yield a
    }
  }

  "preserving" - {
    "if guards for things supporting it" in {
      val res = for {
        bool <- List(true, false, false, true)
        if bool
      } yield bool

      assert(res == List(true, true))
    }
  }
}
