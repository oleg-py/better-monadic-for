package com.olegpy.bm4

import scala.collection.mutable
import scala.reflect.internal.{Definitions, SymbolTable}


trait ImplicitPatterns extends TreeUtils { self =>
  import global._

  def implicitPatterns: Boolean



  object ImplicitPatternDefinition {
    lazy val ut = new NoTupleBinding {
      val noTupling: Boolean = false
      lazy val global: self.global.type = self.global
    }

    def embedImplicitDefs(tupler: Tree, defns: List[ValDef]): Tree = {
      val identMap = defns.map {
        case vd @ ValDef(_, _, SingletonTypeTree(Ident(TermName(ident))), _) =>
          ident -> vd
      }.toMap

      tupler match {
        case Function(vp, Block(valDefns, expr)) =>
          val withImplicits = valDefns.flatMap {
            case vd @ ValDef(_, TermName(nm), _, _) if identMap contains nm =>
              vd :: identMap(nm) :: Nil
            case vd =>
              vd :: Nil
          }

          Function(vp, Block(withImplicits, expr))

        case Block(valDefns, expr) =>
          val withImplicits = valDefns.flatMap {
            case vd @ ValDef(_, TermName(nm), _, _) if identMap contains nm =>
              vd :: identMap(nm) :: Nil
            case vd =>
              vd :: Nil
          }

          Block(withImplicits, expr)

        case other =>
          other
      }
    }

    def unapply(tree: Tree): Option[Tree] = tree match {
      case _ if !implicitPatterns =>
        None
      case CaseDef(ImplicitPatternVals(patterns, valDefns), guard, body) =>
        val newGuard = if (guard.isEmpty) guard else q"{..$valDefns; $guard}"
        val replacement = CaseDef(patterns, newGuard, q"{..$valDefns; $body}")

        Some(replaceTree(
          tree,
          replacement
        ))

      case Block((matcher @ NonLocalImplicits(valdefs)) :: stats, expr) =>
        val m = StripImplicitZero.transform(matcher)
        val replacement = embedImplicitDefs(Block(m :: stats, expr), valdefs)
        Some(replaceTree(tree, replacement))


      case q"$main.map(${tupler @ ut.Tupler(_, _)}).${m @ ut.Untuplable()}(${body @ ut.Untupler(_, _)})" if ForArtifact(tree) =>
        body match {
          case Function(_, Match(_, List(ImplicitPatternVals(_, defns)))) =>
            val t = StripImplicitZero.transform(embedImplicitDefs(tupler, defns))
            val replacement = q"$main.map($t).$m($body)"
            Some(replaceTree(tree, replacement))
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
            ValDef(Modifiers(Flag.IMPLICIT), freshTermName(nm + "$implicit$"), SingletonTypeTree(Ident(TermName(nm))), Ident(TermName(nm)))
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
      case q"implicit0(${Bind(t: TermName, Ident(termNames.WILDCARD))})" if t != termNames.WILDCARD => Some(t)
      case q"implicit0($_)" =>
        reporter.error(arg.pos, "implicit pattern only supports identifier pattern")
        None
      case q"implicit0(..$_)" =>
        reporter.error(arg.pos, "implicit pattern only accepts a single parameter")
        None
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

  object NonLocalImplicits {
    def unapply(vd: ValDef): Option[List[ValDef]] = {
      vd.rhs match {
        case Match(_, CaseDef(pat, _, _) :: Nil) if vd.mods.hasFlag(Flag.ARTIFACT) =>
          val vd = pat.collect {
            case q"implicit0(${Bind(TermName(nm), _)})" =>
              implicit val fnc = currentFreshNameCreator
              ValDef(Modifiers(Flag.IMPLICIT), freshTermName(nm + "$implicit$"), SingletonTypeTree(Ident(TermName(nm))), Ident(TermName(nm)))
          }
          if (vd.nonEmpty) Some(vd) else None
        case _ => None
      }
    }
  }
}
