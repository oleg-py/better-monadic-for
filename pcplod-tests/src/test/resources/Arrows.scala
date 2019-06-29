object PcTest {
  def mkRight[A](a: A) = Right[Throwable, A](a).right

  for {
    x@flatmap@x: Int <- mkRight(66)
    (_, y@map@y) <- mkRight((11, 42))
  } yield xx + yy
}
