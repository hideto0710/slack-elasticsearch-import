package slack.models

/**
 * channelの情報
 * @param id ID
 * @param name 名前
 * @param created 作成日時（UNIX時刻）
 * @param creator 作成者
 * @param members 参加メンバー
 * @param num_members 参加メンバー数
 * @param is_channel チャネルであるか
 * @param is_archived アーカイブされているか
 * @param is_general generalであるか
 * @param is_member 自身がメンバーであるか
 * @param topic トピック
 * @param purpose 目的
 */
case class Channel(
  id: String,
  name: String,
  created: Long,
  creator: String,
  members: Seq[String],
  num_members: Int,
  is_channel: Boolean,
  is_archived:Boolean,
  is_general:Boolean,
  is_member: Boolean,
  topic: ChannelValue,
  purpose: ChannelValue
)

/**
 * channelの項目毎の詳細情報
 * @param value 項目に関する情報
 * @param creator 更新者
 * @param last_set 更新日時
 */
case class ChannelValue(
  value: String,
  creator: String,
  last_set: Long
)

/**
 * コメントの情報
 * @param subtype サブタイプ
 * @param user ユーザー
 * @param text コメント
 * @param ts 日時（UNIX時刻）
 */
case class SlackComment(
  subtype: String,
  user: String,
  text: String,
  ts: String
)