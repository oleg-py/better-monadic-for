package com.olegpy.bm4


trait NoTupleBinding extends TreeUtils {
  import global._
  def noTupling: Boolean

  object NoTupleBinding {
    def unapply(arg: Tree): Option[Tree] = arg match {
      case _ if !noTupling => None
      case TupleBinding(Tupled(main, method, param, vals, used, result)) =>
        val usedVal: Set[String] = used.collect {
          case Bind(TermName(str), _) => str
        }.toSet
        val noUnusedVals = vals.mapConserve {
          case ValDef(_, TermName(s), _, rhs) if !usedVal(s) => rhs
          case a => a
        }

        val rewrite =
          q"$main.$method(($param) => { ..$noUnusedVals; $result })"
        Some(replaceTree(arg, rewrite))

      case _ => None
    }
  }

  case class Tupled(
    main: Tree,
    method: TermName,
    param: Tree,
    vals: List[Tree],
    usedNames: Tree,
    result: Tree
  )

  object TupleBinding {
    def unapply(arg: Tree): Option[Tupled] = arg match {
      case Apply(Select(TupleBinding(td @ Tupled(
      _,
      _,
      _,
      vals,
      _,
      TuplerBlock(moreVals)
      )),  TermName("map")), Untupler(used, ret) :: Nil) =>
        Some(td.copy(vals = vals ::: moreVals, result = ret, usedNames = used))

      case q"$main.map(${Tupler(param, vals)}).$m(${Untupler(used, tree)})" if ForArtifact(arg) =>
        Some(Tupled(main, m, param, vals, used, tree))

      case _ =>
        None
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

  object TuplerBlock {
    def unapply(arg: Tree): Option[List[Tree]] = arg match {
      case Block(
        valDefs, Apply(Select(Ident(TermName("scala")), tn), _)
      ) if tn.startsWith("Tuple") && valDefs.forall(_.isInstanceOf[ValDef]) =>
        Some(valDefs)
      case _ => None
    }
  }

  object Tupler {
    def unapply(arg: Tree): Option[(ValDef, List[Tree])] = arg match {
      case Function(param :: Nil, TuplerBlock(valDefs)) =>
        Some((param, valDefs))
      case _ =>
        None
    }
  }

  object Untupler {
    def unapply(arg: Tree): Option[(Tree, Tree)] = arg match {
      case Function(_ :: Nil, Match(_,
        CaseDef(pat, _, body) :: Nil
      )) =>
        Some((pat, body))
      case _ =>
        None
    }
  }
}
