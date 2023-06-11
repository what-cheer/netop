package whatcheer.logic

import net.liftweb.common._

object Target {
    def validate(_foo: String): Box[Target] = {
        Empty
    }
}

case class Target() {}