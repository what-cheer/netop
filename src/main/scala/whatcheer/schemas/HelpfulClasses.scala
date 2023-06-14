package whatcheer.schemas

import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
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
import net.liftweb.util.Props
import net.liftweb.json.JsonAST
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import whatcheer.utils.CryptoUtil
import net.liftweb.db.DriverType
import net.liftweb.mapper.BaseKeyedMapper
import net.liftweb.mapper.Mapper
import net.liftweb.mapper.IndexedField
import net.liftweb.mapper.KeyedMapper
import net.liftweb.mapper.KeyedMetaMapper
import net.liftweb.mapper.DBIndexed
import net.liftweb.mapper.MappedStringForeignKey
import net.liftweb.mapper.UniqueIndex
import net.liftweb.mapper.BaseMapper
import net.liftweb.mapper.MappedBoolean
import net.liftweb.mapper.MappedField
import net.liftweb.mapper.BaseIndex
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.MetaMapper

object DBConsts {
  def primaryKeyLen = 500
}
abstract class MappedJson[T <: Mapper[T]](theFieldOwner: T)
    extends MappedText[T](theFieldOwner) {
  def getJson(): Box[JsonAST.JValue] = parseOpt(this.get)

  def setJson(jv: JsonAST.JValue): T = {
    this.apply(compactRender(jv))
  }

  def apply(jv: JsonAST.JValue): T = { setJson(jv) }

  // By default, primary keys are auto-generated... but in our case, the primary keys
  // are text fields that may be meaningful
  override def dbAutogenerated_? : Boolean = false
}

abstract class MappedStringIndex[T <: Mapper[T]](theOwner: T, len: Int)
    extends MappedString[T](theOwner, len)
    with IndexedField[String] {

  override def writePermission_? = true
  override def dbIndexed_? = true

  def defined_? = i_is_! != defaultValue
  override def dbPrimaryKey_? = true

  override def defaultValue = Helpers.nextFuncName

  override def setFilter: List[String => String] =
    trim _ :: toLower _ :: super.setFilter

  override def dbIndexFieldIndicatesSaved_? = { i_is_! != defaultValue }

  override def dbDisplay_? = false

  override def fieldCreatorString(dbType: DriverType, colName: String): String =
    colName + " TEXT NOT NULL"

  override def convertKey(in: String): Box[String] = in match {
    case null                 => Empty
    case x if x.length() == 0 => Empty
    case x                    => Full(x.toLowerCase())
  }

  override def convertKey(in: Int): Box[String] = convertKey(in.toString())

  override def convertKey(in: Long): Box[String] = convertKey(in.toString())

  override def convertKey(in: AnyRef): Box[String] = in match {
    case null => Empty
    case x    => convertKey(x.toString())
  }

  override def makeKeyJDBCFriendly(in: String): AnyRef = in

}

trait BaseStringKeyedMapper extends BaseKeyedMapper {
  override type TheKeyType = String
}

trait StringKeyedMapper[OwnerType <: StringKeyedMapper[OwnerType]]
    extends KeyedMapper[String, OwnerType]
    with BaseStringKeyedMapper {
  self: OwnerType =>
}

trait StringIdPK {
  self: BaseStringKeyedMapper =>
  def primaryKeyField: MappedStringIndex[MapperType] = id
  object id
      extends MappedStringIndex[MapperType](
        this.asInstanceOf[MapperType],
        DBConsts.primaryKeyLen
      ) {}
}

trait StringKeyedMetaMapper[A <: StringKeyedMapper[A]]
    extends KeyedMetaMapper[String, A] { self: A => }

trait Properties {
  self: BaseMapper =>

  object properties
      extends MappedJson[MapperType](this.asInstanceOf[MapperType]) {
    override def dbNotNull_? = true
    override def defaultValue: String = "{}"
  }
}
