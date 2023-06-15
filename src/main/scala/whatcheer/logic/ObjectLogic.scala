package whatcheer.logic

import whatcheer.utils.MiscUtil
import whatcheer.schemas.Objects
import net.liftweb.common.Box
import whatcheer.utils.JsonData

object ObjectLogic {
  /**
    * Create a full URI (for looking up an object)
    *
    * @param id
    * @param domain
    * @return
    */
    // def uri(id: String, domain: String): String = f"https://${domain.trim().toLowerCase()}/ap/o/${MiscUtil.urlEncode(id)}"
def primaryKey(id: String, domain: String): String = f"${id.trim().toLowerCase()}@${domain.trim().toLowerCase()}"

    def lookupObject(id: String, domain: String): Box[Objects] = Objects.findByKey(primaryKey(id, domain))

    def serveObject(id: String, domain: String): Box[(JsonData, MiscUtil.Headers)] = 
      for {
        domain <- (Box !! domain) ?~ "Domain must be specified" ~> 400
        id <- (Box !! id) ?~ "id must be specified" ~> 400
        key = primaryKey(id, domain)
        obj <- Objects.findByKey(key) ?~ f"Object '${key}' not found" ~> 404
      } yield (obj.asJsonInterface(), MiscUtil.corsAndActivityPubHeader)
}

