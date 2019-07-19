object Wildcards {
  val a = for {
    implicit0(_: Int) <- Option(42)
  } yield "ok"

  val b = for {
    implicit0(_) <- Option(42)
  } yield "ok"
}
