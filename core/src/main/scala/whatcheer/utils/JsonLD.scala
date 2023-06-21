package whatcheer.utils

import net.liftweb.common.Box
import java.io.ByteArrayInputStream
import com.github.jsonldjava.utils.JsonUtils
import net.liftweb.util.Helpers
import java.util.HashMap
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import net.liftweb.json.JsonAST
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object JsonLD {
  private def loadFromResources(name: String): Object = {
    val stream = this.getClass().getResourceAsStream(name)
    val ret = JsonUtils.fromInputStream(stream)
    stream.close()
    ret
  }

  lazy val ActivityStreamJson = loadFromResources("/json/as.json")
  lazy val SecurityJson = loadFromResources("/json/security.json")

  /** Attempt to parse a byte array into a JsonLD structure
    *
    * @param in
    * @return
    */
  def parseAsJsonLD(in: Array[Byte]): Box[JsonLDHolder] = {
    val inputStream = new ByteArrayInputStream(in)
    val jsonObject = Helpers.tryo {
      ParsedJsonHolder(JsonUtils.fromInputStream(inputStream))
    }
    jsonObject.flatMap(parseJsonToJsonLD _)
  }

  /** Parse a holder of a an `Object` representing a JSON parsed input into a
    * JsonLDHolder... which holds an `Object` that is likely a map of a compact
    * JsonLD thing
    *
    * @param in
    * @return
    */
  def parseJsonToJsonLD(in: ParsedJsonHolder): Box[JsonLDHolder] = {

    val context = new HashMap()
    val options = new JsonLdOptions()
    options.setCompactArrays(false)

    Helpers.tryo {
      val compact = JsonLdProcessor.compact(in.json, context, options)
      val graph = compact.get("@graph").asInstanceOf[java.util.List[Object]]
      JsonLDHolder(graph.get(0))
    }
  }

  def prependDefaultContext(obj: JsonAST.JValue): JsonAST.JObject = {
    obj match {
      // context already defined
      case ret @ JObject(first :: _) if first.name == "@context" => ret

      // prepend context
      case JObject(obj) =>
        JObject(
          JsonAST.JField(
            "@context",
            JsonAST.JArray(
              List(
                JsonAST.JString("https://www.w3.org/ns/activitystreams"),
                ("ostatus" -> "http://ostatus.org#") ~
                  ("atomUri" -> "ostatus:atomUri") ~
                  ("inReplyToAtomUri" -> "ostatus:inReplyToAtomUri") ~
                  ("conversation" -> "ostatus:conversation") ~
                  ("sensitive" -> "as:sensitive") ~
                  ("toot" -> "http://joinmastodon.org/ns#") ~
                  ("votersCount" -> "toot:votersCount"),
                JsonAST.JString("https://w3id.org/security/v1")
              )
            )
          ) :: obj
        )

      // create a new JObject with context
      case jv =>
        JObject(
          JsonAST.JField(
            "@context",
            JsonAST.JArray(
              List(
                JsonAST.JString("https://www.w3.org/ns/activitystreams"),
                ("ostatus" -> "http://ostatus.org#") ~
                  ("atomUri" -> "ostatus:atomUri") ~
                  ("inReplyToAtomUri" -> "ostatus:inReplyToAtomUri") ~
                  ("conversation" -> "ostatus:conversation") ~
                  ("sensitive" -> "as:sensitive") ~
                  ("toot" -> "http://joinmastodon.org/ns#") ~
                  ("votersCount" -> "toot:votersCount"),
                JsonAST.JString("https://w3id.org/security/v1")
              )
            )
          ) :: JsonAST.JField("value", jv) :: Nil
        )
    }
  }
}

/** A thin wrapper around a parsed JSON data structure that's been passed throw
  * JsonLD "compact"
  *
  * @param jsonld
  */
case class JsonLDHolder(jsonld: Object)

/** A thin wrapper around an object that represents JSON parsed data
  *
  * @param json
  */
case class ParsedJsonHolder(json: Object)

trait JsonData {
  import net.liftweb.json._
  import net.liftweb.json.Serialization.{read, write}
  import JsonData._

  def toJson(): JValue = {
    val ret = Extraction.decompose(this)(formats)

    this match {
      case x: HasProperties =>
        (ret, x.theProperties) match {
          case (x, JNull) => x
          case (x, JNothing) => x
          case (JObject(jo), JObject(j2)) => JObject(jo ::: j2)
          case (JObject(jo), other) => JObject(jo ::: List(JField("properties", other)))
          case (x, y) => ("object" -> x) ~ ("properties" -> y)
        }
        case _ => ret
    }
  }
}

object JsonData {
  implicit val formats = net.liftweb.json.DefaultFormats
}

trait HasProperties {
  def theProperties: JValue 
}