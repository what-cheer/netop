package whatcheer.utils

import net.liftweb.json.JsonAST
import net.liftweb.json._
import java.net.URLEncoder
import java.nio.charset.Charset
import net.liftweb.http.Req

object MiscUtil {

  /** Given a string, parse it as JSON. If it's a `String`, then return a `Set`
    * of one element. If it's an `Object`, return a set of keys. If it's an
    * `Array`, return a `Set` of the array values as Strings. If it doesn't
    * parse, return the original input as the sole member of the `Set`
    *
    * @param str
    *   the input to parse
    * @return
    *   A `Set` based on the above rules
    */
  def stringToSet(str: String): Set[String] = {
    parseOpt(str) match {
      case None                 => Set(str.toLowerCase())
      case Some(JObject(items)) => Set(items.map(_.name.toLowerCase()): _*)
      case Some(JString(s))     => Set(s.toLowerCase())
      case Some(JArray(items)) =>
        Set(items.flatMap(_ match {
          case JString(s) => Some(s.toLowerCase())
          case _          => None
        }): _*)
      case _ => Set()
    }
  }

  type Headers = List[(String, String)]

  /** The CORS headers
    *
    * @return
    */
  lazy val corsHeader: Headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Headers" -> "content-type, authorization, idempotency-key",
    "Access-Control-Allow-Methods" -> "GET, PUT, POST, DELETE"
  )

  lazy val activityPubHeader: Headers = List(
    "content-type" -> "application/activity+json; charset=utf-8"
  )

  lazy val corsAndActivityPubHeader: Headers = activityPubHeader ::: corsHeader

lazy val utf8Charset = Charset.forName("UTF-8")

  /**
    * URL encode a string
    *
    * @param str the string
    * @return the URL encoded string
    */
  def urlEncode(str: String): String = URLEncoder.encode(str, utf8Charset)
}

case class Jsonable(str: String) {
  def json: JValue = parse(str)
}

object Jsonable {
  implicit def strToJsonable(str: String): Jsonable = Jsonable(str)
}
