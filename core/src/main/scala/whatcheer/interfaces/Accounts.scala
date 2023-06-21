package whatcheer.interfaces

// https://docs.joinmastodon.org/entities/Account/
// https://github.com/mastodon/mastodon-android/blob/master/mastodon/src/main/java/org/joinmastodon/android/model/Account.java
// https://docs.joinmastodon.org/entities/Account/#CredentialAccount
case class MastodonAccount(
    id: String,
    username: String,
    acct: String,
    url: String,
    display_name: String,
    note: String,
    avatar: String,
    avatar_static: String,
    header: String,
    header_static: String,
    created_at: String,
    locked: Option[Boolean],
    bot: Option[Boolean],
    discoverable: Option[Boolean],
    group: Option[Boolean],
    followers_count: Int,
    following_count: Int,
    statuses_count: Int,
    emojis: List[String],
    fields: List[Field],
    source: Option[Source],
    role: Option[Role]
)

// https://docs.joinmastodon.org/entities/Relationship/
// https://github.com/mastodon/mastodon-android/blob/master/mastodon/src/main/java/org/joinmastodon/android/model/Relationship.java
case class Relationship(
    id: String
)

object Privacy extends Enumeration {
  type PrivacyType = Value

  val Public = Value(1, "public")
  val Unlisted = Value(2, "unlisted")
  val Private = Value(3, "private")
  val Direct = Value(4, "direct")

}

case class Source(
    note: String,
    fields: List[Field],
    privacy: Privacy.PrivacyType,
    sensitive: Boolean,
    language: String,
    follow_requests_count: Int
)

// https://docs.joinmastodon.org/entities/Role/
case class Role(
    id: String,
    name: String,
    color: String,
    position: Int,
    // https://docs.joinmastodon.org/entities/Role/#permission-flags
    permissions: Long,
    highlighted: Boolean,
    created_at: String,
    updated_at: String
)

case class Field(
    name: String,
    value: String,
    verified_at: Option[String]
)
