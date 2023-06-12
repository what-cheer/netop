package whatcheer.interfaces

object Base {
  type URL = String
  type UUID = String
}

import Base._

case class APObject(
    `type`: String,
    // ObjectId, URL used for federation. Called `uri` in Mastodon APIs.
    // https://www.w3.org/TR/activitypub/#obj-id
    id: URL,
    // Link to the HTML representation of the object
    url: URL,
    published: Option[String],
    icon: Option[String],
    image: Option[String],
    summary: Option[String],
    name: Option[String],
    mediaType: Option[String],
    content: Option[String],
    inReplyTo: Option[String],

    // Extension
    preferredUsername: Option[String]
) {
  // Internal
  private var originalActorIdSymbol: Option[String] = None
  private var originalObjectIdSymbol: Option[String] = None
  private var mastodonIdSymbol: Option[UUID] = None
}

// https://www.w3.org/TR/activitystreams-vocabulary/#actor-types
case class Actor(
    `type`: String,
    // ObjectId, URL used for federation. Called `uri` in Mastodon APIs.
    // https://www.w3.org/TR/activitypub/#obj-id
    id: URL,
    // Link to the HTML representation of the object
    url: URL,
    published: Option[String],
    icon: Option[String],
    image: Option[String],
    summary: Option[String],
    name: Option[String],
    mediaType: Option[String],
    content: Option[String],
    inReplyTo: Option[String],
    inbox: URL,
    outbox: URL,
    following: URL,
    followers: URL,
    alsoKnownAs: Option[String],
    publicKey: Option[PublicKey]
) {
  var emailSymbol: Option[String] = None
  var isAdminSymbol: Boolean = false
}

case class PublicKey(
    id: String,
    owner: URL,
    publicKeyPem: String
)
