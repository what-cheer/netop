package whatcheer.interfaces

case class Tag(
    name: String,
    url: Base.URL,
    history: List[Any],
    following: Option[Boolean]
)
