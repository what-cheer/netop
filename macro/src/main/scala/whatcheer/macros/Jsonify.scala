package whatcheer.macros

import scala.reflect.macros.blackbox.Context
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.reflect.api.Positions

@compileTimeOnly("enable macro paradise to expand macro annotations")
class jsonify extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonifyMacro.impl
}

object UnionBuilder {
  def buildTraitsAndObjects(
      c: whitebox.Context,
      baseName: String,
      union: List[String]
  ): List[c.universe.Tree] = {
    import c.universe._

    val theTrait = q"""
    sealed trait ${TypeName(baseName)} extends BuildJson {
      def unionString: String

      def buildJson(): net.liftweb.json.JsonAST.JValue = buildJson_includingPrivate()
      def buildJson_includingPrivate(): net.liftweb.json.JsonAST.JValue = JString(this.unionString)
    }
    """

    val caseObjects = union.map { name =>
      q"""
    case object ${TermName(baseName + "_" + name)} extends ${TypeName(
          baseName
        )} {
      override def unionString = ${name}
      
    }
  """
    }

    theTrait :: caseObjects
  }

  def buildExtractors(
      c: whitebox.Context,
      baseName: String,
      union: List[String]
  ): List[c.universe.Tree] = {
    import c.universe._
    val theImport = q"import net.liftweb.json._"
    val extract = q"""
    def extractFromJson(v: JValue): Option[${TypeName(baseName)}] = 
      extractFromJson_includingPrivate(v: JValue)
    """

    val inclPriv =
      q"""
      def extractFromJson_includingPrivate(v: JValue): Option[${TypeName(
          baseName
        )}] = v match {
        case ..${union.map { name =>
          cq"""JString(${name}) => Some(${TermName(baseName + "_" + name)})
            """
        }}
        case _ => None
      }
      """
    theImport :: extract :: inclPriv :: Nil
  }
}

object JsonifyMacro {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // convert an identity to a type... expand the type
    def identToTypeStr(id: Tree): String = {
      try { c.typecheck(id, mode = c.TYPEmode).tpe.toString() }
      catch { case _: Exception => id.toString() }
    }

    def identToTypeInfo(id: Tree): Option[(Tree, c.Type)] = {
      try {
        val ret = c.typecheck(id, mode = c.TYPEmode)

        Some(ret -> ret.tpe)
      } catch {
        case e: Exception =>
          None
      }
    }

    def buildExtractor(
        pd: ParsedDef,
        fields: List[FieldDef],
        priv: Boolean
    ): List[Tree] = {
      def termName(fd: FieldDef) = TermName(fd.name + "_local")
      val defaultValue: Tree = if (fields.forall(_.hasDefault_?)) {
        q"""Some(this.apply()) """
      } else {
        q"""None"""
      }

      val fieldList = fields.map { f =>
        q"""var ${termName(f)}: Option[${f.theType.tpe}] =  None"""
      }

      lazy val theMap: Tree = q"""
      val theFieldMapTree_json: Map[String, Int] = Map( ..${fields.zipWithIndex
          .map { fi =>
            q"""Tuple2(${fi._1.name} , ${fi._2}) """
          }})
       """

      def matcherAndExtractors(priv: Boolean): List[CaseDef] =
        fields.zipWithIndex.map { case (theDef, idx) =>
          cq"""${idx} => ${termName(theDef)} = ${if (priv || !theDef.private_?)
              theDef.buildExtractorFrom(
                "fieldData",
                priv
              )
            else q"None"} """
        }

      val testerAndSetters: List[Tree] = fields.map { ff =>
        val name = ff.name
        val defValue = ff.getOrFindDefaultValue
        val localVarName = termName(ff)
        defValue match {
          case Some(defValue) =>
            q"""if (${localVarName}.isEmpty) 
              ${localVarName} = Some(${defValue}) """
          case _ => q"None"
        }
      }

      def buildFieldTree(fields: List[FieldDef]): Tree =
        fields match {
          case v :: Nil => q"(${termName(v)}.isDefined)"
          case v :: rest =>
            q"(${termName(v)}.isDefined).&&(${buildFieldTree(rest)})"
          case Nil => q"false"

        }

      val ret = List(
        if (priv) q"" else theMap,
        if (priv) q"" else q"""
      import net.liftweb.json._""",
        q"""
      def ${TermName(
            if (priv) "extractFromJson_includingPrivate" else "extractFromJson"
          )}(v: JValue): Option[${Ident(
            TypeName(pd.name)
          )}] = v match {
        case JObject(__fields__) => {
          var _extra_local: Map[String, JValue] = Map();

          ..${fieldList}

          __fields__.foreach{fieldValue => 
            val fieldName = fieldValue.name
            val fieldData = fieldValue.value
            val fieldIndex: Option[Int] = theFieldMapTree_json.get(fieldName)
            if (fieldIndex.isDefined) {
              val realFieldIndex = fieldIndex.get
              realFieldIndex match {case ..${matcherAndExtractors(priv)}}
            } else {
              _extra_local = _extra_local + (fieldName -> fieldData)
            }
          }

          ..${testerAndSetters}

          if (${buildFieldTree(fields)}) {Some(this.apply(..${fields
            .map(f => q"${termName(f)}.get")}, _extra_local))} else {

            ${defaultValue}
          }
        }
        case _ => ${defaultValue}
      }
       """
      )

      // println(f"extractor be ${ret}")

      ret
    }

