package whatcheer.logic

import net.liftweb.common._
import scala.util.matching.Regex
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import whatcheer.models.Actor

object WebFinger {
  val acctPattern: Regex = "acct:[@~]?([^@]+)@?(.*)".r

  def parse(acct: Box[String]): Box[String] = {
    acct match {
      case Full(acctPattern(user, host)) =>
        if (host.toLowerCase() == Constants.Hostname)
          Full(user)
        else
          ParamFailure("Requested User is not from this domain", 400)
      case _ =>
        ParamFailure(
          """Bad request. Please make sure "acct:USER@DOMAIN" is what you are sending as the "resource" query parameter.""",
          400
        )
    }
  }

  def service(resource: Box[String]): Box[JValue] = {
    for {
      target <- WebFinger.parse(resource)
      myResource <- resource ?~ "failed to include resource" ~> 400
      actor <- Actor.targetActor(target) 

    } yield ("subject" -> myResource) ~ ("links" -> List(
      ("rel" -> "self") ~
        ("type" -> "application/activity+json") ~
        ("href" -> actor.buildActorIRI())
    ))
  }

}
