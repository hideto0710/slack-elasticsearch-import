package util

import scala.annotation.tailrec

object ArgsUtil {

  private type OptionMap = Map[Symbol, String]
  private val argsPattern = """--(.+)""".r

  @tailrec
  def nextOption(list: List[String], map: OptionMap = Map()): OptionMap = list match {
    case argsPattern(p) :: value :: tail =>
      nextOption(tail, map ++ Map(Symbol(p) -> value))
    case unknownOption :: tail =>
      nextOption(tail, map)
    case _ => map
  }
}
