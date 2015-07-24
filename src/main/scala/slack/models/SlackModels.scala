package slack.models

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

case class ChannelValue(
  value: String,
  creator: String,
  last_set: Long
)

case class SlackComment(
  subtype: String,
  user: String,
  text: String,
  ts: String
)