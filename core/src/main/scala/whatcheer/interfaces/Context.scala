package whatcheer.interfaces

case class Identity(
	email: String
)

case class ContextData(
	// ActivityPub Person object of the logged in user
	connectedActor: Actor,

	// Object returned by access provider
	identity: Identity,

	// Client or app identifier
	clientId: String
)

