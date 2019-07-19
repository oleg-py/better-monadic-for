package com.olegpy.bm4

import scala.reflect.internal.{Definitions, SymbolTable}


trait TreeUtils {
  val global: Definitions with SymbolTable
  import global._

  private val uncheckedAnnot = q"new scala.unchecked()"

  object Unchecked {
    def unapply(arg: Tree): Boolean = arg.equalsStructure(uncheckedAnnot)
  }

  def replaceTree[T <: Tree](prev: Tree, next: T): T = {
    atPos(prev.pos.makeTransparent)(next)
      .setAttachments(prev.attachments)
  }

  object ForArtifact {
    def apply(arg: Tree): Boolean =
      arg.children.exists(_.hasAttachment[ForAttachment.type])
  }
}
