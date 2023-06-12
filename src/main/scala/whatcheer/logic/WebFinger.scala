package whatcheer.logic

import net.liftweb.common._
import scala.util.matching.Regex
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import whatcheer.models.Actor

/**
  * Implement the logic behind WebFinger. The conversion of the values
  * to JSON-LD and the HTTP servicing is performed outside of this module.
  */
object WebFinger {
  /**
    * The regular expression for the account request
    */
  val acctPattern: Regex = "acct:[@~]?([^@]+)@?(.*)".r

  /**
    * Given an account in the form required by WebFinger (acct:user@domain),
    * ensure that the resource is properly formatted
    *
    * @param resource - the resource to test... note that the resource is pulled from the `resource` http request parameter, thus could
    * be `Empty`.
    * @return
    */
  def parse(resource: Box[String]): Box[String] = {
    resource match {
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

  /**
    * Service the WebFinger request based on a `resource` that's passed in.
    *
    * @param resource the value of the `resource` parameter
    * @return The JSON (without conversion to JSON LD)
    */
  def service(resource: Box[String]): Box[JValue] = {
    for {
      target <- WebFinger.parse(resource)
      myResource <- resource ?~ "failed to include resource" ~> 400
      actor <- Actor.targetActor(target) 

    } yield ("subject" -> myResource) ~ ("links" -> List(
      ("rel" -> "self") ~
        ("type" -> "application/activity+json") ~
        ("href" -> Actor.buildActorIRI(actor))
    ))
  }

}
