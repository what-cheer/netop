package whatcheer.interfaces

object Visibility extends Enumeration {
  type VisibilityType = Value

  val Public = Value(1, "public")
  val Unlisted = Value(2, "unlisted")
  val Private = Value(3, "private")
  val Direct = Value(4, "direct")
}


// https://docs.joinmastodon.org/entities/Status/
// https://github.com/mastodon/mastodon-android/blob/master/mastodon/src/main/java/org/joinmastodon/android/model/Status.java
case class MastodonStatus(
	id: Base.UUID,
	uri: Base.URL,
	url: Base.URL,
	created_at: String,
	account: MastodonAccount,
	content: String,
	visibility: Visibility.VisibilityType,
	spoiler_text: String,
	emojis: List[String],
	media_attachments: List[MediaAttachment],
	mentions: List[Any],
	tags: List[Any],
	favourites_count: Option[Int],
	reblogs_count: Option[Int],
	reblog: Option[MastodonStatus],
	edited_at: Option[String],
	replies_count: Option[Int],
	reblogged: Option[Boolean],
	favourited: Option[Boolean],
	in_reply_to_id: Option[String],
	in_reply_to_account_id: Option[String]
)

// https://docs.joinmastodon.org/entities/Context/
case class Context(
	ancestors: List[MastodonStatus],
	descendants: List[MastodonStatus]
)