    def rebuildModule(pd: ParsedDef, fields: List[FieldDef]): ModuleDef = {
      val ModuleDef(mods, name, template) = pd.objectModule
      val Template(templateInfo, valDef, trees) = template

      ModuleDef(
        mods,
        name,
        Template(
          templateInfo,
          valDef,
          buildExtractor(pd, fields, false) ::: buildExtractor(
            pd,
            fields,
            true
          ) ::: trees
        )
      )

    }

    // An extractor
    object FindAndFixJsonifyable {

      // take a template. If the Template is extends the marker trait whatcheer.macros.support.Jsonifyable
      // then look for `val _fields_` and separate it out for further processing
      def unapply(t: Template): Option[(Template, ValDef)] = {
        val Template(types, vd, kids) = t

        // ensure the trait derives from the Jsonifyable macro trait
        val mt = types
          .map(v => identToTypeStr(v))
          .filter(_ == "whatcheer.macros.support.Jsonifyable")

        if (mt.isEmpty) None
        else {
          // separate the `_fields_` val from the rest of the tree
          // and remove it from the tree because it will not actually compile
          val fieldKids = kids.map {
            // separate the fields definition
            case v @ ValDef(_, TermName("_fields_"), _, _) => (None, Some(v))
            case v                                         => (Some(v), None)
          }

          // if we found `_fields_`, extract the result
          fieldKids
            .flatMap(_._2)
            .headOption
            .map(v => (Template(types, vd, fieldKids.flatMap(_._1)), v))
        }
      }
    }

    def buildUnion(in: List[Tree]): List[Tree] = {
      object UnionTemplate {
        def unapply(in: Template): Option[Template] = in match {
          case t @ Template(types, _, _)
              if !types
                .map(v => identToTypeStr(v))
                .filter(_ == "whatcheer.macros.support.StringUnion")
                .isEmpty =>
            Some(t)
          case _ => None
        }
      }

      object StringConsts {
        def unapply(in: List[Tree]): Option[List[String]] = {
          val possible = in.map {
            case Literal(Constant(str: String)) => Some(str)
            case _                              => None
          }

          if (possible.forall(_.isDefined)) Some(possible.flatten)
          else None

        }
      }

      def findUnionAndGenerateCaseObjects(
          name: String,
          kids: List[Tree]
      ): (List[Tree], List[Tree]) = {
        val valDef = kids.collect {
          case ValDef(_, TermName("_union_"), _, kidz) => kidz
        }

        valDef match {
          case Apply(_, StringConsts(stringz)) :: _ =>
            (
              UnionBuilder.buildTraitsAndObjects(c, name, stringz),
              UnionBuilder.buildExtractors(c, name, stringz) ::: kids
            )
          case x :: _ =>
            c.abort(
              x.pos,
              f"The `val _union_` must be a `List` of `String` literals"
            )
          case _ =>
            c.abort(
              kids.head.pos,
              f"An object with the `StringUnion` marker must have a `val _union_` field defining the union of string constants"
            )
        }

      }

      in.flatMap {
        case ModuleDef(
              mods,
              theName @ TermName(name),
              UnionTemplate(Template(types, vd, kids))
            ) =>
          val (added, updatedKids): (List[Tree], List[Tree]) =
            findUnionAndGenerateCaseObjects(name, kids)
          ModuleDef(mods, theName, Template(types, vd, updatedKids)) :: added

        case v => List(v)
      }
    }
    // object FindAndFixUnion {

    //   // take a template. If the Template is extends the marker trait whatcheer.macros.support.Jsonifyable
    //   // then look for `val _fields_` and separate it out for further processing
    //   def unapply(t: Template): Option[Template] = {
    //     val Template(types, vd, kids) = t

    //     val () // FIXME

    //     // ensure the trait derives from the StringUnion macro trait
    //     val mt = types
    //       .map(v => identToTypeStr(v))
    //       .filter(_ == "whatcheer.macros.support.StringUnion")

