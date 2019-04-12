package com.olegpy.bm4

import scala.reflect.internal.{Definitions, ModifierFlags, SymbolTable}
import scala.tools.nsc.Global
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers


trait NoMapIdentity extends TreeUtils {
  import global._

  def noMapIdentity: Boolean

  object NoMapIdentity {
    def unapply(tree: Tree): Option[Tree] = tree match {
      case _ if !noMapIdentity => None

      // plain monomorphic map
      case q"${sel @ q"$body.map"}[..$_](${IdentityFunction()})"
        if sel.hasAttachment[ForAttachment.type] &&
          tree.tpe =:= body.tpe =>
        Some(replaceTree(tree, body))

      // map with implicit parameters
      case q"${sel @ q"$body.map"}[..$_](${IdentityFunction()})(..$_)"
        if sel.hasAttachment[ForAttachment.type] &&
          tree.tpe =:= body.tpe =>
        Some(replaceTree(tree, body))

      // map on implicit conversion with implicit parameters (e.g. simulacrum ops)
      case q"${sel @ q"$_($body)(..$_).map"}[..$_](${IdentityFunction()})"
        if sel.hasAttachment[ForAttachment.type] &&
          body.tpe.widen =:= tree.tpe // body.tpe will be inferred to singleton type
        =>
        Some(replaceTree(tree, body))
      case _ =>
        None
    }
  }

  object IdentityFunction {
    def unapply(tree: Tree): Boolean = tree match {
      case Function(ValDef(mods, TermName(x), tpt, _) :: Nil, i @ Ident(TermName(x2)))
        if (x == x2) &&
          mods.flags == ModifierFlags.PARAM &&
          (i.tpe =:= tpt.tpe) =>
        true
      case Function(ValDef(_, _, tpt, EmptyTree) :: Nil, LiteralUnit(u))
        if tpt.tpe =:= definitions.UnitTpe && tpt.tpe =:= u.tpe =>
        true
      case _ =>
        false
    }
  }

  object LiteralUnit {
    def unapply(tree: Tree): Option[Tree] = tree match {
      case u @ Literal(Constant(())) =>
        Some(u)

      // In Scala 2:13, we get `(x$1: Unit @unchecked) match { case _ => () }`
      case u @ Match(Typed(Ident(TermName(x)), tpt),
                     List(
                       CaseDef(Ident(termNames.WILDCARD), EmptyTree, Literal(Constant(())))))
        if tpt.tpe =:= definitions.UnitTpe =>
        Some(u)

      case _ =>
        None
    }
  }
}
