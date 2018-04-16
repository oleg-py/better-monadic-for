package com.olegpy.bm4


trait NoUncheckedFilter extends TreeUtils {
  import global._

  def noUncheckedFilter: Boolean

  object FollowUp {
    private val set = Set("map", "flatMap", "foreach")
    def unapply(n: Name): Boolean = set contains n.toString
  }

  object Refuter {
    // Matches if the function is a lambda passed to `withFilter`
    // Rules are:
    // - The body is a match where parameter is annotated with @scala.unchecked
    // - It's variable name starts with "check$ifrefutable$"
    def unapply(f: Function): Boolean = f match {
      case Function(_, Match(Annotated(_, Ident(w)), _))
        if w.startsWith(nme.CHECK_IF_REFUTABLE_STRING) =>
        true
      case _ => false
    }
  }

  object CleanUnchecked {
    def unapply(f: Function): Option[Tree] = f match {
      // Bring exhaustivity warnings back to patterns
      case Function(a, Match(Annotated(_, arg), body)) =>
        Some(replaceTree(f, Function(a, Match(arg, body))))

      // @unchecked is not emitted in cases like a: Int <- List(1, 2, 3)
      // So preserve the function, it's fine.
      case _ => Some(f)
    }
  }

  object NoUncheckedFilter {
    def unapply(t: Tree): Option[Tree] = t match {
      case q"$a.withFilter(${Refuter()}).${n @ FollowUp()}(${CleanUnchecked(g)})" =>
        val sel = q"$a.$n".updateAttachment(ForAttachment)
        Some(replaceTree(t, q"$sel($g)"))
      case q"$a.withFilter(${Refuter()})" =>
        Some(replaceTree(t, a))
      case _ => None
    }
  }
}
