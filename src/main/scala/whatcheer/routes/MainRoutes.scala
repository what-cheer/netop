package whatcheer.routes

import net.liftweb.http.rest.RestHelper
import net.liftweb.http.Req
import net.liftweb.http.S
import net.liftweb.common.Full
import net.liftweb.http.JsonResponse
import net.liftweb.actor.LAFuture
import net.liftweb.common.{Box, Full, Empty, Failure, ParamFailure}
import net.liftweb.http.LiftResponse
import net.liftweb.common.Failure
import net.liftweb.http.PlainTextResponse
import net.liftweb.http.InMemoryResponse
import net.liftweb.http.JsonResponse
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import whatcheer.logic._
import whatcheer.utils.JsonLD
import whatcheer.schemas.Actor

object MainRoutes extends RestHelper {
  serve {
    // case Constants.Ac
    case Constants.ActorBaseURI :: Actor(actor) ::
        Constants.ActorBaseInboxURI :: Nil Get _ =>
      null // app.route(routes.inbox)get(apex.net.inbox.get)
    case Constants.ActorBaseURI ::
        Actor(actor) ::
        Constants.ActorBaseInboxURI :: Nil Post _ =>
      null // post(apex.net.inbox.post)

    case Constants.ActorBaseURI :: Actor(actor) :: Nil Get req if Validation.validateJsonLD(req).isDefined =>
      JsonLD.prependDefaultContext(actor.json)

    case ".well-known" :: "webfinger" :: Nil Get req =>
      WebFinger.service(req.param("resource"))

    case ".well-known" :: "nodeinfo" :: Nil Get req =>
      ("links" -> List(
        ("rel" -> "http://nodeinfo.diaspora.software/ns/schema/2.1") ~ ("href" -> f"https://${req.header("host").openOr(Constants.Hostnames.head)}/${Constants.NodeInfoURI}/2.1"),
        ("rel" -> "http://nodeinfo.diaspora.software/ns/schema/2.0") ~ ("href" -> f"https://${req.header("host").openOr(Constants.Hostnames.head)}/${Constants.NodeInfoURI}/2.0")
      )) ~ (".server_type" -> "What Cheer, Netop")

    case Constants.NodeInfoURI :: version :: Nil Get _ =>
      NodeInfo.getNodeInfo(version)

//   .
//   .post(apex.net.inbox.post)
// app.route(routes.outbox)
//   .get(apex.net.outbox.get)
//   .post(apex.net.outbox.post)
// app.get(routes.actor, apex.net.actor.get)
// app.get(routes.followers, apex.net.followers.get)
// app.get(routes.following, apex.net.following.get)
// app.get(routes.liked, apex.net.liked.get)
// app.get(routes.object, apex.net.object.get)
// app.get(routes.activity, apex.net.activityStream.get)
// app.get(routes.shares, apex.net.shares.get)
// app.get(routes.likes, apex.net.likes.get)

// app.post('/proxy', apex.net.proxy.post)
  }

  implicit def toLiftResponse(in: Box[JValue]): Box[LiftResponse] = {
    in match {
      case Full(jv) => Full(JsonResponse(jv))
      case ParamFailure(msg, _, _, code: Int) =>
        Full(PlainTextResponse(msg, code))
      case Failure(msg, _, _) => Full(PlainTextResponse(msg, 404))
      case _                  => Empty
    }
  }
}
