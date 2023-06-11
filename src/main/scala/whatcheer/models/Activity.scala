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

object Activity
    extends Activity
    with LongKeyedMetaMapper[Activity] {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[Activity])
        def create(actor: Actor, activityType: ActivityTypes.Value, to: List[String], etc: JsonAST.JValue): Activity = {
            ???
        }
}

class Activity
    extends LongKeyedMapper[Activity]
    with IdPK
    with CreatedTrait {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[Activity])

  def getSingleton = Activity

  object actor extends MappedLongForeignKey(this, Actor)
  object activityType extends MappedEnum(this, ActivityTypes)
private object to extends MappedText(this)
private object etc extends MappedText(this)
}

object ActivityTypes extends Enumeration {
  type ActivityType = Value

  val Create, Follow, Like, Announce, Block, Update, Arrive =
    Value
}