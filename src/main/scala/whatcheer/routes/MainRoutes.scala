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
import whatcheer.utils.JsonData
import whatcheer.utils.MiscUtil

object MainRoutes extends RestHelper {
  serve {
    case ".well-known" :: "webfinger" :: Nil Get req =>
      WebFinger.service(req.param("resource"))

    case ".well-known" :: "nodeinfo" :: Nil Get req =>
      ("links" -> List(
        ("rel" -> "http://nodeinfo.diaspora.software/ns/schema/2.1") ~ ("href" -> f"https://${req
            .header("host")
            .openOr(Constants.Hostnames.head)}/${Constants.NodeInfoURI}/2.1"),
        ("rel" -> "http://nodeinfo.diaspora.software/ns/schema/2.0") ~ ("href" -> f"https://${req
            .header("host")
            .openOr(Constants.Hostnames.head)}/${Constants.NodeInfoURI}/2.0")
      )) ~ (".server_type" -> "What Cheer, Netop")

    case Constants.NodeInfoURI :: version :: Nil Get _ =>
      NodeInfo.getNodeInfo(version)

    case "ap" :: "o" :: id :: Nil Get req =>
      () => ObjectLogic.serveObject(id, req.hostName)

    case "ap" :: "users" :: id :: Nil Get req =>
      () => ActorLogic.serveActor(id, req.hostName)
    case "ap" :: "users" :: id :: "inbox" :: Nil Get _ => ???
    case "ap" :: "users" :: id :: "followers" :: rest Get req =>
      () => ActorLogic.serveFollowers(id, req.hostName, rest)
    case "ap" :: "users" :: id :: "following" :: rest Get _ => ???
    case "ap" :: "users" :: id :: "outbox" :: rest Get _    => ???

    case "api" :: "V1" :: "accounts" :: id :: Nil Get _ => ???
    case "api" :: "V1" :: "accounts" :: id :: "featured_tags" :: Nil Get _ =>
      ???
    case "api" :: "V1" :: "accounts" :: id :: "follow" :: Nil Get _    => ???
    case "api" :: "V1" :: "accounts" :: id :: "followers" :: Nil Get _ => ???
    case "api" :: "V1" :: "accounts" :: id :: "following" :: Nil Get _ => ???
    case "api" :: "V1" :: "accounts" :: id :: "lists" :: Nil Get _     => ???
    case "api" :: "V1" :: "accounts" :: id :: "statuses" :: Nil Get _  => ???
    case "api" :: "V1" :: "accounts" :: id :: "unfollow" :: Nil Get _  => ???

    case "api" :: "V1" :: "accounts" :: "relationships" :: Nil Get _      => ???
    case "api" :: "V1" :: "accounts" :: "update_credentials" :: Nil Get _ => ???
    case "api" :: "V1" :: "accounts" :: "verify_credentials" :: Nil Get _ => ???

    case "api" :: "V1" :: "apps" :: Nil Get _                         => ???
    case "api" :: "V1" :: "apps" :: "verify_credentials" :: Nil Get _ => ???

    case "api" :: "V1" :: "blocks" :: Nil Get _        => ???
    case "api" :: "V1" :: "custom_emojis" :: Nil Get _ => ???
    case "api" :: "V1" :: "filters" :: Nil Get _       => ???

    case "api" :: "V1" :: "instance" :: Nil Get _            => ???
    case "api" :: "V1" :: "instance" :: "peers" :: Nil Get _ => ???
    case "api" :: "V1" :: "instance" :: "rules" :: Nil Get _ => ???

    case "api" :: "V1" :: "mutes" :: Nil Get _ => ???

    case "api" :: "V1" :: "notifications" :: Nil Get _       => ???
    case "api" :: "V1" :: "notifications" :: id :: Nil Get _ => ???

    case "api" :: "V1" :: "push" :: "subscriptions" :: Nil Get _ => ???

    case "api" :: "V1" :: "statuses" :: Nil Get _                      => ???
    case "api" :: "V1" :: "statuses" :: id :: Nil Get _                => ???
    case "api" :: "V1" :: "statuses" :: id :: "context" :: Nil Get _   => ???
    case "api" :: "V1" :: "statuses" :: id :: "favourite" :: Nil Get _ => ???
    case "api" :: "V1" :: "statuses" :: id :: "reblog" :: Nil Get _    => ???

    case "api" :: "V1" :: "tags" :: id :: Nil Get _ => ???

    case "api" :: "V1" :: "timelines" :: "home" :: Nil Get _      => ???
    case "api" :: "V1" :: "timelines" :: "public" :: Nil Get _    => ???
    case "api" :: "V1" :: "timelines" :: "tag" :: id :: Nil Get _ => ???

    case "api" :: "V1" :: "trends" :: "links" :: Nil Get _    => ???
    case "api" :: "V1" :: "trends" :: "statuses" :: Nil Get _ => ???

    case "api" :: "V2" :: "instance" :: Nil Get _    => ???
    case "api" :: "V2" :: "media" :: Nil Post _      => ???
    case "api" :: "V2" :: "media" :: id :: Nil Put _ => ???
    case "api" :: "V2" :: "search" :: Nil Get _      => ???

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

  implicit def jsonDataLiftResponse(
      in: Box[(JsonData, MiscUtil.Headers)]
  ): Box[LiftResponse] = {
    in match {
      case Full(jd -> headers) =>
        Full(JsonResponse(jd.toJson(), headers, Nil, 200))
      case ParamFailure(msg, _, _, code: Int) =>
        Full(PlainTextResponse(msg, code))
      case Failure(msg, _, _) => Full(PlainTextResponse(msg, 404))
      case _                  => Empty
    }
  }

  implicit def jsonToLiftResponse(
      in: Box[(JValue, MiscUtil.Headers)]
  ): Box[LiftResponse] = {
    in match {
      case Full(jd -> headers) =>
        Full(JsonResponse(jd, headers, Nil, 200))
      case ParamFailure(msg, _, _, code: Int) =>
        Full(PlainTextResponse(msg, code))
      case Failure(msg, _, _) => Full(PlainTextResponse(msg, 404))
      case _                  => Empty
    }
  }
}
