package com.olegpy.bm4

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.{Transform, TypingTransformers}


class BetterMonadicFor(val global: Global) extends Plugin {
  val name = "bm4"
  val description = "Remove withFilter / partial matches in for-comprehension"
  lazy val components =
    new ForRewriter(this, global) ::
    new MapRemover(this, global) ::
    new TupleRemover(this, global) ::
    Nil

  var noUncheckedFilter = true
  var noMapIdentity     = true
  var noTupling         = true
  var implicitPatterns  = true

  val knobs = Map(
    "no-filtering" -> "Remove .withFilter from generator desugaring",
    "no-map-id"    -> "Remove .map(x => x) and .map(_ => ())",
    "no-tupling"   -> "Change desugaring of = bindings to not use tuples where possible",
    "implicit-patterns" -> "Enable syntax for implicit definitions inside patterns"
  )


  override val optionsHelp: Option[String] = Some(
    knobs
      .map { case (key, help) =>
        s"  -P:$name:$key:(y/n)".padTo(31, ' ') ++ help
      }
      .mkString(System.lineSeparator)
  )

  override def init(options: List[String], error: String => Unit): Boolean = {
    val (known, unknown) = options.partition(s => knobs.keys.exists(s.startsWith))
    if (unknown.nonEmpty) {
      error(s"Unknown options: ${unknown.mkString(", ")}")
      return false
    }

    val toBoolean = (txt: String) => txt.toLowerCase match {
      case "y" | "yes" | "1" | "true"  => true
      case "n" | "no"  | "0" | "false" => false
      case _ =>
        error(s"Unknown boolean value $txt")
        return false
    }

    for {
      key <- known
      _ = if (!key.contains(':')) {
        error(s"Option $key does not include the parameter (e.g. $key:y)")
        return false
      }
      Array(prefix, value) = key.split(":", 2)
    } prefix match {
      case "no-filtering" => noUncheckedFilter = toBoolean(value)
      case "no-map-id"    => noMapIdentity     = toBoolean(value)
      case "no-tupling"   => noTupling         = toBoolean(value)
      case "implicit-patterns" => implicitPatterns = toBoolean(value)
    }

    noUncheckedFilter || noMapIdentity || noTupling || implicitPatterns
  }
}

class ForRewriter(plugin: BetterMonadicFor, val global: Global)
  extends PluginComponent with Transform with TypingTransformers
    with NoUncheckedFilter with ImplicitPatterns
{

  import global._

  override def implicitPatterns: Boolean = plugin.implicitPatterns
  override val noUncheckedFilter: Boolean = plugin.noUncheckedFilter

  val runsAfter = "parser" :: Nil
  override val runsRightAfter = Some("parser")
  override val runsBefore = "namer" :: Nil
  val phaseName = "bm4-parser"

  def newTransformer(unit: CompilationUnit): Transformer = new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    // The magic happens in `unapply` of objects defined in mixed in traits
    override def transform(tree: Tree): Tree = tree match {
      case NoUncheckedFilter(cleaned) =>
        transform(cleaned)
      case ImplicitPatternDefinition(updated) =>
        transform(updated)
      case _ =>
        super.transform(tree)
    }
  }
}

class MapRemover(plugin: BetterMonadicFor, val global: Global)
   extends PluginComponent with Transform with TypingTransformers
     with NoMapIdentity
{
  import global._


  protected def newTransformer(unit: global.CompilationUnit) =
    new MapIdentityRemoveTransformer(unit)

  def noMapIdentity = plugin.noMapIdentity

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

class TupleRemover(plugin: BetterMonadicFor, val global: Global)
  extends PluginComponent with Transform with TypingTransformers
  with NoTupleBinding {
  import global._
  protected def newTransformer(unit: global.CompilationUnit): Transformer =
    new TupleRemoveTransformer(unit)

  def noTupling: Boolean = plugin.noTupling
  val phaseName: String = "bm4-parser2"
  val runsAfter: List[String] = "parser" :: "bm4-parser" :: Nil
  override val runsRightAfter: Option[String] = Some("bm4-parser")
  override val runsBefore: List[String] = "patmat" :: Nil

  class TupleRemoveTransformer(unit: CompilationUnit)
    extends TypingTransformer(unit)
  {
    override def transform(tree: Tree): Tree = tree match {
      case NoTupleBinding(cleaned) =>
        transform(cleaned)
      case _ =>
        super.transform(tree)
    }
  }
}
