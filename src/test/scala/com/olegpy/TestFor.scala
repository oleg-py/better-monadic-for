package com.olegpy

import utest._




object TestFor {
  final class IO[A](val run: () => A) {
    def map[B](f: A => B): IO[B] = IO(f(run()))
    def flatMap[B](f: A => IO[B]): IO[B] = IO(f(run()).run())
  }

  object IO {
    def apply[A](f: => A): IO[A] = new IO(() => f)
  }

  // These both show exhaustive warnings
  println((for {
    (a, Nil) <- IO((1, List(1)))
    (i, j, k::Nil) <- IO((1, 2, List()))
  } yield i).run())

  for {
    (a, Nil) <- List((1, List(1)))
    (i, j, k::Nil) <- List((1, 2, List()))
  } yield ()

  // And foreach does too
  for {
    (a, Nil) <- List((1, List(1)))
    (i, j, k::Nil) <- List((1, 2, List()))
  } ()

  // And `if` clause is still there, no warnings
  for {
    i <- 1 to 20
    if i % 2 == 1
  } println(i)

  for {
    a: Int <- IO(42)
  } yield a
}
