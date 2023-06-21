package whatcheer.schemas

import whatcheer.macros
import whatcheer.macros.support.{BuildJson, Jsonifyable}

trait FancyTrait
case class Evil(a: Int, b: Int) extends BuildJson {
  def buildJson(): net.liftweb.json.JsonAST.JValue =
    net.liftweb.json.JsonAST.JNull
}

case class Slorp() {
  def buildJson(): net.liftweb.json.JsonAST.JValue =
    net.liftweb.json.JsonAST.JNull
}

import java.util.Date

@macros.jsonify
object Schemas {
  object MrYak extends Jsonifyable {
    val _fields_ = (
      B("https://www.w3.org/ns/activitystreams"),
      M(MrMoose),
      E(FancyTrait),
      (foo: Int_?, Some(33), "#meow"),
      foo2: scala.Option[Int],
      bar: scala.Float,
      myStr: String_?,
      myDate: Date_?,
      myDouble: Double,
      myChar: Char,
      myList: List[Int],
      maybeList: Option[List[Int]],
      (moose: MrMoose, MrMoose(barBell = 3)),
      mooseLst: List[MrMoose],
      slorp: Slorp_?,
      mooseOptLst: Option[List[MrMoose]],
      bb: Boolean,
      (bi: BigInt, 0),
      (dog: Int, 33),

      // (sloth: Evil, Evil(33, 44), "#woof")
      (sloth: Evil, Evil(1, 2))
    )

    lazy val qq = MrYak(67) // , sloth = Evil(33, 11))
  }

  

  object MrMoose extends Jsonifyable {
    val _fields_ = (
      (baz: Int, 42),
      barBell: Float
    )
  }

  var qq: scala.Option[MrMooseTrait] = None

  lazy val m = MrYak(42)

  m.copy(baz = 34, foo = Some(42)).barBell

  m.setExtra("key", JInt(343))
  m.setExtra("dorrr", JString("hjello"))

  def doIt(): JValue = {

    val z = JInt(33)
    val q: Int = z.num.toShort

    lazy val m = MrYak(42)

    m.copy(baz = 34, foo = Some(42))
      .setExtra("key", JInt(343))
      .setExtra("dorrr", JString("hjello"))
      .buildJson()
  }

  JString("hellos")

  implicit def toOption[T](v: T): Option[T] = v match {
    case null => None
    case v    => Some(v)
  }
}