    //     println(f"mt is ${mt}")
    //     if (mt.isEmpty) Some(t)
    //     else {
    //       // separate the `_union_` val from the rest of the tree
    //       // and remove it from the tree because it will not actually compile
    //       val unionKids = kids.filter {
    //         // separate the fields definition
    //         case ValDef(_, TermName("_union_"), _, _) => true
    //         case _                                    => false
    //       }

    //       unionKids match {
    //         case ValDef(_, _, t1, t2) =>
    //           println(
    //             f"Matched t1 ${t1} ${t1.getClass()}\nt2 ${t2} ${t2.getClass()}"
    //           )
    //       }
    //       Some(t)

    //     }
    //   }
    // }

    def findAndFix(t: Tree): (Option[Tree], Option[BuildableInfo]) = t match {
      case ModuleDef(
            modifiers,
            tn @ TermName(name),
            FindAndFixJsonifyable(fixedTemplate, fields)
          ) =>
        (
          None,
          Some(
            BuildableInfo(name, fields, ModuleDef(modifiers, tn, fixedTemplate))
          )
        )
      case v => (Some(v), None)
    }

    def findItemsToJsonifyAndFixTree(
        tree: List[Tree]
    ): (List[Tree], List[BuildableInfo]) = {
      val fixed = tree.map(t => findAndFix(t))
      (fixed.flatMap(_._1), fixed.flatMap(_._2))
    }

    sealed trait DefInfo

    case class Merged(mergeWith: String) extends DefInfo

    case class BaseURL(url: String) extends DefInfo

    /** The extracted information needed to build a new class and fix the old
      */
    case class BuildableInfo(
        name: String,
        fields: ValDef,
        baseObject: ModuleDef
    )

    case class FieldDef(
        name: String,
        theType: TermAndType,
        private_? : Boolean,
        defaultValue: Option[Tree] = None,
        url: Option[String] = None
    ) extends DefInfo {

      def buildExtractorFrom(varName: String, priv: Boolean): Tree =
        theType.buildExtractorFrom(varName, priv)

      lazy val getOrFindDefaultValue: Option[Tree] =
        defaultValue.orElse {
          theType.mainType.defaultValue
        }

      def hasDefault_? : Boolean = getOrFindDefaultValue.isDefined
      // def fullType(): Tree = theType
    }

    case class ExtendDef(what: TypeName) extends DefInfo

    def buildTheTrait(className: String, fields: List[FieldDef]): Tree = {

      val theDefs =
        fields.map(v => q"""def ${TermName(v.name)}: ${v.theType.tpe}""")

      q"""trait ${TypeName(
          className
        )} extends whatcheer.macros.support.BuildJson {
        
        ..${theDefs}
        def _extra: Map[String, JValue]

        def setExtra(name: String, value: JValue): ${TypeName(className)}

        def removeExtra(name: String): ${TypeName(className)}
      }"""

    }

    def buildTheClass(
        className: String,
        fields: List[FieldDef],
        extendWhat: List[ExtendDef]
    ): Tree = {
      val theFields = fields.map { v =>
        v.getOrFindDefaultValue match {
          case Some(defValue) =>
            q"""val ${TermName(v.name)}: ${v.theType.tpe} = ${defValue} """
          case None => q"""var ${TermName(v.name)}: ${v.theType.tpe}  """
        }
      }

      // val extendIt = extendWhat.map(v => TypeName(v))
      val privateJsonOut: List[Tree] =
        fields.map(_.theType.buildJsonRenderer(true))

      val jsonOut: List[Tree] =
        fields.filter(f => !f.private_?).map(_.theType.buildJsonRenderer(false))

      var ret = q"""case class ${TypeName(
          className
        )}(..${theFields}, _extra: Map[String, JValue] = Map()) extends ${TypeName(
          f"${className}Trait"
        )} with ..${extendWhat.map(_.what)} {
          import net.liftweb.json._

        def setExtra(key: String, value: JValue): ${TypeName(
          className
        )} = this.copy(_extra = this._extra.updated(key, value))

        def removeExtra(key: String): ${TypeName(
          className
        )} = this.copy(_extra = (this._extra - key))

        def buildJson(): JValue = {
          JObject(List[Option[JField]](..${jsonOut}).flatten ::: _extra.toList.map(kv => JField(kv._1, kv._2)))
        }

        def buildJson_includingPrivate(): JValue = {
          JObject(List[Option[JField]](..${privateJsonOut}).flatten ::: _extra.toList.map(kv => JField(kv._1, kv._2)))
        }
      }"""

      // println(ret)

      ret
    }

