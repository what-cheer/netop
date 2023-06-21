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

object JsonifyMacro {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // convert an identity to a type... expand the type
    def identToTypeStr(id: Tree): String = {
      try { c.typecheck(id, mode = c.TYPEmode).tpe.toString() }
      catch { case _: Exception => id.toString() }
    }

    def identToTypeInfo(id: Tree): Option[c.Type] = {
      try { Some(c.typecheck(id, mode = c.TYPEmode).tpe) }
      catch {
        case e: Exception =>
          None
      }
    }

    // An extractor
    object FindAndFix {

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
    def findAndFix(t: Tree): (Tree, Option[(String, ValDef)]) = t match {
      case ModuleDef(
            modifiers,
            tn @ TermName(name),
            FindAndFix(fixedTemplate, fields)
          ) =>
        (ModuleDef(modifiers, tn, fixedTemplate), Some(name, fields))
      case v => (v, None)
    }

    def findItemsToJsonifyAndFixTree(
        tree: List[Tree]
    ): (List[Tree], List[(String, ValDef)]) = {
      val fixed = tree.map(t => findAndFix(t))
      (fixed.map(_._1), fixed.flatMap(_._2))
    }

    sealed trait DefInfo

    case class Merged(mergeWith: String) extends DefInfo

    case class BaseURL(url: String) extends DefInfo

    case class FieldDef(
        name: String,
        theType: TermAndType,
        defaultValue: Option[Tree] = None,
        url: Option[String] = None
    ) extends DefInfo {

      def getOrFindDefaultValue(): Option[Tree] =
        defaultValue.orElse {
          theType.mainType.defaultValue
        }

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
        v.getOrFindDefaultValue() match {
          case Some(defValue) =>
            q"""val ${TermName(v.name)}: ${v.theType.tpe} = ${defValue} """
          case None => q"""var ${TermName(v.name)}: ${v.theType.tpe}  """
        }
      }

