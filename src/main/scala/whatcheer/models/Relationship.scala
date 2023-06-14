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

/*
object Relationship
    extends Relationship
    with LongKeyedMetaMapper[Relationship] {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[Relationship])

  /** Creating some compound indexes for owner/relationship and
    * owner/relationship/createdAt
    *
    * @return
    */
  override def dbIndexes = Index(actor, relationshipType) :: Index(
    actor,
    relationshipType,
    createdAt
  ) :: super.dbIndexes

}

class Relationship
    extends LongKeyedMapper[Relationship]
    with IdPK
    with CreatedTrait {
  // Logger must be lazy, since we cannot instantiate until after boot is complete
  private lazy val logger = Logger(classOf[Relationship])

  def getSingleton = Relationship

  object actor extends MappedLongForeignKey(this, Actor)
  object relationshipType extends MappedEnum(this, RelationshipTypes)
  object target extends MappedPoliteString(this, 500) {
    override def setFilter = trim _ :: toLower _ :: super.setFilter
  }

}

object RelationshipTypes extends Enumeration {
  type RelationshipType = Value

  val Following, Follower, Liked, Bookmark, Share, Likes, Blocked, Rejected =
    Value
}
*/