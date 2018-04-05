package com.olegpy.bm4

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.{Transform, TypingTransformers}


class BetterMonadicFor(val global: Global) extends Plugin {
  val name = "bm4"
  val description = "Remove withFilter / partial matches in for-comprehension"
  val components =
    new ForRewriter(this, global) ::
    new MapRemover(this, global) ::
    Nil

  val noUncheckedFilter = true
  val noMapIdentity = true

  override def init(options: List[String], error: String => Unit): Boolean =
    super.init(options, error)
}

class ForRewriter(plugin: BetterMonadicFor, val global: Global)
  extends PluginComponent with Transform
    with TypingTransformers
    with NoUncheckedFilter
{

  import global._

  override val noUncheckedFilter: Boolean = plugin.noUncheckedFilter

  val runsAfter = "parser" :: Nil
  override val runsBefore = "namer" :: Nil
  val phaseName = "bm4-parser"

  def newTransformer(unit: CompilationUnit): Transformer = new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    // The magic happens in `unapply` of objects defined in mixed in traits
    override def transform(tree: Tree): Tree = tree match {
      case NoUncheckedFilter(cleaned) =>
        transform(cleaned)
      case _ =>
        super.transform(tree)
    }
  }
}

class MapRemover(plugin: BetterMonadicFor, val global: Global)
   extends PluginComponent with Transform
     with TypingTransformers
     with NoMapIdentity
{ parent =>
  import global._


  protected def newTransformer(unit: global.CompilationUnit) =
    new MapIdentityRemoveTransformer(unit)

  val noMapIdentity = true

  val phaseName = "bm4-typer"
  val runsAfter = "typer" :: Nil


  override val runsBefore: List[String] = "patmat" :: Nil

  class MapIdentityRemoveTransformer(unit: CompilationUnit)
    extends TypingTransformer(unit)
  {
    override def transform(tree: Tree): Tree = {
      tree match {
        case NoMapIdentity(cleaned) =>
          transform(cleaned)
        case _ =>
          super.transform(tree)
      }
    }
  }
}