package whatcheer.utils

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64

object CryptoUtil {
  val BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
  val END_CERT = "-----END CERTIFICATE-----";
  val LINE_SEPARATOR = System.getProperty("line.separator");

  def keyBytesToPrettyText(key: Array[Byte]): String = {

    val encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

    val encodedCertText = new String(encoder.encode(key));
    val prettified_cert =
      BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    return prettified_cert;
  }

  def generateKeyPair(): KeyPair = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(4096);
    val keyPair = keyGen.genKeyPair()
    keyPair
  }
}
