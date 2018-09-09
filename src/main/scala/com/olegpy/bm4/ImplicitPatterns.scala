package com.olegpy.bm4

import scala.collection.mutable
import scala.reflect.internal.{Definitions, SymbolTable}


trait ImplicitPatterns extends TreeUtils { self =>
  import global._

  def implicitPatterns: Boolean



  object ImplicitPatternDefinition {
    lazy val ut = new NoTupleBinding {
      val noTupling: Boolean = false
      lazy val global = self.global
    }

    def unapply(tree: Tree): Option[Tree] = tree match {
      case _ if !implicitPatterns =>
        None
      case CaseDef(ImplicitPatternVals(patterns, valDefns), guard, body) =>
        val newGuard = if (guard.isEmpty) guard else q"{..$valDefns; $guard}"
        Some(replaceTree(
          tree,
          CaseDef(patterns, newGuard, q"{..$valDefns; $body}")
        ))

      case q"$main.map(${tupler @ ut.Tupler(_, _)}).${m @ ut.Untuplable()}(${body @ ut.Untupler(_, _)})" if ForArtifact(tree) =>
        body match {
          case Function(_, Match(_, List(ImplicitPatternVals(_, _)))) =>
            abort("GOT TUPLED")
          case _ => None
        }

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

  object ImplicitPatCheck {
    def unapply(arg: Tree): Option[TermName] = arg match {
      // TODO: support implicit0(x: Type)
      case q"implicit0(${t: TermName})" if t != termNames.WILDCARD => Some(t)
      case q"implicit0($_)" => abort("implicit pattern only supports identifier pattern")
      case q"implicit0(..$_)" => abort("implicit pattern only accepts a single parameter")
      case _ => None
    }
  }

  object HasImplicitPattern {
    def unapply(arg: Tree): Boolean = arg.exists {
      case ImplicitPatCheck(_) => true
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
