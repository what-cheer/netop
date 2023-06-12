package bootstrap.liftweb

import net.liftweb._
import common.Full
import http.Html5Properties
import util._
import Helpers._
import common._
import http._
import mapper._
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.classic.joran.JoranConfigurator
import java.io.{ByteArrayInputStream, InputStream}
import whatcheer.routes.MainRoutes
import whatcheer.models.Actor
import whatcheer.models.Relationship
import whatcheer.models.TheObject
import whatcheer.models.Activity
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.util.Properties
import java.io.FileInputStream

class Boot {
  def booted_? = Boot.hasBooted_?.get()
  def boot: Unit = Boot.boot
}

/** A class that's instantiated early and run. It allows the application to
  * modify lift's environment
  */
object Boot {
  private val hasBooted_? = new AtomicBoolean(false)
  def booted_? = hasBooted_?.get()
  def boot: Unit = {

    import java.util.{Map => JMap}

    if (booted_?) return

    // Add more property lookups for running in UberJar mode
    System.getenv("WHATCHEER_PROPS_FILE") match {
      case v if v != null =>
        val fis = new FileInputStream(System.getenv("WHATCHEER_PROPS_FILE"))
        val ret = new Properties

        ret.load(fis)
        fis.close()

        val otherProps = Map(ret.entrySet.toArray.flatMap {
          case s: JMap.Entry[_, _] =>
            List((s.getKey.toString, s.getValue.toString))
          case _ => Nil
        }.toIndexedSeq: _*)

        Props.prependProvider(otherProps)
      case _ =>
    }

    val xml =
      """
        |<configuration>
        |  <appender name="STDOUT" class="whatcheer.utils.PerThreadLogger">
        |    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        |      <pattern>%d{ISO8601,UTC} [%thread] %-5level %logger{36} %X{file_name} - %msg%n</pattern>
        |    </encoder>
        |  </appender>
        |
        |  <root level="info">
        |    <appender-ref ref="STDOUT" />
        |  </root>
        |</configuration>
      """.stripMargin

    Logger.setup = Full(() => {
      val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      val configurator = new JoranConfigurator()
      configurator.setContext(lc)
      // the context was probably already configured by default configuration rules
      lc.reset()
      val is: InputStream = new ByteArrayInputStream(xml.getBytes)
      configurator.doConfigure(is)
    })

    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(
          Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr
            "jdbc:h2:./lift_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"),
          Props.get("db.password")
        )

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(mapper.DefaultConnectionIdentifier, vendor)
    }

    Schemifier.schemify(
      true,
      Schemifier.infoF _,
      Actor,
      Relationship,
      TheObject,
      Activity
    )

    LiftRules.supplementalHeaders.default.set(() => Nil)
    // don't polute the logs
    LiftRules.contentSecurityPolicyViolationReport = { violation =>
      Empty
    }

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      Html5Properties(r.userAgent)
    )

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper())

    LiftRules.statelessDispatch.append(MainRoutes)

    hasBooted_?.set(true)

  }
}
