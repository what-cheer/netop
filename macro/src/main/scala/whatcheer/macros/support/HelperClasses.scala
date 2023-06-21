package whatcheer.macros.support

import java.util.Date

/**
  * A marker trait. Add this trait to an `object` within an `object`. The
  * top level object should include `@macros.jsonify` and each `object`
  * in that object with this marker trait will have it's `val _fields_` 
  * examined for the definition of a JSON-friendly class
  */
trait Jsonifyable

/**
  * This trait must be added to any class that isn't 
  */
trait BuildJson {
  def buildJson(): net.liftweb.json.JsonAST.JValue
}

object RendererAndParser {
  def dateToString(in: Date): String = in.toString()
}