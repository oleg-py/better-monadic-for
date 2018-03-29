package com.olegpy

import scala.tools.nsc
import nsc.{Global, Phase}
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.{InfoTransform, Transform, TypingTransformers}
import nsc.symtab.Flags._
import nsc.ast.TreeDSL
import scala.reflect.NameTransformer
import scala.collection.mutable

class BetterMonadicFor(val global: Global) extends Plugin {
  val name = "better-monadic-for"
  val description = "Remove withFilter / partial matches in for-comprehension"
  val components = new ForRewriter(this, global) :: Nil
}

class ForRewriter(plugin: Plugin, val global: Global)
  extends PluginComponent with Transform with TypingTransformers {

  import global._

  val sp = new StringParser[global.type](global)

  val runsAfter = "parser" :: Nil
  override val runsBefore = "namer" :: Nil
  val phaseName = "better-monadic-for"

  def newTransformer(unit: CompilationUnit): Transformer = new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {

    def debug(t: Tree): String = t match {
      case Literal(txt) => txt.escapedStringValue
      case Select(q, name) => s"Select(${debug(q)}, '${name.toString}')"
      case (_: Ident) => t.summaryString
      case _ =>
        s"${t.shortClass}(${t.children.map(debug).mkString(", ")})"
    }


    override def transform(tree: global.Tree): global.Tree = tree match {
      case NoWithFilter(sel) =>
        val r = transform(sel)
        println(r)
        r

      case Block(stats, expr) =>
        Block(transformTrees(stats), transform(expr))
      case e @ `pendingSuperCall` =>
        // This extends Apply, and bad things happen if you touch it
        e
      case a @ Apply(fun, args) =>
        Apply(transform(fun), transformTrees(args))
      case Select(subtree, name) =>
        Select(transform(subtree), name)
      case _ =>
        super.transform(tree)
    }

    object UnsafeCandidate {
      private val set = Set("map", "flatMap", "foreach")
      def unapply(n: Name): Boolean = set contains n.toString
    }

    object UncheckedFilter {
      def unapply(f: Function): Boolean = f match {
        case Function(_, Match(Annotated(q"new scala.unchecked()", Ident(w)), _))
        if w.toString.contains("ifrefutable") =>
          true
        case _ => false
      }
    }

    object NoUnchecked {
      def unapply(f: Function): Option[Tree] = f match {
        case Function(a, m @ Match(Annotated(q"new scala.unchecked()", arg), body)) =>
          Some(f.copy(a, m.copy(arg, body)))

        // a: Int <- List(1, 2, 3)
        case _ => Some(f)
      }

    }

    object NoWithFilter {
      def unapply(t: Tree): Option[Tree] = t match {
        case q"$a.withFilter(${f @ UncheckedFilter()}).${n @ UnsafeCandidate()}(${NoUnchecked(g)})" =>
          Some(q"$a.$n($g)")
        case _ => None
      }
    }
  }
}