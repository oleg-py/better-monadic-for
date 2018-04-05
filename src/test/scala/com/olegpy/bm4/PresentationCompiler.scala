package com.olegpy.bm4

import org.ensime.pcplod._
import org.scalatest.FreeSpec

class PresentationCompiler extends FreeSpec {
  "PC should have no errors" in {
    withMrPlod("Arrows.scala") { pc =>
      assert(pc.messages.isEmpty)
    }
  }

  "PC should be able to read values & types properly" in {
    withMrPlod("Arrows.scala") { pc =>
      assert(pc.symbolAtPoint('flatmap).contains("PcTest.xx"))
      assert(pc.typeAtPoint('flatmap).contains("Int"))
      assert(pc.symbolAtPoint('map).contains("PcTest.yy"))
      assert(pc.typeAtPoint('map).contains("Int"))
    }
  }
}
