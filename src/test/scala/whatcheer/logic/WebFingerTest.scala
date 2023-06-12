package whatcheer.logic

import whatcheer.models.Actor
import net.liftweb.util.Props
import bootstrap.liftweb.Boot
import net.liftweb.common.Full
import net.liftweb.common.ParamFailure
import net.liftweb.json.JsonAST

/**
  * Test that the web finger code is working
  *
  */
class WebFingerTest extends munit.FunSuite {
  Boot.boot

  test("Get Test Actor") {
    assert(Props.testMode)
    val testActor = Actor.findActor("test")

    assert(testActor.isDefined)
  }

  test("WebFinger for test actor") {
    val answer = WebFinger.service(Full("acct:test@localhost"))
    assert(answer.isDefined)
    val json = answer.openOrThrowException("Tested above")
    assertEquals(json \ "subject", JsonAST.JString("acct:test@localhost"))
    assertEquals(
      (json \ "links")(0) \ "href",
      JsonAST.JString("http://localhost:8080/u/test")
    )
  }

  test("WebFinger error on wrong domain") {
    val answer = WebFinger.service(Full("acct:test@frog"))
    assert((answer: @unchecked) match {
      case ParamFailure(msg, _, _, code: Int)
          if msg
            .toLowerCase()
            .contains("not from this domain") && code == 400 =>
        true
    })
  }

  test("WebFinger error on wrong input") {
    val answer = WebFinger.service(Full("account:test@frog"))
    assert((answer: @unchecked) match {
      case ParamFailure(msg, _, _, code: Int)
          if msg
            .contains("acct:USER@DOMAIN") && code == 400 =>
        true
    })
  }

  test("WebFinger error on unknown user") {
    val answer = WebFinger.service(Full("acct:frogbutt@localhost"))
    assert((answer: @unchecked) match {
      case ParamFailure(msg, _, _, code: Int)
          if msg
            .toLowerCase()
            .contains("not found") && code == 404 =>
        true
    })
  }
}
