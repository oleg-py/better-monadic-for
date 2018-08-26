package com.olegpy.bm4


trait ImplicitPatterns extends TreeUtils {
  import global._

  def implicitPatterns: Boolean

  object ImplicitPatternDefinition {
    def unapply(tree: Tree): Option[CaseDef] = tree match {
      case _ if !implicitPatterns =>
        None
      case CaseDef(ImplicitPatternVals(patterns, valDefns), guard, body) =>
        val newGuard = if (guard.isEmpty) guard else q"{..$valDefns; $guard}"
        println(q"{..$valDefns; $body}")
        Some(replaceTree(
          tree,
          CaseDef(patterns, newGuard, q"{..$valDefns; $body}")
        ))
      case _ =>
        None
    }
  }

  object ImplicitPatternVals {
    def unapply(arg: Tree): Option[(Tree, List[ValDef])] = arg match {
      case HasImplicitPattern() =>
        val vals = arg.collect {
          case q"implicit0(${Bind(TermName(nm), _)})" =>
            implicit val fnc = currentFreshNameCreator
            ValDef(Modifiers(Flag.IMPLICIT), freshTermName(nm), SingletonTypeTree(Ident(TermName(nm))), Ident(TermName(nm)))
        }
        // We're done with implicit0 "keyword", exterminate it
        Some((StripImplicitZero.transform(arg), vals))
      case _ => None
    }
  }

  object HasImplicitPattern {
    def unapply(arg: Tree): Boolean = arg.exists {
      // TODO: support implicit0(x: Type)
      case q"implicit0(${t: TermName})" if t != termNames.WILDCARD => true
      case q"implicit0($_)" => abort("implicit pattern only supports identifier pattern")
      case q"implicit0(..$_)" => abort("implicit pattern only accepts a single parameter")
      case _ => false
    }
  }

  object StripImplicitZero extends Transformer {
    override def transform(tree: Tree): Tree = tree match {
      case q"implicit0($x)" => super.transform(x)
      case _ => super.transform(tree)
    }
  }
}
