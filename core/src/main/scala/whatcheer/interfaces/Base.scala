package whatcheer.interfaces

import whatcheer.utils.JsonData
import whatcheer.utils.HasProperties
import net.liftweb.json.JsonAST

object Base {
  type URL = String
  type UUID = String
}

import Base._

case class APObject(
    `type`: String,
    // ObjectId, URL used for federation. Called `uri` in Mastodon APIs.
    // https://www.w3.org/TR/activitypub/#obj-id
    id: String,
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
) extends JsonData with HasProperties {
  // Internal
  private var originalActorIdSymbol: Option[String] = None
  private var originalObjectIdSymbol: Option[String] = None
  private var mastodonIdSymbol: Option[UUID] = None

  var theProperties: JsonAST.JValue = JsonAST.JNull
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
) extends JsonData with HasProperties {
  var emailSymbol: Option[String] = None
  var isAdminSymbol: Boolean = false

  var theProperties: JsonAST.JValue = JsonAST.JNull
}

case class PublicKey(
    id: String,
    owner: URL,
    publicKeyPem: String
)
