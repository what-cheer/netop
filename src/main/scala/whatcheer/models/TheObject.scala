package whatcheer.models


import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedPoliteString
import net.liftweb.mapper.UpdatedTrait
import net.liftweb.mapper.CreatedTrait
import net.liftweb.mapper.MappedDateTime
import java.util.Date
import net.liftweb.mapper.By
import net.liftweb.common.Box
import whatcheer.logic.Constants
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import net.liftweb.util.TimeHelpers
import net.liftweb.common.Logger
import net.liftweb.common.{Full, Empty}
import net.liftweb.common.ParamFailure
import java.net.URL
import net.liftweb.mapper.MappedText
import net.liftweb.util.Helpers
import java.security.KeyPairGenerator
import net.liftweb.mapper.MappedForeignKey
import net.liftweb.mapper.MappedLongForeignKey
import net.liftweb.mapper.MappedEnum
import net.liftweb.mapper.Index
import net.liftweb.json.JsonAST
import net.liftweb.mapper.DBIndexed

object TheObject
    extends TheObject
    with LongKeyedMetaMapper[TheObject] {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[TheObject])
      
}

class TheObject
    extends LongKeyedMapper[TheObject]
    with IdPK
    with CreatedTrait {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[TheObject])

  def getSingleton = TheObject

  object objectType extends MappedEnum(this, ObjectTypes)
  object index extends MappedPoliteString(this, 500) with DBIndexed
        object objectValue extends MappedText(this)
}

object ObjectTypes extends Enumeration {
  type ObjectType = Value

  val Unknown =
    Value
}
