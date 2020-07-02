package com.olegpy.bm4

import org.ensime.pcplod._
import org.scalatest.freespec.AnyFreeSpec

class PresentationCompiler extends AnyFreeSpec {
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

  "PC should yield errors" - {
    "when implicit0(_: Tpe) is used" in {
      withMrPlod("Wildcards.scala") { pc =>
        val firstError = pc.messages.head

        assert(firstError.severity == PcMessageSeverity.Error)
        assert(firstError.message == "implicit pattern requires an identifier, but a wildcard was used: `implicit0((_: Int))`. " +
          "This doesn't introduce anything into the implicit scope. You might want to remove the implicit0 pattern and type.")
      }
    }

    "when implicit0(_) is used" in {
      withMrPlod("Wildcards.scala") { pc =>
        val secondError = pc.messages(1)

        assert(secondError.severity == PcMessageSeverity.Error)
        assert(secondError.message == "implicit pattern requires an identifier, but a wildcard was used: `implicit0(_)`. " +
          "This doesn't introduce anything into the implicit scope. You might want to remove the implicit0 pattern.")
      }
    }
  }
}
