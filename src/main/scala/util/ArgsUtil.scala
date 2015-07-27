package util

import scala.annotation.tailrec

/**
 * コマンドライン引数処理用のUtility
 *
 * コマンドライン引数の処理に関わるUtilityMethodを提供する。
 * @version 0.1
 */
object ArgsUtil {

  private type OptionMap = Map[Symbol, String]
  private val argsPattern = """--(.+)""".r

  /**
   * コマンドライン引数をマッピングして返す。
   *
   * 現状、"--"で始まるものをキーとしている。
   * @param list コマンドライン引数リスト
   * @param map マッピング
   * @return マッピング結果
   */
  @tailrec
  def nextOption(list: List[String], map: OptionMap = Map()): OptionMap = list match {
    case argsPattern(p) :: value :: tail =>
      nextOption(tail, map ++ Map(Symbol(p) -> value))
    case unknownOption :: tail =>
      nextOption(tail, map)
    case _ => map
  }
}
