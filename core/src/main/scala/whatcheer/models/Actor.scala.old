package whatcheer.models

import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.IdPK
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

/*
object Actor extends Actor with LongKeyedMetaMapper[Actor] {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[Actor])

  def findActor(actorToFind: String): Box[Actor] =
    find(By(Actor.username, actorToFind.toLowerCase()))

  def targetActor(actorToFind: String): Box[Actor] =
    findActor(actorToFind) match {
      case Full(v) if v.tombstoned_? =>
        ParamFailure(f"Resource ${actorToFind} TOMBSTONED", 410)
      case Empty => ParamFailure(f"Resource ${actorToFind} not found", 404)
      case x     => x
    }

  def unapply(key: String): Option[Actor] = findActor(key)

  override def afterSchemifier: Unit = {
    super.afterSchemifier

    if (/*Props.testMode &&*/ findActor("test").isEmpty) {
      Actor.createActor("test", "Test User", "User for testing", Empty).save
      logger.info("Added user 'test'")
    }
  }

  def createActor(
      userName: String,
      displayName: String,
      summary: String,
      icon: Box[URL],
      actorType: String = "Person"
  ): Actor = {
    val keyPair = CryptoUtil.generateKeyPair()

    Actor.create
      .username(userName.toLowerCase().trim())
      .summary(summary)
      .displayName(displayName)
      .icon(icon.map(i => i.toExternalForm()).openOr(""))
      .actorType(actorType)
      .pubKey(CryptoUtil.keyBytesToPrettyText(keyPair.getPublic().getEncoded()))
      .privKey(
        CryptoUtil.keyBytesToPrettyText(keyPair.getPrivate().getEncoded())
      )
  }

  def buildActorIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}"

  def buildInboxIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}/${Constants.ActorBaseInboxURI}"

  def buildOutboxIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}/${Constants.ActorBaseOutboxURI}"

  def buildFollowersIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}/${Constants.ActorBaseFollowersURI}"

  def buildFollowingIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}/${Constants.ActorBaseFollowingURI}"

  def buildLikedIRI(actor: Actor): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(actor.username.get, StandardCharsets.UTF_8.toString())}/${Constants.ActorBaseLikedURI}"
}

class Actor
    extends LongKeyedMapper[Actor]
    with IdPK
    with UpdatedTrait
    with CreatedTrait {
  def getSingleton = Actor
  object username extends MappedPoliteString(this, 500) {
    override def dbIndexed_? : Boolean = true

    override def setFilter = toLower _ :: super.setFilter
  }
  object summary extends MappedText(this)
  object displayName extends MappedPoliteString(this, 500)
  object pubKey extends MappedText(this)
  object privKey extends MappedText(this)
  object icon extends MappedText(this)
  object actorType extends MappedPoliteString(this, 100)
  object tombstonedAt extends MappedDateTime(this) {
    override def defaultValue: Date = new Date(0L)
  }

  lazy val json: JsonAST.JValue = {
    val theId = Actor.buildActorIRI(this)
    ("id" -> theId) ~
      ("type" -> actorType.get) ~ ("name" -> username.get) ~ ("preferredUsername" -> displayName.get) ~
      ("summary" -> summary.get) ~ ("inbox" -> Actor.buildInboxIRI(this)) ~
      ("outbox" -> Actor.buildOutboxIRI(this)) ~
      ("followers" -> Actor.buildFollowersIRI(this)) ~
      ("following" -> Actor.buildFollowingIRI(this)) ~
      ("liked" -> Actor.buildLikedIRI(this)) ~
      ("publicKey" -> ("id" -> f"${theId}#main-key") ~ ("owner" -> theId) ~ ("publicKeyPem" -> pubKey.get)) ~
      ("endpoints" -> ("id" -> f"${theId}#endpoints") ~
        ("uploadMedia" -> f"${Constants.BaseURL}/${Constants.UploadEndpoint}") ~
        ("oauthAuthorizationEndpoint" -> f"${Constants.BaseURL}/${Constants.OAuthEndpoint}") ~
        ("proxyUrl" -> f"${Constants.BaseURL}/${Constants.ProxyEndpoint}"))
  }

  def tombstoned_? : Boolean = tombstonedAt.get.getTime() != 0L

}
*/