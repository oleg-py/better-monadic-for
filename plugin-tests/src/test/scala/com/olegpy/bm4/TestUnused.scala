package com.olegpy.bm4

// Test for issue #12
object TestUnused extends App{
  for {
    _ <- Option(()) //parameter value x$5 in value $anonfun is never used
    _ = 0
  } yield ()
}
