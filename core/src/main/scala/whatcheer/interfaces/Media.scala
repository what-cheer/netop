package whatcheer.interfaces

object Media extends Enumeration {
  type MediaType = Value

  val Unknown = Value(1, "unknown")
  val Image = Value(2, "image")
  val GIFV = Value(3, "gifv")
  val Video = Value(4, "video")
    val Audio = Value(5, "audio")
}



case class MediaAttachment (
	id: String,
	`type`: Media.MediaType,
	url: Base.URL,
	preview_url: Base.URL,
	meta: Option[String],
	description: String,
	blurhash: String
)