    case class ParsedDef(
        name: String,
        merged: List[Merged],
        baseUrls: List[BaseURL],
        fields: List[FieldDef],
        extended: List[ExtendDef],
        objectModule: ModuleDef
    ) {
      def traitName: String = f"${name}Trait"

      def allFields(info: Map[String, ParsedDef]): List[FieldDef] = {
        val toMerge = merged.map(m =>
          info.get(m.mergeWith) match {
            case Some(it) => it
            case None =>
              c.abort(
                c.enclosingPosition,
                f"""Attempting to merge ${m.mergeWith} into ${name}. But the definition is not found in ${info.keySet}"""
              )
          }
        )

        val allFields: List[FieldDef] =
          (fields :: toMerge.map(_.allFields(info))).flatten
        allFields
      }

      def allBaseUrls(info: Map[String, ParsedDef]): List[BaseURL] = {
        val toMerge = merged.map(m =>
          info.get(m.mergeWith) match {
            case Some(it) => it
            case None =>
              c.abort(
                c.enclosingPosition,
                f"""Attempting to merge ${m.mergeWith} into ${name}. But the definition is not found in ${info.keySet}"""
              )
          }
        )

        Set(
          (baseUrls :: toMerge.map(_.allBaseUrls(info))).flatten: _*
        ).toList
      }

      def allExtended(info: Map[String, ParsedDef]): List[ExtendDef] = {
        val toMerge = merged.map(m =>
          info.get(m.mergeWith) match {
            case Some(it) => it
            case None =>
              c.abort(
                c.enclosingPosition,
                f"""Attempting to merge ${m.mergeWith} into ${name}. But the definition is not found in ${info.keySet}"""
              )
          }
        )

        Set(
          (extended :: toMerge.map(_.extended) ::: toMerge.map(m =>
            List(ExtendDef(TypeName(m.traitName)))
          )).flatten: _*
        ).toList
      }

      def buildClasses(info: Map[String, ParsedDef]): List[Tree] = {
        val allFields = this.allFields(info)
        val allBaseUrls = this.allBaseUrls(info)
        val allExtended = this.allExtended(info)
        buildTheTrait(traitName, allFields) :: buildTheClass(
          name,
          allFields,
          allExtended
        ) :: rebuildModule(
          this,
          allFields
        ) :: Nil // List(buildTheTrait(name, ))
      }
    }

    object ExtractName {

      def unapply(in: Name): Option[String] = {
        val ret = in match {
          case TypeName(b) => Some(b)
          case TermName(b) => Some(b)
          case _ =>
            None
        }
        ret
      }

      def unapply(in: Tree): Option[String] = {
        val ret = in match {
          case Ident(ExtractName(b))                   => Some(b)
          case TypeName(b)                             => Some(b)
          case TermName(b)                             => Some(b)
          case AppliedTypeTree(_, ExtractName(a) :: _) => Some(a)
          case Select(_, ExtractName(a))               => Some(a)
          case _ =>
            None
        }

        ret
      }
    }

    case class TermAndType(
        name: String,
        mainType: TypeInfo,
        private_? : Boolean
    ) {
      def buildExtractorFrom(varName: String, priv: Boolean): Tree =
        mainType.buildExtractorFrom(varName, priv)

      def optional_? : Boolean = mainType.optional_?

      lazy val tpe: Tree = mainType.underlyingType
      def buildJsonRenderer(priv: Boolean): Tree =
        mainType.buildRenderer(name, priv)
    }

    object TermAndType {
      def apply(name: String, mainType: TypeInfo): TermAndType = {
        val ret = name match {
          case n2 if n2.startsWith("$bang") =>
            apply(n2.substring(5), mainType, true)
          case n2 if n2.startsWith("_") => apply(n2, mainType, true)
          case _                        => apply(name, mainType, false)
        }
        if (ret.private_? && !mainType.optional_?) {
          c.abort(
            c.enclosingPosition,
            f"""Field ${ret.name} marked as private and the associated type my be `Option` which can be expressed as `Option[${mainType}]`"""
          )
        }

        ret
      }
    }

    sealed trait TypeInfo {
      def defaultValue: Option[Tree]
      def buildRenderer(name: String, priv: Boolean): Tree

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree
      def underlyingType: Tree
      def buildExtractorFrom(varName: String, priv: Boolean): Tree
      def optional_? : Boolean = false
    }

    sealed abstract class TypeInfoClass extends TypeInfo {
      def buildRenderer(name: String, priv: Boolean): Tree =
        q"""Some(JField(${name}, ${renderFieldConstructor(
            Ident(TermName(name)),
            priv
          )})) """
    }

