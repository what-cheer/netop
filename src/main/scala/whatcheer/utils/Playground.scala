package whatcheer.utils

import java.io.FileInputStream
import com.github.jsonldjava.utils.JsonUtils
import java.util.HashMap
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import java.security.KeyPairGenerator
import java.util.Base64

object Playground {
   val keyGen = KeyPairGenerator.getInstance("RSA")
   
  def run(): Any = {
    val BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
val END_CERT = "-----END CERTIFICATE-----";
val LINE_SEPARATOR = System.getProperty("line.separator");
   
    keyGen.initialize(4096);
    val keyPair = keyGen.genKeyPair()
        val  encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

    val rawCrtText = keyPair.getPublic().getEncoded();
    val encodedCertText = new String(encoder.encode(rawCrtText));
    val prettified_cert = BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    return prettified_cert;
  }
}
