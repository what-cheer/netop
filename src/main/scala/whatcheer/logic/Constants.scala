package whatcheer.logic

import net.liftweb.util.Props
import java.net.URLDecoder
import java.net.URLEncoder
import whatcheer.models.Actor
import java.nio.charset.StandardCharsets

object Constants {
  lazy val Hostname: String = Props.get("whatcheer.name", "localhost").toLowerCase()
  lazy val BaseURL: String = Props.get("whatcheer.baseurl", "http://localhost:8080")
  lazy val ActorBaseURI: String = Props.get("whatcheer.actorbase", "u")
  lazy val NodeInfoURI: String = Props.get("whatcheer.nodeinfouri", "nodeinfo")
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
