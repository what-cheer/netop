package whatcheer.logic

import net.liftweb.json.JsonAST
import net.liftweb.common.Empty
import net.liftweb.common.Box
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.common.ParamFailure
import net.liftweb.common.Full
import whatcheer.models.Actor

object NodeInfo {
  def generateNodeInfo(version: String): JsonAST.JValue = {
    ("version" -> version) ~ ("software" -> ("name" -> Constants.Name) ~ ("version" -> Constants.Version))
    ("protocols" -> List(
      "activitypub"
    )) ~ ("services" -> ("inbound" -> List()) ~ ("outbound" -> List())) ~
      ("openRegistrations" -> Constants.OpenRegistrations) ~ ("usage" -> Actor
        .count()) ~ ("metadata" -> JObject())
  }

  def getNodeInfo(version: String): Box[JsonAST.JValue] = {
    if (
      version
        .startsWith("2") && (version.length() == 1 || version.charAt(1) == '.')
    ) {
      Full(generateNodeInfo(version))
    } else {
      ParamFailure("Only nodeinfo 2.x supported", 404)

    }
  }
}
