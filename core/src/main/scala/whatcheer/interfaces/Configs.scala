package whatcheer.interfaces

// https://docs.joinmastodon.org/entities/Instance/
case class InstanceConfig(
    uri: String,
    title: String,
    thumbnail: String,
    languages: List[String],
    email: String,
    description: String,
    short_description: Option[String],
    rules: List[Rule]
)

case class Thumbnail(url: String)
case class Registration(enabled: Boolean)
case class Contact(email: String)
case class InstanceConfigV2(
    domain: String,
    title: String,
    version: String,
    source_url: String,
    description: String,
    thumbnail: Thumbnail,
    languages: List[String],
    registrations: Registration,
    contact: Contact,
    rules: List[Rule]
)

// https://docs.joinmastodon.org/entities/Rule/
case class Rule(
    id: String,
    text: String
)

case class DefaultImages(
    avatar: String,
    header: String
)
