package whatcheer.macros.support

import java.time.Instant
import net.liftweb.json._
import java.time.format.DateTimeFormatter
import java.net.URL
import java.util.UUID

/** A marker trait. Add this trait to an `object` within an `object`. The top
  * level object should include `@macros.jsonify` and each `object` in that
  * object with this marker trait will have it's `val _fields_` examined for the
  * definition of a JSON-friendly class
  */
trait Jsonifyable

/** A marker trait on an `object` that will create a `sealed trait` and a series
  * of `case object`s for a union of `String` values.
  *
  * The string values should be defined in the `val _union_` and be a `List`.
  *
  * For examples `val _union_ = List("foo", "bar", "baz")`
  *
  * The `_union_` field is left intact in the object
  */
trait StringUnion

/** This trait must be added to any class that isn't
  */
trait BuildJson {
  def buildJson(): net.liftweb.json.JsonAST.JValue
  def buildJson_includingPrivate(): net.liftweb.json.JsonAST.JValue
}

object RendererAndParser {

  lazy val dateFormatter = DateTimeFormatter.ISO_INSTANT
  def dateToString(in: Instant): String = this.synchronized {
    dateFormatter.format(in)
  }
  def jvalueToDate(in: JValue): Option[Instant] = in match {
    case JString(d) => stringToDate(d)
    case JInt(instant) =>
      try {
        Some(Instant.ofEpochMilli(instant.longValue))
      } catch {
        case e: Exception => None
      }
    case _ => None
  }
  def stringToDate(in: String): Option[Instant] = this.synchronized {
    try {
      Some(Instant.from(dateFormatter.parse(in)))
    } catch {
      case e: Exception => None
    }
  }

  def stringToURL(in: String): Option[URL] = {
    try {
      Some(new URL(in))
    } catch {
      case _: Exception => None
    }
  }

  def jvalueToURL(in: JValue): Option[URL] = in match {
    case JString(d) => stringToURL(d)
    case _          => None
  }

  def urlToString(in: URL): String = in.toExternalForm()

  def stringToUUID(in: String): Option[UUID] = {
    try {
      Some(UUID.fromString(in))
    } catch {
      case _: Exception => None
    }
  }

  def jvalueToUUID(in: JValue): Option[UUID] = in match {
    case JString(d) => stringToUUID(d)
    case _          => None
  }

  def uuidToString(in: UUID): String = in.toString()

}

object Helpful {
  implicit def promoteToSome[T](v: T): Some[T] = Some(v)

  type AnyJson = Map[String, JValue]
  type StringMap = Map[String, JValue]

  type ADate = Instant

}
