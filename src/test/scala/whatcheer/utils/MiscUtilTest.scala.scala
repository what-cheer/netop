package whatcheer.utils

import net.liftweb.json.JsonAST

class MiscUtilTest extends munit.FunSuite {
  import Jsonable._

  test("fancy JSON") {
    val x = f"""{"links": [
                {"rel": "http://nodeinfo.diaspora.software/ns/schema/2.1",
                 "href": "https://${"Yakss".length()}/2.1"},
                {"rel": "http://nodeinfo.diaspora.software/ns/schema/2.0",
                 "href": "https://${"moose".length()}/2.0"}
    ]}""".json

    assertNotEquals(x, JsonAST.JNull.asInstanceOf[JsonAST.JValue])
    assertEquals(
      (x \ "links")(0) \ "rel",
      JsonAST.JString("http://nodeinfo.diaspora.software/ns/schema/2.1")
    )
    assertEquals(
      (x \ "links")(1) \ "href",
      JsonAST.JString("https://5/2.0")
    )
  }
}
