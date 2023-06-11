package whatcheer.utils

import net.liftweb.common.Box
import java.io.ByteArrayInputStream
import com.github.jsonldjava.utils.JsonUtils
import net.liftweb.util.Helpers
import java.util.HashMap
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor

object JsonLD {

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

  /**
    * Parse a holder of a an `Object` representing a JSON parsed
    * input into a JsonLDHolder... which holds an `Object` that
    * is likely a map of a compact JsonLD thing
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
}

/**
  * A thin wrapper around a parsed JSON data structure that's been passed
  * throw JsonLD "compact"
  *
  * @param jsonld
  */
case class JsonLDHolder(jsonld: Object)

/**
  * A thin wrapper around an object that represents JSON parsed data
  *
  * @param json
  */
case class ParsedJsonHolder(json: Object)
