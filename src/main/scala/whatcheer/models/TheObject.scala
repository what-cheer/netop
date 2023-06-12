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
import net.liftweb.mapper.MappedForeignKey
import net.liftweb.mapper.MappedLongForeignKey
import net.liftweb.mapper.MappedEnum
import net.liftweb.mapper.Index
import net.liftweb.json.JsonAST
import net.liftweb.mapper.DBIndexed
import net.liftweb.mapper.Mapper
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._

object TheObject extends TheObject with LongKeyedMetaMapper[TheObject] {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[TheObject])

  def createObj(theType: ObjectTypes.Value, value: JValue, to: Box[JValue], attributedTo: Box[Actor]): TheObject = {
    val ret = TheObject.create
    ret.index.set(Helpers.nextFuncName)
    ret.objectValue.setJson(value)
    to.foreach(ret.to.setJson _)
    ret.attributedTo.apply(attributedTo)
    ret
  }

  def toIRI(obj: TheObject): String = f"${Constants.BaseURL}/${Constants.ObjectBaseURI}/${URLEncoder.encode(obj.index.get, StandardCharsets.UTF_8.toString())}"
}

class TheObject extends LongKeyedMapper[TheObject] with IdPK with CreatedTrait {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[TheObject])

  def getSingleton = TheObject

  object objectType extends MappedEnum(this, ObjectTypes)
  object index extends MappedPoliteString(this, 500) with DBIndexed
  object objectValue extends MappedJson(this)
  object to extends MappedJson(this)
  object attributedTo extends MappedLongForeignKey(this, Actor)


  //  lazy val json: JsonAST.JValue = {
  //   val theId = Actor.buildActorIRI(this)
  //   ("id" -> TheObject.toIRI(this)) ~
  //     ("type" -> objectType.get.toString()) ~ ("content" -> this.getJson().openOr(JNull)) ~ ("attributedTo" -> displayName.get) ~
  //     ("summary" -> summary.get) ~ ("inbox" -> Actor.buildInboxIRI(this)) ~
  //     ("outbox" -> Actor.buildOutboxIRI(this)) ~
  //     ("followers" -> Actor.buildFollowersIRI(this)) ~
  //     ("following" -> Actor.buildFollowingIRI(this)) ~
  //     ("liked" -> Actor.buildLikedIRI(this)) ~
  //     ("publicKey" -> ("id" -> f"${theId}#main-key") ~ ("owner" -> theId) ~ ("publicKeyPem" -> pubKey.get)) ~
  //     ("endpoints" -> ("id" -> f"${theId}#endpoints") ~
  //       ("uploadMedia" -> f"${Constants.BaseURL}/${Constants.UploadEndpoint}") ~
  //       ("oauthAuthorizationEndpoint" -> f"${Constants.BaseURL}/${Constants.OAuthEndpoint}") ~
  //       ("proxyUrl" -> f"${Constants.BaseURL}/${Constants.ProxyEndpoint}"))
  //  }
}

object ObjectTypes extends Enumeration {
  type ObjectType = Value

  val Unknown, Note =
    Value
}

abstract class MappedJson[T<:Mapper[T]](theFieldOwner: T) extends MappedText[T](theFieldOwner) {
  def getJson(): Box[JsonAST.JValue] = parseOpt(this.get)

  def setJson(jv: JsonAST.JValue): T = {
    this.apply(compactRender(jv))
  }

  def apply(jv: JsonAST.JValue): T = {setJson(jv)}
}
