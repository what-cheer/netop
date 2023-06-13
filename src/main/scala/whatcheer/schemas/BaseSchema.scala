package whatcheer.schemas

import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedPoliteString
import net.liftweb.mapper.UpdatedTrait
import net.liftweb.mapper.CreatedTrait
import net.liftweb.mapper.MappedDateTime
import java.util.Date
import net.liftweb.mapper.By
import net.liftweb.common.Box
import whatcheer.logic.Constants
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import net.liftweb.util.TimeHelpers
import net.liftweb.common.Logger
import net.liftweb.common.{Full, Empty}
import net.liftweb.common.ParamFailure
import java.net.URL
import net.liftweb.mapper.MappedText
import net.liftweb.util.Helpers
import java.security.KeyPairGenerator
import net.liftweb.util.Props
import net.liftweb.json.JsonAST
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import whatcheer.utils.CryptoUtil
import net.liftweb.db.DriverType
import net.liftweb.mapper.BaseKeyedMapper
import net.liftweb.mapper.Mapper
import net.liftweb.mapper.IndexedField
import net.liftweb.mapper.KeyedMapper
import net.liftweb.mapper.KeyedMetaMapper
import whatcheer.models.MappedJson
import net.liftweb.mapper.DBIndexed
import net.liftweb.mapper.MappedStringForeignKey
import net.liftweb.mapper.UniqueIndex
import net.liftweb.mapper.BaseMapper
import net.liftweb.mapper.MappedBoolean
import net.liftweb.mapper.MappedField
import net.liftweb.mapper.BaseIndex
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.MetaMapper

object DBConsts {
  def primaryKeyLen = 500
}

abstract class MappedStringIndex[T <: Mapper[T]](theOwner: T, len: Int)
    extends MappedString[T](theOwner, len)
    with IndexedField[String] {

  // override def writePermission_? = false // not writable

  override def dbIndexed_? = true

  def defined_? = i_is_! != defaultValue
  override def dbPrimaryKey_? = true

  override def defaultValue = Helpers.nextFuncName

  override def setFilter: List[String => String] =
    trim _ :: toLower _ :: super.setFilter

  override def dbIndexFieldIndicatesSaved_? = { i_is_! != defaultValue }

  override def dbDisplay_? = false

  override def fieldCreatorString(dbType: DriverType, colName: String): String =
    colName + " TEXT NOT NULL"

  override def convertKey(in: String): Box[String] = in match {
    case null                 => Empty
    case x if x.length() == 0 => Empty
    case x                    => Full(x.toLowerCase())
  }

  override def convertKey(in: Int): Box[String] = convertKey(in.toString())

  override def convertKey(in: Long): Box[String] = convertKey(in.toString())

  override def convertKey(in: AnyRef): Box[String] = in match {
    case null => Empty
    case x    => convertKey(x.toString())
  }

  override def makeKeyJDBCFriendly(in: String): AnyRef = in

}

trait BaseStringKeyedMapper extends BaseKeyedMapper {
  override type TheKeyType = String
}

trait StringKeyedMapper[OwnerType <: StringKeyedMapper[OwnerType]]
    extends KeyedMapper[String, OwnerType]
    with BaseStringKeyedMapper {
  self: OwnerType =>
}

trait StringIdPK {
  self: BaseStringKeyedMapper =>
  def primaryKeyField: MappedStringIndex[MapperType] = id
  object id
      extends MappedStringIndex[MapperType](
        this.asInstanceOf[MapperType],
        DBConsts.primaryKeyLen
      ) {}
}

trait StringKeyedMetaMapper[A <: StringKeyedMapper[A]]
    extends KeyedMetaMapper[String, A] { self: A => }

object Actor extends Actor with StringKeyedMetaMapper[Actor] {}

trait Properties {
  self: BaseMapper =>

  object properties
      extends MappedJson[MapperType](this.asInstanceOf[MapperType]) {
    override def dbNotNull_? = true
    override def defaultValue: String = "{}"
  }
}

class Actor
    extends StringKeyedMapper[Actor]
    with StringIdPK
    with UpdatedTrait
    with CreatedTrait
    with Properties {
  private lazy val logger = Logger(classOf[Actor])
  def getSingleton = Actor
  object theType extends MappedText(this) {
    override def dbNotNull_? : Boolean = true
    override def dbColumnName: String = "type"
  }
  object email extends MappedText(this) with DBIndexed
  object privkey extends MappedText(this)
  object privkeySalt extends MappedText(this) {
    override def dbColumnName: String = "privkey_salt"
  }
  object pubkey extends MappedText(this)

}