      // val extendIt = extendWhat.map(v => TypeName(v))
      val jsonOut: List[Tree] = fields.map(_.theType.buildJsonRenderer())
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
      }"""

      // println(ret)

      ret
    }

    case class ParsedDef(
        name: String,
        merged: List[Merged],
        baseUrls: List[BaseURL],
        fields: List[FieldDef],
        extended: List[ExtendDef]
    ) {
      def traitName: String = f"${name}Trait"
      def buildClasses(info: Map[String, ParsedDef]): List[Tree] = {
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

        val allFields = (fields :: toMerge.map(_.fields)).flatten
        val allBaseUrls = Set(
          (baseUrls :: toMerge.map(_.baseUrls)).flatten: _*
        ).toList
        val allExtended = Set(
          (extended :: toMerge.map(_.extended) ::: toMerge.map(m =>
            List(ExtendDef(TypeName(m.traitName)))
          )).flatten: _*
        ).toList
        buildTheTrait(traitName, allFields) :: buildTheClass(
          name,
          allFields,
          allExtended
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

    case class TermAndType(name: String, mainType: TypeInfo) {
      // lazy val mainType: TypeInfo = typeFor(tpe)

      lazy val tpe: Tree = mainType.underlyingType
      def buildJsonRenderer(): Tree = mainType.buildRenderer(name)
    }

    sealed trait TypeInfo {
      def defaultValue: Option[Tree]
      def buildRenderer(name: String): Tree
      def renderFieldConstructor(toRender: Tree): Tree
      def underlyingType: Tree
    }

    sealed abstract class TypeInfoClass extends TypeInfo {
      def buildRenderer(name: String): Tree =
        q"""Some(JField(${name}, ${renderFieldConstructor(
            Ident(TermName(name))
          )})) """
    }
    case class IntTypeInfo(underlyingType: Tree) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"0")

      def renderFieldConstructor(toRender: Tree): Tree =
        q"""JInt(${toRender}) """
    }
    case class DoubleTypeInfo(underlyingType: Tree) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"0f")
      def renderFieldConstructor(toRender: Tree): Tree =
        q"""JDouble(${toRender}) """
    }
    object BoolTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"false")
      def renderFieldConstructor(toRender: Tree): Tree =
        q""" JBool(${toRender}) """
      def underlyingType: Tree = tq"Boolean"
    }
    object StringTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q""" "" """)
      def renderFieldConstructor(toRender: Tree): Tree =
        q""" JString(${toRender}) """
      def underlyingType: Tree = tq"String"
    }
    object DateTypeInfo extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"new java.util.Date()")
      def renderFieldConstructor(toRender: Tree): Tree =
        q"""JString(whatcheer.macros.support.RendererAndParser.dateToString(${toRender})) """
      def underlyingType: Tree = tq"java.util.Date"
    }
    case class OptionTypeInfo(sub: TypeInfo) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"None")

      override def buildRenderer(name: String): Tree =
        q"""${TermName(name)}.map(v => JField(${name}, ${renderFieldConstructor(
            Ident(TermName(name))
          )})) """
      def renderFieldConstructor(toRender: Tree): Tree =
        sub.renderFieldConstructor(Ident(TermName("v")))

      def underlyingType: Tree = tq"""Option[${sub.underlyingType}] """
    }
    case class ListTypeInfo(sub: TypeInfo) extends TypeInfoClass {
      def defaultValue: Option[Tree] = Some(q"Nil")

      def renderFieldConstructor(toRender: Tree): Tree =
        q"""JArray(${toRender}.map(v => ${sub.renderFieldConstructor(
            Ident(TermName("v"))
          )})) """

      def underlyingType: Tree = tq"""List[${sub.underlyingType}] """
    }
    case class OtherTypeInfo(name: String, theType: Option[c.Type] = None)
        extends TypeInfoClass {
      def defaultValue: Option[Tree] = None

      def renderFieldConstructor(toRender: Tree): Tree =
        q"""${toRender}.buildJson() """

      def underlyingType: Tree = theType match {
        case Some(tt) => tq"${tt}"
        case None     => tq"${Ident(TypeName(name))}"
      }
    }

    object ExtractVarDef {
      def typeFor(t: c.Type): TypeInfo = t.baseClasses.head.fullName match {
        case "scala.Int" => IntTypeInfo(tq"${Ident(TypeName("Int"))}")
        case "scala.math.BigInt" => IntTypeInfo(tq"${Ident(TypeName("BigInt"))}")
        case "scala.Long"    => IntTypeInfo(tq"${Ident(TypeName("Long"))}")
        case "scala.Short"   => IntTypeInfo(tq"${Ident(TypeName("Short"))}")
        case "scala.Char"    => IntTypeInfo(tq"${Ident(TypeName("Char"))}")
        case "scala.Float"   => DoubleTypeInfo(tq"${Ident(TypeName("Float"))}")
        case "scala.Double"  => DoubleTypeInfo(tq"${Ident(TypeName("Double"))}")
        case "scala.Boolean" => BoolTypeInfo
        case "java.lang.String" => StringTypeInfo
        case "java.util.Date"   => DateTypeInfo
        case "scala.Option"     => OptionTypeInfo(typeFor(t.typeArgs.head))
        case "scala.collection.immutable.List" =>
          ListTypeInfo(typeFor(t.typeArgs.head))

        case other => OtherTypeInfo(other, Some(t))
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

        ret.map { case (name, typeTree) =>
          identToTypeInfo(typeTree) match {
            case Some(theType) => TermAndType(name, typeFor(theType))
            case _ =>
              typeTree match {

                case AppliedTypeTree(
                      Ident(TypeName("Option")),
                      AppliedTypeTree(
                        Ident(TypeName("List")),
                        ExtractName(typeName) :: Nil
                      ) :: Nil
                    ) =>
                  TermAndType(
                    name,
                    OptionTypeInfo(ListTypeInfo(OtherTypeInfo(typeName)))
                  )

                case AppliedTypeTree(
                      Ident(TypeName("Option")),
                      ExtractName(typeName) :: Nil
                    ) =>
                  TermAndType(name, OptionTypeInfo(OtherTypeInfo(typeName)))

                case AppliedTypeTree(
                      Ident(TypeName("List")),
                      ExtractName(typeName) :: Nil
                    ) =>
                  TermAndType(name, ListTypeInfo(OtherTypeInfo(typeName)))

                case ExtractName(typeName) =>
                  TermAndType(name, OtherTypeInfo(typeName))
                case v =>
                  c.abort(
                    c.enclosingPosition,
                    f"Unable to expand the type ${typeTree} ${typeTree.getClass()}"
                  )
              }
          }
        }
      }
    }

    object ParsedDef {
      def parse(in: (String, ValDef)): (String, ParsedDef) = {
        val (name, ValDef(_, _, _, kid)) = in

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
          case ExtractVarDef(theType) => FieldDef(theType.name, theType)

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
                FieldDef(theType.name, theType, Some(expr), Some(url))
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
                FieldDef(theType.name, theType, Some(expr))

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
            theInfo.collect { case e: ExtendDef => e }
          )
        )

      }
    }

    def buildCaseClasses(defs: List[(String, ValDef)]): List[Tree] = {
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

      Template(t1, vd, addedTreeItems ::: fixedTree)
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
