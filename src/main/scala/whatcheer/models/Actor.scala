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

  override def afterSchemifier: Unit = {
    super.afterSchemifier
    if (findActor("dpp").isEmpty) {
      Actor.create.username("dpp").save
      logger.info("Added user dpp")
    }
  }

  def createActor(
      userName: String,
      displayName: String,
      summary: String,
      icon: Box[URL],
      actorType: String = "Person"
  ): Actor = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(4096);
    val keyPair = keyGen.genKeyPair()
    val pub = Helpers.hexEncode(keyPair.getPublic().getEncoded())
    val priv = Helpers.hexEncode(keyPair.getPrivate().getEncoded())

    Actor.create
      .username(userName.toLowerCase().trim())
      .summary(summary)
      .displayName(displayName)
      .icon(icon.map(i => i.toExternalForm()).openOr(""))
      .actorType(actorType)
      .pubKey(pub)
      .privKey(priv)
  }

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

  def tombstoned_? : Boolean = tombstonedAt.get.getTime() != 0L

  def buildActorIRI(): String =
    f"${Constants.BaseURL}/${Constants.ActorBaseURI}/${URLEncoder.encode(username.get, StandardCharsets.UTF_8.toString())}"

}
