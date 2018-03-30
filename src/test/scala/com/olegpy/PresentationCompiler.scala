package com.olegpy

import utest._
import org.ensime.pcplod._

object PresentationCompiler extends TestSuite {
  val tests = Tests {
    "PC should have no errors" - {
      withMrPlod("Arrows.scala") { pc =>
        assert(pc.messages.isEmpty)
      }
    }

    "PC should be able to read values & types properly" - {
      withMrPlod("Arrows.scala") { pc =>
        assert(pc.symbolAtPoint('flatmap).contains("PcTest.xx"))
        assert(pc.typeAtPoint('flatmap).contains("Int"))
        assert(pc.symbolAtPoint('map).contains("PcTest.yy"))
        assert(pc.typeAtPoint('map).contains("Int"))
      }
    }
  }
}