object ActorFollowing
    extends ActorFollowing
    with StringKeyedMetaMapper[ActorFollowing] {
  override def dbIndexes: List[BaseIndex[ActorFollowing]] =
    UniqueIndex(actorId, targetActorId) :: super.dbIndexes

    override def dbTableName: String = "actor_following"
}

class ActorFollowing
    extends StringKeyedMapper[ActorFollowing]
    with StringIdPK
    with UpdatedTrait
    with CreatedTrait {
  private lazy val logger = Logger(classOf[ActorFollowing])
  def getSingleton = ActorFollowing

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object targetActorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "target_actor_id"

    override def dbIndexed_? = true

    override def dbNotNull_? = true

  }

  object targetActorAcct extends MappedText(this) {
    override def dbColumnName: String = "target_actor_acct"

    override def dbNotNull_? = true
  }
  object state extends MappedText(this) {
    override def defaultValue: String = "pending"
    override def dbNotNull_? = true
  }
}

object Objects extends Objects with StringKeyedMetaMapper[Objects] {}

class Objects
    extends StringKeyedMapper[Objects]
    with StringIdPK
    with UpdatedTrait
    with CreatedTrait
    with Properties {
  private lazy val logger = Logger(classOf[Objects])
  def getSingleton = Objects

  object mastodonId extends MappedText(this) {
    override def fieldCreatorString(
        dbType: DriverType,
        colName: String
    ): String =
      colName + " TEXT UNIQUE NOT NULL"
    override def dbColumnName: String = "mastodon_id"

  }
  object theType extends MappedText(this) {
    override def dbNotNull_? = true
    override def dbColumnName: String = "type"
  }

  object originalActorId extends MappedText(this) {
    override def dbColumnName: String = "original_actor_id"
  }

  object originalObjectId extends MappedText(this) {
    override def dbColumnName: String = "original_object_id"

    override def fieldCreatorString(
        dbType: DriverType,
        colName: String
    ): String =
      colName + " TEXT UNIQUE"

  }
  object replyToObjectId extends MappedText(this) {
    override def dbColumnName: String = "reply_to_object_id"
  }
  object local extends MappedBoolean(this)

}

object InboxObjects
    extends InboxObjects
    with StringKeyedMetaMapper[InboxObjects] {
  override def dbTableName: String = "inbox_objects"
}

class InboxObjects
    extends StringKeyedMapper[InboxObjects]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[InboxObjects])
  override def getSingleton: KeyedMetaMapper[String, InboxObjects] =
    InboxObjects

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = true
  }
}

object OutboxObjects
    extends OutboxObjects
    with StringKeyedMetaMapper[OutboxObjects] {
  override def dbTableName: String = "outbox_objects"
}

class OutboxObjects
    extends StringKeyedMapper[OutboxObjects]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[OutboxObjects])
  override def getSingleton = OutboxObjects

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = true
  }

  object target extends MappedText(this) {
    override def dbNotNull_? = true

    override def dbIndexed_? : Boolean = true
    override def defaultValue: String =
      "https://www.w3.org/ns/activitystreams#Public"
  }
}

object ActorNotifications
    extends ActorNotifications
    with LongKeyedMetaMapper[ActorNotifications] {}

class ActorNotifications
    extends LongKeyedMapper[ActorNotifications]
    with IdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[ActorNotifications])
  override def getSingleton = ActorNotifications

  object theType extends MappedText(this) {
    override def dbNotNull_? : Boolean = true
    override def dbColumnName: String = "type"
  }

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object fromActorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "from_actor_id"
    override def dbIndexed_? = false
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = false
  }
}

object ActorFavourites
    extends ActorFavourites
    with StringKeyedMetaMapper[ActorFavourites] {
  override def dbTableName: String = "actor_favourites"
}

class ActorFavourites
    extends StringKeyedMapper[ActorFavourites]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[ActorFavourites])
  override def getSingleton = ActorFavourites

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = true
  }
}

object ActorReblogs
    extends ActorReblogs
    with StringKeyedMetaMapper[ActorReblogs] {
  override def dbTableName: String = "actor_reblogs"
}

class ActorReblogs
    extends StringKeyedMapper[ActorReblogs]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[ActorReblogs])
  override def getSingleton = ActorReblogs

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = true
  }
}

