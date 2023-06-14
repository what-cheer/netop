package whatcheer.utils

import net.liftweb.json.JsonAST
import net.liftweb.json._

object MiscUtil {
  /**
    * Given a string, parse it as JSON. If it's a `String`, then
      return a `Set` of one element. If it's an `Object`, return
        a set of keys. If it's an `Array`, return a `Set` of the array
        values as Strings. If it doesn't parse, return the original
        input as the sole member of the `Set`
    *
    * @param str the input to parse
    * @return A `Set` based on the above rules
    */
  def stringToSet(str: String): Set[String] = {
    parseOpt(str) match {
      case None => Set(str.toLowerCase())
      case Some(JObject(items)) => Set(items.map(_.name.toLowerCase()) :_ *)
      case Some(JString(s)) => Set(s.toLowerCase())
      case Some(JArray(items)) => Set(items.flatMap(_ match {
        case JString(s) => Some(s.toLowerCase())
        case _ => None
      }) :_ *)
      case _ => Set()
    }
  }
}
