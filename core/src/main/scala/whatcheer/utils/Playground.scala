package whatcheer.utils

import java.io.FileInputStream
import com.github.jsonldjava.utils.JsonUtils
import java.util.HashMap
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import java.security.KeyPairGenerator
import java.util.Base64
import whatcheer.schemas.Schemas._
import java.net.URL

case class DogMeat(
    var a: Int,
    var b: Option[String] = None,
    var c: Option[Int] = None
) {
  def a(i: Int): DogMeat = {
    this.a = i
    this
  }

  def b(i: String): DogMeat = {
    this.b = Some(i)
    this
  }

  def b(i: Option[String]): DogMeat = {
    this.b = i
    this
  }
}

object Playground {
  val keyGen = KeyPairGenerator.getInstance("RSA")

  def run() = {
    var x: Privacy = Privacy_public
    x match {
      case Privacy_direct =>
    }

    val m = MastodonAccount()
    m.created_at.getEpochSecond()

    val q = NotificationsQueryResult(
      id = new URL("https://example.com"),
    )
  }

  def run33() = {
    // BullBear.bar()
    val a = DogMeat(33)
    val b = DogMeat(42, c = Some(44))

    a.b = Some("Hello")

    a.a(99).b("meow")
    (a, b)

  }

  //   @macros.identity object Sloth {

  // }

  object BullBear {
    def foo() = "33"
    def bar() = "343"
  }

  def run1(): Any = {
    val BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    val END_CERT = "-----END CERTIFICATE-----";
    val LINE_SEPARATOR = System.getProperty("line.separator");

    keyGen.initialize(4096);
    val keyPair = keyGen.genKeyPair()
    val encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

    val rawCrtText = keyPair.getPublic().getEncoded();
    val encodedCertText = new String(encoder.encode(rawCrtText));
    val prettified_cert =
      BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    return prettified_cert;
  }
}
