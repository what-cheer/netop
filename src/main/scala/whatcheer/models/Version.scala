package whatcheer.models

case class Version(id: String) {

}

object Version {
    def unapply(str: String): Option[Version] = Some(Version(str))
}