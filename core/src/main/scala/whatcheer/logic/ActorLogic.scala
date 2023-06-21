package whatcheer.logic

import net.liftweb.common._
import whatcheer.schemas.Actor
import whatcheer.utils.MiscUtil
import whatcheer.utils.JsonData
import whatcheer.schemas.ActorFollowing
import net.liftweb.mapper.By
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object ActorLogic {
  type ServeResp = Box[(JsonData, MiscUtil.Headers)]
  type ServeJsonResp = Box[(JsonAST.JValue, MiscUtil.Headers)]
  def parseHandle(id: String): Box[(String, String)] = {
    id match {
      case Actor.idPattern(id, domain)
          if id.trim().length() > 0 && domain.trim().length() > 0 =>
        Full(id.trim().toLowerCase() -> domain.trim().toLowerCase())
      case _ => Empty
    }
  }

  // def uri(id: String, domain: String): String = f"https://${domain}/ap/users/${MiscUtil.urlEncode(id)}"
  def primaryKey(id: String, domain: String): String =
    f"${id.trim().toLowerCase()}@${domain.trim().toLowerCase()}"

  def findActor(
      id: String,
      domain: String
  ): Box[Actor] = {
    for {
      (theId, theDomain) <- parseHandle(
        id
      ) ?~ "Failed to parse the supplied ID" ~> 400
      theDomain <- Full(theDomain).filter(
        _ == domain
      ) ?~ f"Domain ${theDomain} doesn't match requested domain ${domain}" ~> 400
      key = primaryKey(theId, theDomain)
      actor <- Actor.findByKey(key) ?~ f"The user ${key} not found" ~> 404
    } yield actor
  }
  def serveActor(
      id: String,
      domain: String
  ): ServeResp = {
    for {
      actor <- findActor(id, domain)
    } yield (actor.asJsonInterface() -> MiscUtil.corsAndActivityPubHeader)
  }

  def serveFollowers(
      id: String,
      domain: String,
      info: List[String]
  ): ServeJsonResp = {
    info match {
      case Nil =>
        for {
          actor <- findActor(id, domain)
          followerCount = ActorFollowing.count(
            By(ActorFollowing.targetActorId, actor),
            By(ActorFollowing.state, "accepted")
          )
        } yield (
          ("id" -> actor.followersUrl) ~ ("type" -> "OrderedCollection") ~ ("totalItems" -> followerCount) ~
            ("first" -> (actor.followersUrl + "/page")) ~ ("last" -> (actor.followersUrl + "/page?min_id=0")),
          MiscUtil.corsAndActivityPubHeader
        )
      case "page" :: Nil => ???
      case _             => ParamFailure("Uknown Resource", 404)

    }
  }
}
