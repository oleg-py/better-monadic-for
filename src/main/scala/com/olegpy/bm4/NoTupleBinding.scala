package com.olegpy.bm4

trait NoTupleBinding extends TreeUtils {
  import global._
  def noTupling: Boolean

  object NoTupleBinding {
    def unapply(arg: Tree): Option[Tree] = arg match {
      case _ if !noTupling => None
      case q"$thing.map(${Tupler(param, vals)}).$m(${Untupler(tree)})" if ForArtifact(arg) =>
        val rewrite =
          q"$thing.$m(($param) => { ..$vals; $tree })"
        println(arg)
        Some(replaceTree(arg, rewrite))

      case _ => None
    }
  }

  object Untuplable {
    def unapply(arg: Name): Boolean = arg match {
      case TermName("map") | TermName("flatMap") | TermName("foreach") =>
        true
      case _ =>
        false
    }
  }

  object Tupler {
    def unapply(arg: Tree): Option[(ValDef, Seq[Tree])] = arg match {
      case Function(param :: Nil, Block(
        valDefs, Apply(Select(Ident(TermName("scala")), tn), _)
      )) if tn.startsWith("Tuple") && valDefs.forall(_.isInstanceOf[ValDef]) =>
        Some((param, valDefs))
      case _ =>
        None
    }
  }

  object Untupler {
    def unapply(arg: Tree): Option[Tree] = arg match {
      case Function(_ :: Nil, Match(_,
        CaseDef(_, _, body) :: Nil
      )) =>
        Some(body)
      case _ =>
        None
    }
  }
}
