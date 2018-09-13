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
        case vd @ ValDef(_, TermName(nm), _, Ident(TermName(ident))) if nm contains "$implicit" =>
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

  private def mkValDef(nm: String, tnm: Tree): ValDef = {
    implicit val fnc = currentFreshNameCreator
    ValDef(
      Modifiers(Flag.IMPLICIT | Flag.ARTIFACT | Flag.SYNTHETIC),
      freshTermName(nm + "$implicit$"),
      tnm,
      Ident(TermName(nm))
    )
  }

  object ImplicitPatternVals {
    def unapply(arg: Tree): Option[(Tree, List[ValDef])] = arg match {
      case HasImplicitPattern() =>
        val vals = arg.collect {
          case q"implicit0(${Bind(TermName(nm), Typed(_, tpt))})" =>
            mkValDef(nm, tpt)
        }
        // We're done with implicit0 "keyword", exterminate it
        Some((StripImplicitZero.transform(arg), vals))
      case _ => None
    }
  }

  object HasImplicitPattern {
    def unapply(arg: Tree): Boolean = arg.exists {
      case q"implicit0(${Bind(t: TermName, Typed(Ident(termNames.WILDCARD), _))})" if t != termNames.WILDCARD =>
        true
      case q"implicit0($_)" =>
        reporter.error(arg.pos, "implicit pattern only supports identifier with type pattern")
        false
      case q"implicit0(..$_)" =>
        reporter.error(arg.pos, "implicit pattern only accepts a single parameter")
        false
      case _ =>
        false
    }
  }

  object StripImplicitZero extends Transformer {
    override def transform(tree: Tree): Tree = tree match {
      case q"implicit0(${Bind(t: TermName, _)})" => super.transform(Bind(t, Ident(termNames.WILDCARD)))
      case _ => super.transform(tree)
    }
  }

  object NonLocalImplicits {
    def unapply(vd: ValDef): Option[List[ValDef]] = {
      vd.rhs match {
        case Match(_, CaseDef(pat, _, _) :: Nil) if vd.mods.hasFlag(Flag.ARTIFACT) =>
          val vd = pat.collect {
            case q"implicit0(${Bind(TermName(nm), Typed(_, tpt))})" =>
              mkValDef(nm, tpt)
          }
          if (vd.nonEmpty) Some(vd) else None
        case _ => None
      }
    }
  }
}
