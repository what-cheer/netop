package whatcheer.interfaces

object NotificationEnum extends Enumeration {
  type NotificationType = Value

  val Mention = Value(1, "mention")
  val Status = Value(2, "status")
  val Reblog = Value(3, "reblog")
  val Follow = Value(4, "follow")
  val FollowRequest = Value(5, "follow_request")
  val Favorite = Value(6, "favourite")
  val Poll = Value(7, "poll")
  val Update = Value(8, "update")
  val AdminSignUp = Value(9, "admin.sign_up")
  val AdminReport = Value(10, "admin.report")
}

case class Notification(
    id: String,
    `type`: NotificationEnum.NotificationType,
    created_at: String,
    account: MastodonAccount,
    status: Option[MastodonStatus]
)

case class NotificationsQueryResult( // extends ObjectsRow {
    properties: String,
    mastodon_id: String,
    id: Base.URL,
    cdate: String,
    `type`: NotificationEnum.NotificationType,
    original_actor_id: Base.URL,
    notif_from_actor_id: Base.URL,
    notif_cdate: String,
    notif_id: Base.URL,
    from_actor_id: String
)
