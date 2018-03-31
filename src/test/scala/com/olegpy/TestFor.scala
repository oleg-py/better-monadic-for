package com.olegpy

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import cats.effect.IO
import monix.execution.Scheduler.Implicits.global
import monix.eval.Task
import utest._
import cats.implicits._



object TestFor extends TestSuite {
  val tests = Tests {
    'allowing - {
      "destructuring for monads without withFilter" - {
        val task = for {
          (a, b) <- Task.zip2(Task("Hello"), Task("there"))
        } yield s"$a $b"

        assert(task.runSyncUnsafe(Duration.Inf) == "Hello there")
      }

      "also with lots of generators inside" - {
        val io = for {
          _ <- IO.unit
          (a, b) <- IO((1, 2))
          _ <- IO.unit
          s <- IO("Output")
          Some(sep) <- IO(Some(": "))
        } yield s"$s$sep${a + b}"
        assert(io.unsafeRunSync() == "Output: 3")
      }

      "and with nested for-s" - {
        val task = for {
          (a, b) <- Task.zip2(Task("Hello"), Task("there"))
          txt = s"$a $b!"
          io = for ((c, d) <- IO(("General", "Kenobi"))) yield s"$c $d!"
          txt2 <- io.to[Task]
        } yield s"$txt $txt2"

        assert(task.runSyncUnsafe(Duration.Inf) == "Hello there! General Kenobi!")
      }

      "easy type patterns on left hand side" - {
        * -  (for (x: Int <- IO(42)) yield x)
      }

      "traversing through deeply nested definitions" - {
        object A {
          class B {
            val c: Unit = {
              def localMethod = for (x: Int <- IO(42)) yield x
              ()
            }
          }
        }
      }
    }

    // TODO: utest compileError does not use plugin
    "exhaustiveness checks" - {
      * - {
        for (Some(x) <- IO(none[Int])) yield x
      }
      * - {
        object Singleton
        for (Singleton <- IO(Singleton); (a, Nil) <- IO((1, List(1)))) yield a
      }
    }

    'preserving - {
      "if guards for things supporting it" - {
        val res = for {
          bool <- List(true, false, false, true)
          if bool
        } yield bool

        assert(res == List(true, true))
      }
    }
  }
}
