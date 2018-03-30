package com.olegpy

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.{Transform, TypingTransformers}


class BetterMonadicFor(val global: Global) extends Plugin {
  val name = "better-monadic-for"
  val description = "Remove withFilter / partial matches in for-comprehension"
  val components = new ForRewriter(this, global) :: Nil
}

class ForRewriter(plugin: Plugin, val global: Global)
  extends PluginComponent with Transform with TypingTransformers {

  import global._

  val runsAfter = "parser" :: Nil
  override val runsBefore = "namer" :: Nil
  val phaseName = "better-monadic-for"

  def newTransformer(unit: CompilationUnit): Transformer = new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {

    def inheritPos(tree: Tree)(modified: Tree) =
      atPos(tree.pos.makeTransparent)(modified)

    // Transformer does not go deep into blocks by default.
    // Not sure if I'm extending the wrong class
    override def transform(tree: Tree): Tree = tree match {
      // The magic happens in `unapply`:
      case NoWithFilter(cleaned) =>
        val r = transform(cleaned)
        //println(r)
        inheritPos(tree)(r)
      case _ =>
        super.transform(tree)
    }

    object UnsafeCandidate {
      private val set = Set("map", "flatMap", "foreach")
      def unapply(n: Name): Boolean = set contains n.toString
    }

    object UncheckedFilter {
      // Matches if the function is a lambda passed to `withFilter`
      // Rules are:
      // - The body is a match where parameter is annotated with @scala.unchecked
      // - It's variable name contains "$ifrefutable"
      def unapply(f: Function): Boolean = f match {
        case Function(_, Match(Annotated(q"new scala.unchecked()", Ident(w)), _))
        if w.toString.contains("$ifrefutable") =>
          true
        case _ => false
      }
    }

    object NoUnchecked {
      def unapply(implicit f: Function): Option[Tree] = f match {
        // Bring exhaustivity warnings back to patterns
        case Function(a, Match(Annotated(q"new scala.unchecked()", arg), body)) =>
          Some(inheritPos(f)(Function(a, Match(arg, body))))

        // @unchecked is not emitted in cases like a: Int <- List(1, 2, 3)
        // So preserve the function, it's fine.
        case _ => Some(f)
      }
    }

    object NoWithFilter {
      def unapply(implicit t: Tree): Option[Tree] = t match {
        case q"$a.withFilter(${UncheckedFilter()}).${n @ UnsafeCandidate()}(${NoUnchecked(g)})" =>
          Some(inheritPos(t)(q"$a.$n($g)"))
        case _ => None
      }
    }
  }
}