object ActorReplies
    extends ActorReplies
    with StringKeyedMetaMapper[ActorReplies] {
  override def dbTableName: String = "actor_replies"
}

class ActorReplies
    extends StringKeyedMapper[ActorReplies]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[ActorReplies])
  override def getSingleton = ActorReplies

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object objectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "object_id"
    override def dbIndexed_? = true
  }

  object inReplyToObjectId
      extends MappedStringForeignKey(this, Objects, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Objects] = Objects
    override def dbColumnName: String = "in_reply_to_object_id"
    override def dbIndexed_? = true
  }
}

object Clients extends Clients with StringKeyedMetaMapper[Clients] {}

class Clients
    extends StringKeyedMapper[Clients]
    with StringIdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[Clients])
  override def getSingleton = Clients

  object secret extends MappedText(this) {
    override def dbNotNull_? = true
  }

  object name extends MappedText(this) {
    override def dbNotNull_? = true
  }

  object redirectUris extends MappedText(this) {
    override def dbNotNull_? = true
    override def dbColumnName: String = "redirect_uris"
  }
  object website extends MappedText(this)
  object scopes extends MappedText(this)

}

object Subscriptions
    extends Subscriptions
    with LongKeyedMetaMapper[Subscriptions] {
  override def dbIndexes: List[BaseIndex[Subscriptions]] =
    UniqueIndex(actorId, clientId) :: super.dbIndexes
}

class Subscriptions
    extends LongKeyedMapper[Subscriptions]
    with IdPK
    with CreatedTrait
    with UpdatedTrait {
  private lazy val logger = Logger(classOf[Subscriptions])
  override def getSingleton = Subscriptions

  object actorId
      extends MappedStringForeignKey(this, Actor, DBConsts.primaryKeyLen) {

    override def foreignMeta: KeyedMetaMapper[String, Actor] = Actor
    override def dbColumnName: String = "actor_id"
    override def dbIndexed_? = true
  }

  object clientId
      extends MappedStringForeignKey(this, Clients, DBConsts.primaryKeyLen) {
    override def foreignMeta: KeyedMetaMapper[String, Clients] = Clients

  }

  object endpoint extends MappedText(this)
  object keyP256dh extends MappedText(this) {
    override def dbNotNull_? = true
    override def dbColumnName: String = "key_p256dh"
  }

  object keyAuth extends MappedText(this) {
    override def dbNotNull_? = true
    override def dbColumnName: String = "key_auth"
  }

  object alertMention extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_mention"
  }

  object alertStatus extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_status"
  }
  object alertReblog extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_reblog"
  }
  object alertFollow extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_follow"
  }
  object alertFollowRequest extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_follow_request"
  }
  object alertFavourite extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_favourite"
  }
  object alertPoll extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_poll"
  }
  object alertUpdate extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_update"
  }
  object alertAdminSignUp extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_admin_sign_up"
  }
  object alertAdminReport extends MappedBoolean(this) {
    override def dbColumnName: String = "alert_admin_report"
  }
  object policy extends MappedText(this)
}

object Peers extends Peers with MetaMapper[Peers] {
  override def dbIndexes: List[BaseIndex[Peers]] =
    UniqueIndex(domain) :: super.dbIndexes
}

class Peers extends Mapper[Peers] {
  private lazy val logger = Logger(classOf[Peers])
  override def getSingleton = Peers

  object domain extends MappedText(this) {
    override def dbNotNull_? = true

  }
}
/*




CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts5 (
    type,
    name,
    preferredUsername,
    status
);

CREATE TRIGGER IF NOT EXISTS actors_search_fts_insert AFTER INSERT ON actors
BEGIN
    INSERT INTO search_fts (rowid, type, name, preferredUsername)
    VALUES (new.rowid,
            new.type,
            json_extract(new.properties, '$.name'),
            json_extract(new.properties, '$.preferredUsername'));
END;

CREATE TRIGGER IF NOT EXISTS actors_search_fts_delete AFTER DELETE ON actors
BEGIN
    DELETE FROM search_fts WHERE rowid=old.rowid;
END;

CREATE TRIGGER IF NOT EXISTS actors_search_fts_update AFTER UPDATE ON actors
BEGIN
    DELETE FROM search_fts WHERE rowid=old.rowid;
    INSERT INTO search_fts (rowid, type, name, preferredUsername)
    VALUES (new.rowid,
            new.type,
            json_extract(new.properties, '$.name'),
            json_extract(new.properties, '$.preferredUsername'));
END;

 */
