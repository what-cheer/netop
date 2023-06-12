package whatcheer.logic

import net.liftweb.http.Req
import net.liftweb.common.Box
import net.liftweb.http.ContentType
import net.liftweb.common.Full
import net.liftweb.common.Empty
import net.liftweb.json.JsonAST
import whatcheer.utils.JsonLDHolder
import whatcheer.utils.JsonLD
import net.liftweb.common.ParamFailure

object Validation {
  def validateJsonLD(reqProxy: ReqProxy): Box[(Boolean, Box[JsonLDHolder])] = {
    val jld = reqProxy.acceptsList.filter(c => c.theType == "application" && (c.subtype == "ld+json" || c.subtype == "activity+json"))
    val textHtml = reqProxy.acceptsList.filter(c => c.theType == "text" && c.subtype == "html")
    val isJsonLdGet = reqProxy.method == "GET" && textHtml.length == 0 && jld.length > 0
    val isJsonLdProxy = reqProxy.method == "POST" && jld.length > 0 && reqProxy.contentType == Full(Constants.formUrlType)
    if (isJsonLdGet || isJsonLdProxy) Full((true, Empty))
    else if (reqProxy.method == "POST" && reqProxy.contentType == Full(Constants.jsonldOutgoingType(0))) {
        val holder = reqProxy.body.flatMap(JsonLD.parseAsJsonLD _)
        holder match {
            case Full(_) => Full((true, holder))
            case _ => ParamFailure("Request body is not valid JSON-LD", 400)
        }
    } else {
        ParamFailure("Invalid request", 404)
    }
  }


}

trait ReqProxy {
    def method: String
    def acceptsList: List[ContentType]
    def contentType: Box[String]
    def body: Box[Array[Byte]]
}

object ReqProxy {
    implicit def fromRequest(theRequest: Req): ReqProxy = new ReqProxy {
        lazy val method: String = theRequest.requestType.method
        lazy val acceptsList: List[ContentType] = theRequest.weightedAccept

        lazy val contentType: Box[String] = theRequest.contentType

        lazy val body = theRequest.body
    }
}