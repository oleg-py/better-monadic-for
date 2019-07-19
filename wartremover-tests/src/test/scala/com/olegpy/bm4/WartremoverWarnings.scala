package com.olegpy.bm4

import org.scalatest.freespec.AnyFreeSpec


class WartremoverWarnings extends AnyFreeSpec {
  "Wartremover should not complain" in {
    def right[A, B](b: B): Either[A, B] = Right(b)
    case class Foo(a: Long)
    for {
      Foo(a) <- right[String, Foo](Foo(1))
      b <- right(Foo(2))
      Foo(c) = b
    } yield a + c
  }
}
