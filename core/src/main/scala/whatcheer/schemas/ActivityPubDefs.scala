package whatcheer.schemas

import whatcheer.macros
import whatcheer.macros.support.{BuildJson, Jsonifyable, StringUnion, Helpful}
import Helpful._
import net.liftweb.json._
import java.time.Instant
import java.net.URL
import java.util.UUID

@macros.jsonify
object Schemas {

  object MastodonAccount extends Jsonifyable {
    val _fields_ = (
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
      created_at: ADate,
      locked: Boolean_?,
      bot: Boolean_?,
      discoverable: Boolean_?,
      group: Boolean_?,
      followers_count: Int,
      following_count: Int,
      statuses_count: Int,
      emojis: List[AnyJson],
      fields: List[Field]
    )
  }

  object Relationship extends Jsonifyable {
    val _fields_ = Tuple(id: String)
  }

  object Privacy extends StringUnion {
    val _union_ = List("public", "unlisted", "private", "direct")
  }

  object Source extends Jsonifyable {
    val _fields_ = (
      note: String,
      fields: List[Field],
      privacy: Privacy,
      sensitive: Boolean,
      language: String,
      follow_requests_count: Int
    )
  }

  object CredentialAccount extends Jsonifyable {

    val _fields_ = (
      M(MastodonAccount),
      source: Source,
      role: Role
    )
  }

  object Role extends Jsonifyable {
    val _fields_ = (
      id: String,
      name: String,
      color: String,
      position: Int,
      // https://docs.joinmastodon.org/entities/Role/#permission-flags
      permissions: Int,
      highlighted: Boolean,
      created_at: String,
      updated_at: String
    )
  }

  object Field extends Jsonifyable {
    val _fields_ = (name: String, value: String, verified_at: Option[Instant])
  }

// https://docs.joinmastodon.org/entities/Instance/
  object InstanceConfig extends Jsonifyable {
    val _fields_ = (
      uri: String,
      title: String,
      thumbnail: String,
      languages: List[String],
      email: String,
      description: String,
      short_description: String_?,
      rules: List[Rule]
    )
  }
  object Enabled extends Jsonifyable {
    val _fields_ = Tuple(enabled: Boolean)
  }

  object URLField extends Jsonifyable {
    val _fields_ = Tuple(url: URL)
  }

  // object Email extends Jsonifyable {
  //   val _fields_ = Tuple(email: String)
  // }
  object InstanceConfigV2 extends Jsonifyable {
    val _fields_ = (
      domain: String,
      title: String,
      version: String,
      source_url: URL,
      description: String,
      thumbnail: URLField,
      languages: List[String],
      registrations: Enabled,
      contact: ContextSchema.Identity,
      rules: List[Rule]
    )
  }

// https://docs.joinmastodon.org/entities/Rule/
  object Rule extends Jsonifyable {
    val _fields_ = (id: String, text: String)
  }

  object DefaultImages extends Jsonifyable {
    val _fields_ = (avatar: String, header: String)
  }

  object Identity extends Jsonifyable {
    val _fields_ = Tuple(email: String)
  }

  object ContextData extends Jsonifyable {
    val _fields_ = (
      // ActivityPub Person object of the logged in user
      connectedActor: BaseSchema.Person,

      // Object returned by Cloudflare Access' provider
      identity: Identity,

      // Client or app identifier
      clientId: String
    )
  }

  // https://www.w3.org/TR/activitystreams-vocabulary/#actor-types
  object Actor extends Jsonifyable {
    val _fields_ = (
      M(APObject),
      inbox: URL,
      outbox: URL,
      following: URL,
      followers: URL,
      alsoKnownAs: String_?,
      `!emailSymbol`: String_?,
      `!isAdminSymbol`: Boolean_?
    )
  }
  object PubKey extends Jsonifyable {
    val _fields_ = (id: String, owner: URL, publicKeyPem: String)

  }
// https://www.w3.org/TR/activitystreams-vocabulary/#dfn-person
  object Person extends Jsonifyable {
    val _fields_ = (M(Actor), publicKey: PubKey)

  }

  object APObject extends Jsonifyable {

    val _fields_ = (
      `type`: String,
      // ObjectId, URL used for federation. Called `uri` in Mastodon APIs.
      // https://www.w3.org/TR/activitypub/#obj-id
      id: URL,
      // Link to the HTML representation of the object
      url: URL,
      published: String_?,
      icon: APObject_?,
      image: APObject_?,
      summary: String_?,
      name: String_?,
      mediaType: String_?,
      content: String_?,
      inReplyTo: String_?,

      // Extension
      preferredUsername: String_?,
      // Internal
      `!originalActorIdSymbol`: String_?,
      `!originalObjectIdSymbol`: String_?,
      `!mastodonIdSymbol`: UUID_?
    )
  }

  object Document extends Jsonifyable {
    val _fields_ = Tuple1(M(APObject))
  }

  object MediaType extends StringUnion {
    val _union_ = List("unknown", "image", "gifv", "video", "audio")
  }

  object MediaAttachment extends Jsonifyable {
    val _fields_ = (
      id: String,
      `type`: MediaType,
      url: URL,
      preview_url: URL,
      meta: AnyJson,
      description: String,
      blurhash: String
    )
  }

  object Tag extends Jsonifyable {
    val _fields_ =
      (name: String, url: URL, history: List[AnyJson], following: Boolean_?)
  }

// https://docs.joinmastodon.org/entities/Status/
// https://github.com/mastodon/mastodon-android/blob/master/mastodon/src/main/java/org/joinmastodon/android/model/Status.java
  object MastodonStatus extends Jsonifyable {
    val _fields_ = (
      id: UUID,
      uri: URL,
      url: URL,
      created_at: Instant,
      account: MastodonAccount,
      content: String,
      visibility: Privacy,
      spoiler_text: String,
      emojis: List[AnyJson],
      media_attachments: List[MediaAttachment],
      mentions: List[AnyJson],
      tags: List[Tag],
      favourites_count: Int_?,
      reblogs_count: Int_?,
      reblog: Option[MastodonStatus],
      edited_at: Instant_?,
      replies_count: Int_?,
      reblogged: Boolean_?,
      favourited: Boolean_?,
      in_reply_to_id: String_?,
      in_reply_to_account_id: String_?
    )
  }

// https://docs.joinmastodon.org/entities/Context/
  object Context extends Jsonifyable {
    val _fields_ =
      (ancestors: List[MastodonStatus], descendants: List[MastodonStatus])
  }

  object NotificationType extends StringUnion {
    val _union_ = List(
      "mention",
      "status",
      "reblog",
      "follow",
      "follow_request",
      "favourite",
      "poll",
      "update",
      "admin.sign_up",
      "admin.report"
    )
  }

  object Notification extends Jsonifyable {
    val _fields_ = (
      id: String,
      `type`: NotificationType,
      created_at: ADate,
      account: MastodonAccount,
      status: MastodonStatus_?
    )
  }

  object NotificationsQueryResult extends Jsonifyable {
    val _fields_ = (
      M(ObjectsRow),
      (`type`: NotificationType, NotificationType_mention),
      original_actor_id: URL_?,
      notif_from_actor_id: URL_?,
      notif_cdate: ADate,
      notif_id: URL_?,
      from_actor_id: String
    )
  }

  object ObjectsRow extends Jsonifyable {
    val _fields_ =
      (properties: String, mastodon_id: UUID, id: URL, cdate: ADate)
  }

}
