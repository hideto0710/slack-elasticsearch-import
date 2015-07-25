package util

import scala.annotation.tailrec

object ArgsUtil {

  private type OptionMap = Map[Symbol, String]

  // TODO: 自動で選択可能な引数を決定したい。
  @tailrec
  def nextOption(list: List[String], map: OptionMap = Map()): OptionMap = list match {
    case "--from" :: value :: tail =>
      nextOption(tail, map ++ Map('from -> value))
    case "--to" :: value :: tail =>
      nextOption(tail, map ++ Map('to -> value))
    case "--channel" :: value :: tail =>
      nextOption(tail, map ++ Map('channel -> value))
    case unknownOption :: tail =>
      nextOption(tail, map)
    case _ => map
  }

}