    object MapValueTypeInfo extends TypeInfoClass {
      lazy val underlyingType: Tree = tq"Map[String, JValue]"
      def defaultValue: Option[Tree] = Some(q"Map[String, JValue]()")

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JObject(${toRender}.map{
          case (k, v) => JField(k, v)
        }.toList) """

      override def toString(): String = "Map[String, JValue]"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = {

        q""" 
        ${TermName(varName)} match {
          case JObject(fields) => Some(Map(fields.map{case JField(k, v) => (k -> v)} :_ *))
          case _ => None
        }
        """
      }
    }
    object MapStringTypeInfo extends TypeInfoClass {
      lazy val underlyingType: Tree = tq"Map[String, String]"
      def defaultValue: Option[Tree] = Some(q"Map[String, String]()")

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JObject(${toRender}.map(info => info match {
          case (k, v) => JField(k, JString(v))
        }).toList) """

      override def toString(): String = "Map[String, String]"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = {

        q""" 
        ${TermName(varName)} match {
          case JObject(fields) =>
             Map(fields.flatMap{
              case JField(k, JString(v)) => Some((k , v))
              case _ => None
              } :_ *)
          case _ => None
        }
        """
      }
    }

    case class IntTypeInfo(underlyingName: String) extends TypeInfoClass {

      lazy val underlyingType: Tree = tq"${Ident(TypeName(underlyingName))}"
      def defaultValue: Option[Tree] = Some(q"0")

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JInt(${toRender}) """

      override def toString(): String = underlyingName

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = {
        underlyingName match {
          case "Int" => q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.intValue)
          case JDouble(v) => Some(v.intValue)
          case _ => None
        }
        """
          case "BigInt" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v)
          
