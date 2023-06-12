package whatcheer.logic

import net.liftweb.util.Props
import java.net.URLDecoder
import java.net.URLEncoder
import whatcheer.models.Actor
import java.nio.charset.StandardCharsets

object Constants {
  lazy val Hostname: String =
    Props.get("whatcheer.name", "localhost").toLowerCase()
  lazy val BaseURL: String =
    Props.get("whatcheer.baseurl", "http://localhost:8080")
  lazy val ActorBaseURI: String = Props.get("whatcheer.actorbase", "u")
  lazy val ObjectBaseURI: String = Props.get("whatcheer.objectbase", "o")
  lazy val StreamBaseURI: String = Props.get("whatcheer.streambase", "s")
  lazy val ActorBaseInboxURI: String =
    Props.get("whatcheer.actorbase.inbox", "inbox")
  lazy val ActorBaseOutboxURI: String =
    Props.get("whatcheer.actorbase.outbox", "outbox")
  lazy val ActorBaseFollowersURI: String =
    Props.get("whatcheer.actorbase.followers", "followers")
  lazy val ActorBaseFollowingURI: String =
    Props.get("whatcheer.actorbase.following", "following")
  lazy val ActorBaseLikedURI: String =
    Props.get("whatcheer.actorbase.liked", "liked")
  lazy val NodeInfoURI: String =
    Props.get("whatcheer.endpoint.nodeinfo", "nodeinfo")
  lazy val UploadEndpoint: String =
    Props.get("whatcheer.endpoint.upload", "upload")
  lazy val OAuthEndpoint: String =
    Props.get("whatcheer.endpoint.oauth", "authorize")
  lazy val ProxyEndpoint: String =
    Props.get("whatcheer.endpoint.proxy", "proxy")
  lazy val Name: String = Props.get("whatcheer.name", "whatcheer")
  lazy val Version: String = Props.get("whatcheer.version", "0.1")

  lazy val OpenRegistrations: Boolean =
    Props.getBool("whatcheer.openregistrations", false)

  val ASContext: List[String] = List(
    "https://www.w3.org/ns/activitystreams",
    "https://w3id.org/security/v1"
  )

  val formUrlType: String = "application/x-www-form-urlencoded"
  val jsonldTypes: List[String] = List(
    // req.accepts uses accepts which does match profile
    """application/ld+json; profile="https://www.w3.org/ns/activitystreams"""",
    "application/activity+json",
    // req.is uses type-is which cannot handle profile
    "application/ld+json"
  )

  // type-is is not able to match this pattern
  val jsonldOutgoingType: String =
    """application/ld+json; profile="https://www.w3.org/ns/activitystreams""""
  // since we use json-ld procedding, it will always appear this way regardless of input format
  val publicAddress: String = "as:Public"
}
