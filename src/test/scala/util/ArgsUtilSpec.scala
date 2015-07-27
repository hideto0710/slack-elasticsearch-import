package util

import org.scalatest.{FlatSpec, Matchers}

class ArgsUtilSpec() extends FlatSpec() with Matchers {

  "An ArgsUtil" should "be able to get --from --to arguments" in {
    val optionList = List("--from", "10", "1", "--to", "299")
    ArgsUtil.nextOption(optionList) should be (Map('from -> "10", 'to -> "299"))
  }

  it should "not be able to get arguments" in {
    val optionList = List("from", "10", "to", "299", "-channel", "all")
    ArgsUtil.nextOption(optionList) should be (Map())
  }
}