          case _ => None
        }
        """
          case "Long" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.longValue)
          case JDouble(v) => Some(v.longValue)
          case _ => None
        }
        """
          case "Short" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.shortValue)
          case JDouble(v) => Some(v.shortValue)
          case _ => None
        }
        """
          case "Char" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.charValue)
          case _ => None
        }
        """

        }

      }
    }
    case class DoubleTypeInfo(underlyingName: String) extends TypeInfoClass {
      lazy val underlyingType: Tree = tq"${Ident(TypeName(underlyingName))}"
      def defaultValue: Option[Tree] = Some(q"0f")
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JDouble(${toRender}) """
      override def toString(): String = underlyingName
      def buildExtractorFrom(varName: String, priv: Boolean): Tree = {
        underlyingName match {
          case "Float" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.floatValue)
          case JDouble(v) => Some(v.floatValue)
          case _ => None
        }
        """
          case "Double" =>
            q""" 
        ${TermName(varName)} match {
          case JInt(v) => Some(v.doubleValue)
          case JDouble(v) => Some(v.doubleValue)
          case _ => None
        }
        """
        }
      }
    }
    object BoolTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"false")
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q""" JBool(${toRender}) """
      def underlyingType: Tree = tq"Boolean"
      override def toString(): String = "Boolean"
      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        ${TermName(varName)} match {
          case JBool(v) => Some(v)
          case JInt(v) => Some(v.intValue != 0)
          case JDouble(v) => Some(v.intValue != 0)
          case _ => None
        }
        """
    }
    object StringTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q""" "" """)
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q""" JString(${toRender}) """
      def underlyingType: Tree = tq"String"

      override def toString(): String = "String"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        ${TermName(varName)} match {
          case JString(v) => Some(v)
          case _ => None
        }
        """
    }

    object URLTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = None
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JString(whatcheer.macros.support.RendererAndParser.urlToString(${toRender})) """
      def underlyingType: Tree = tq"java.net.URL"

      override def toString(): String = "URL"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        whatcheer.macros.support.RendererAndParser.jvalueToURL(${TermName(
          varName
        )})
        """
    }

    object UUIDTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"java.util.UUID.randomUUID()")
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JString(whatcheer.macros.support.RendererAndParser.uuidToString(${toRender})) """
      def underlyingType: Tree = tq"java.util.UUID"

      override def toString(): String = "UUID"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        whatcheer.macros.support.RendererAndParser.jvalueToUUID(${TermName(
          varName
        )})
        """
    }

    object DateTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"java.time.Instant.now()")
      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JString(whatcheer.macros.support.RendererAndParser.dateToString(${toRender})) """
      def underlyingType: Tree = tq"java.time.Instant"

      override def toString(): String = "Instant"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        whatcheer.macros.support.RendererAndParser.jvalueToDate(${TermName(
          varName
        )})
        """
    }
    case class OptionTypeInfo(sub: TypeInfo) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"(None : ${underlyingType})")

      override def buildRenderer(name: String, priv: Boolean): Tree =
        q"""${TermName(name)}.map(v => JField(${name}, ${renderFieldConstructor(
            Ident(TermName(name)),
            priv
          )})) """

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        sub.renderFieldConstructor(Ident(TermName("v")), priv)

      def underlyingType: Tree = tq"""Option[${sub.underlyingType}] """

      override def toString(): String = underlyingType.toString()

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" {

      val localInfo: Option[${sub.underlyingType}] = ${sub.buildExtractorFrom(
          varName,
          priv
        )};

        
       val theRet: Option[${underlyingType}] = localInfo match {
          case None => (None : Option[${underlyingType}])
          case v @ Some(_) => (Some(v) : Option[${underlyingType}])
        }

        theRet
      }
        """

      override def optional_? : Boolean = true
    }

    case class MapObjTypeInfo(sub: TypeInfo) extends TypeInfoClass {

      lazy val underlyingType: Tree =
        tq"""Map[String, ${sub.underlyingType}] """

      def defaultValue: Option[Tree] = Some(q"Map[String, ${underlyingType}]()")

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JObject(${toRender}.map{
          case (k, v) => JField(k,${sub.renderFieldConstructor(
            Ident(TermName("v")),
            priv
          )} )
        }.toList) """

      override def toString(): String = f"Map[String, ${sub.toString()}]"

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = {

        q""" 
        ${TermName(varName)} match {
          case JObject(fields) =>
             Some(Map(fields.flatMap{
              case JField(k, vv) => 
                ${sub.buildExtractorFrom(
            "vv",
            priv
          )}.map(got => k -> got)
              case _ => None
              } :_ *))
          case _ => None
        }
        """
      }
    }

    case class ListTypeInfo(sub: TypeInfo) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"Nil")

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        q"""JArray(${toRender}.map(v => ${sub.renderFieldConstructor(
            Ident(TermName("v")),
            priv
          )})) """

      def underlyingType: Tree = tq"""List[${sub.underlyingType}] """

      override def toString(): String = underlyingType.toString()

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" 
        (${TermName(varName)} match {
          case JArray(v) => Some(v.flatMap{vv => ${sub.buildExtractorFrom(
          "vv",
          priv
        )}})
          case _ => None
        })
        """
    }
    case class OtherTypeInfo(name: String, theType: Option[c.Type] = None)
        extends TypeInfoClass {
      def defaultValue: Option[Tree] = None

      def renderFieldConstructor(toRender: Tree, priv: Boolean): Tree =
        if (priv)
          q"""${toRender}.buildJson_includingPrivate() """
        else
          q"""${toRender}.buildJson() """

      def underlyingType: Tree = theType match {
        case Some(tt) => tq"${tt}"
        case None     => tq"${Ident(TypeName(name))}"
      }

      override def toString(): String = underlyingType.toString()

      def buildApplierFor(name: String): Tree = {
        def buildIt(in: List[TermName]): Tree = in match {
          case v :: Nil  => q"${v}"
          case v :: rest => q"(${buildIt(rest)}).${v}"
          case Nil       => q""
        }

        buildIt(
          name
            .split("\\.")
            .map(n => TermName(n))
            .toList
            .reverse
        )
      }

      def buildExtractorFrom(varName: String, priv: Boolean): Tree = q""" {
        
        (${buildApplierFor(name)}).${if (priv) {
          TermName("extractFromJson_includingPrivate")
        } else { TermName("extractFromJson") }}(${TermName(varName)})
        }
        """
    }

    object ExtractVarDef {
      def typeFor(t2: c.Type): TypeInfo = {
        val t = t2.dealias
        t.baseClasses.head.fullName match {
          case "scala.Int"         => IntTypeInfo("Int")
          case "scala.math.BigInt" => IntTypeInfo("BigInt")
          case "scala.Long"        => IntTypeInfo("Long")
          case "scala.Short"       => IntTypeInfo("Short")
          case "scala.Char"        => IntTypeInfo("Char")
          case "scala.Float"       => DoubleTypeInfo("Float")
          case "scala.Double"      => DoubleTypeInfo("Double")
          case "scala.Boolean"     => BoolTypeInfo
          case "java.lang.String"  => StringTypeInfo
          case "java.time.Instant" => DateTypeInfo
          case "java.net.URL"      => URLTypeInfo
          case "java.util.UUID"    => UUIDTypeInfo
          case "scala.Option"      => OptionTypeInfo(typeFor(t.typeArgs.head))
          case "scala.collection.immutable.List" =>
            ListTypeInfo(typeFor(t.typeArgs.head))

          case "scala.collection.immutable.Map" =>
            (
              t.typeArgs.head.baseClasses.head.fullName,
              t.typeArgs.drop(1).head.baseClasses.head.fullName
            ) match {
              case ("java.lang.String", "java.lang.String") =>
                MapStringTypeInfo
              case ("java.lang.String", "net.liftweb.json.JsonAST.JValue") =>
                MapValueTypeInfo
              case ("java.lang.String", "net.liftweb.json.JValue") =>
                MapValueTypeInfo
              case ("java.lang.String", zz) =>
                val secondType = typeFor(t.typeArgs.drop(1).head)

                secondType match {
                  case StringTypeInfo => MapStringTypeInfo
                  case v              => MapObjTypeInfo(v)
                }
              case (a, b) =>
                c.abort(c.enclosingPosition, f"Failed on type Map[${a}, ${b}]")
            }

          case other => OtherTypeInfo(other, Some(t))
        }
      }

      def unapply(in: Tree): Option[TermAndType] = {

        val ret = in match {
          case Typed(Ident(TermName(a)), Select(tree, ExtractName(name)))
              if name.endsWith("_$qmark") =>
            val theType = name.substring(0, name.length() - 7)
            val typeDecl = Select(
              tree,
              TypeName(theType)
            )

            Some(
              a -> AppliedTypeTree(
                Ident(TypeName("Option")),
                typeDecl :: Nil
              )
            )

          case Typed(Ident(TermName(a)), ExtractName(b))
              if b.endsWith("_$qmark") =>
            val theType = b.substring(0, b.length() - 7)
            val typeDecl = Ident(TypeName(theType))

            Some(
              a -> AppliedTypeTree(
                Ident(TypeName("Option")),
                typeDecl :: Nil
              )
            )

          case Typed(
                ExtractName(a),
                tn @ Select(_, ExtractName(theType))
              ) =>
            val typeDecl = tn

            Some(a -> typeDecl)

          case Typed(Ident(TermName(a)), tn @ ExtractName(theType)) =>
            val typeDecl = tn

            Some(a -> typeDecl)

          case Typed(
                ExtractName(a),
                tn @ AppliedTypeTree(az @ ExtractName(theType), b)
              ) =>
            val typeDecl: Tree = az

            Some(a -> typeDecl)
          case v @ Typed(a, b) =>
            None

          case _ => None
        }

        val realRet = ret.map { case (name, typeTree) =>
          identToTypeInfo(typeTree) match {
            case Some((_, theType)) =>
              TermAndType(name, typeFor(theType))
            case _ =>
              def computeTheTree(in: Tree): TypeInfo = {
                val ret =
                  in match {

                    case AppliedTypeTree(
                          Ident(TypeName("Option")),
                          other :: Nil
                        ) =>
                      OptionTypeInfo(computeTheTree((other)))

                    case AppliedTypeTree(
                          Ident(TypeName("List")),
                          rest :: Nil
                        ) =>
                      ListTypeInfo(computeTheTree(rest))

                    case AppliedTypeTree(
                          Ident(TypeName("Map")),
                          Ident(TypeName("String")) :: Ident(
                            TypeName("String")
                          ) :: Nil
                        ) =>
                      MapStringTypeInfo

                    case AppliedTypeTree(
                          Ident(TypeName("Map")),
                          Ident(TypeName("String")) :: Ident(
                            TypeName("JValue")
                          ) :: Nil
                        ) =>
                      MapValueTypeInfo

                    case AppliedTypeTree(
                          Ident(TypeName("Map")),
                          Ident(TypeName("String")) :: ExtractName(
                            secondParam
                          ) :: Nil
                        ) =>
                      MapObjTypeInfo(OtherTypeInfo(secondParam))

                    case ExtractName(typeName) =>
                      OtherTypeInfo(typeName)
                    case v =>
                      c.abort(
                        c.enclosingPosition,
                        f"Unable to expand the type ${typeTree} ${typeTree.getClass()}"
                      )
                  }

                ret
              }

              TermAndType(name, computeTheTree(typeTree))
          }

        }
        realRet
      }
    }

    object ParsedDef {
      def parse(in: BuildableInfo): (String, ParsedDef) = {
        val BuildableInfo(name, ValDef(_, _, _, kid), module) = in

        val kids = kid match {
          case Apply(_, what) => what
          case v =>
            c.abort(
              c.enclosingPosition,
              f"expecting `_fields_` to be a tuple of field defintions"
            )
        }

        val theInfo: List[DefInfo] = kids.map {
          case Apply(Ident(TermName("B")), tree) =>
            tree match {
              case Literal(Constant(url: String)) :: Nil => BaseURL(url)
              case Literal(Constant(v)) :: Nil =>
                c.abort(
                  c.enclosingPosition,
                  f"""Using `B` to define a base URL, you must pass a constant String: `B("https://foo.bar")` instead of ${v
                      .getClass()}"""
                )
              case stuff =>
                c.abort(
                  c.enclosingPosition,
                  f"""Using `B` to define a base URL, you must pass a constant String: `B("https://foo.bar")` as a single parameter, instead of ${stuff}"""
                )
            }
          case v @ ExtractVarDef(theType) =>
            FieldDef(theType.name, theType, theType.private_?)

          case Apply(Ident(TermName("E")), tree) =>
            tree match {
              case Ident(TermName(name)) :: Nil => ExtendDef(TypeName(name))
              case param =>
                c.abort(
                  c.enclosingPosition,
                  f"""Using `E` to define a trait that this case classe extends, use: `E(MyTrait)` instead of E(${param})"""
                )

            }

          case Apply(Ident(TermName("M")), tree) =>
            tree match {
              case Ident(TermName(name)) :: Nil => Merged(name)
              case param :: Nil =>
                c.abort(
                  c.enclosingPosition,
                  f"""Using `M` to define a class to merge into this one, you must pass the type identifer: `M(MyOtherClass)` instead of ${param}"""
                )
              case stuff =>
                c.abort(
                  c.enclosingPosition,
                  f"""Using `M` to define a class to merge into this one, you must pass the type identifer: `M(MyOtherClass)` instead of ${stuff}"""
                )
            }

          case Apply(
                Select(Ident(TermName("scala")), TermName("Tuple3")),
                tree
              ) =>
            tree match {
              case ExtractVarDef(theType) :: expr :: Literal(
                    Constant(url: String)
                  ) :: Nil =>
                FieldDef(
                  theType.name,
                  theType,
                  theType.private_?,
                  Some(expr),
                  Some(url)
                )
              case ExtractVarDef(theType) :: expr :: theUrl :: Nil =>
                c.abort(
                  c.enclosingPosition,
                  f"""When specifying a field, a default value, and a URL, the URL must be a literation String for example `(myVar: Int, 42, "https://someplace")` instead of ${theUrl}"""
                )
              case failed =>
                c.abort(
                  c.enclosingPosition,
                  f"""When specifying a field, a default value, and a URL, field must be of the form `fieldName: type`, for example `(myVar: Int, 42, "https://someplace")` instead of ${failed}"""
                )

            }

          case Apply(
                Select(Ident(TermName("scala")), TermName("Tuple2")),
                tree
              ) =>
            tree match {
              case ExtractVarDef(theType) :: expr :: Nil =>
                FieldDef(theType.name, theType, theType.private_?, Some(expr))

              case failed =>
                c.abort(
                  c.enclosingPosition,
                  f"""When specifying a field and a default value, field must be of the form `fieldName: type`, for example `(myVar: Int, 42)` instead of ${failed}"""
                )
            }

          case v =>
            c.abort(
              c.enclosingPosition,
              f"""can't parse ${v}
            The definitions must be in the form:
              * varName: type -> `foo: Int`
              * (varName: type, defaultValue) -> `(foo: Int, 42)`
              * (varName: type, defaultValue, urlConstant) -> `(foo: Int, 42, "https://json-ld-url")`
              * B(url) The base URL: `B("https://json-ld-url")`
              * E(Class) A trait this the implementation extends: `E(MyTrait)`
              * M(Class) the class to merge into this class: `M(MyOtherClass)`
             """
            )
        }

        (
          name,
          ParsedDef(
            name,
            theInfo.collect { case m: Merged => m },
            theInfo.collect { case b: BaseURL => b },
            theInfo.collect { case f: FieldDef => f },
            theInfo.collect { case e: ExtendDef => e },
            module
          )
        )

      }
    }

    def buildCaseClasses(defs: List[BuildableInfo]): List[Tree] = {
      val parsedDefs: Map[String, ParsedDef] = Map(
        defs.map(ParsedDef.parse(_)): _*
      )
      parsedDefs.values.flatMap(_.buildClasses(parsedDefs)).toList
    }

    def updateTemplate(in: Template): Template = {
      val Template(t1, vd, tree) = in

      val (fixedTree, toBuild) = findItemsToJsonifyAndFixTree(tree)

      val addedTreeItems: List[Tree] =
        q"""import net.liftweb.json._ """ :: buildCaseClasses(toBuild)

      Template(t1, vd, buildUnion(addedTreeItems ::: fixedTree))

    }

    annottees.map(_.tree).toList match {
      case ModuleDef(modifiers, names, template) :: Nil =>
        c.Expr[Any](
          ModuleDef(modifiers, names, updateTemplate(template))
        )
      case more =>
        c.abort(
          c.enclosingPosition, // c.universe.NoPosition,
          f"Can only apply macro to an `object`"
        )
    }

  }
}
