# What Cheer Netop

What Cheer Netop is an MIT licensed implementation of [Fediverse](https://en.wikipedia.org/wiki/Fediverse)/[ActivityPub](https://en.wikipedia.org/wiki/ActivityPub)
in [Scala](https://www.scala-lang.org/) and [Lift](https://liftweb.net) by
[David Pollak](https://macaw.social/@dpp).


> In a canoe with several others, Roger scouted the area across the Seekonk River. They spotted a group of Narragansett on a large rock, known afterwards as Slate Rock, along the western shore of the Seekonk River. As they approached the Narragansett greeted them by calling out: “What Cheer Netop!” This greeting is a combination of English and Narragansett languages. ‘What cheer’ was an informal common English greeting of the day, short for ‘what cheery news do you bring’ and today’s equivalent of “what’s up?’’ “Netop” is the Narragansett word for friend.
>    
> [Roger Williams: Founding Providence](https://www.nps.gov/rowi/learn/historyculture/foundingprovidence.htm)

"What Cheer, Netop" also translates to "Hi, Neighbor!" This is a perfect embodiment of the Fediverse. Each federated instance is a neighbor (netop)
to other Fediverse instances. They share a common language and say "Hi" and have lots of conversations with each other.


The implementation is based on [ActivityPub-Express](https://github.com/immers-space/activitypub-express) which is also MIT licensed and
[Wildebeest](https://github.com/cloudflare/wildebeest) which is Apache 2.0 licensed.


## Why?

A JVM-based ActivityPub server has performance and deployment advantages.

Scala and Lift are well known to David Pollak

An MIT licensed ActivityPub server has value in terms of commercial extensions
that most of the AGPL implementations don't lend themselves to.

Yes, you may disagree with some or all of the above reasons. This is
the Internet.

## Discussion and Contributions

Right now, all discussion is via GitHub tickets.

If you want to contribute, open a ticket, have a discussion, open
a PR, etc.

## Running

Netop is a basic [LiftWeb](https://liftweb.net) application. It may be run in an app server or as a stand-alone
UberJar.

If running as a stand-alone UberJar specify the location of the properties file, set the `WHATCHEER_PROPS_FILE` environment
variabel to point to the properties file.

Properties:

* `whatcheer.name`: The domain name of the server. default: `localhost`
* `whatcheer.baseurl`: The base URL of the server. default: `http://localhost:8080`
* `whatcheer.objectbase`: the base for "object" related requests. For example `/o/:id` default: `o`
* `whatcheer.streambase`: the base for "stream" related requests. For example `/s/:id` default: `s`
* `whatcheer.actorbase`: the base for "actor" related requests. For example `/u/:actor` default: `u`
* `whatcheer.actorbase.inbox`: the base for the actor's inbox. For example `/u/:actor/inbox` default `inbox`
* `whatcheer.actorbase.outbox`: the base for the actor's outbox. For example `/u/:actor/outbox` default `outbox`
* `whatcheer.actorbase.following`: the base for the actor's following list. For example `/u/:actor/following` default `following`
* `whatcheer.actorbase.followers`: the base for the actor's followers list. For example `/u/:actor/followers` default `followers`
* `whatcheer.actorbase.liked`: the base for the actor's liked list. For example `/u/:actor/liked` default `liked`
* `whatcheer.endpoint.nodeinfo`: the endpoint for node info related request. default: `nodeinfo`
* `whatcheer.endpoint.upload`: the endpoint for uploading stuff. default: `upload`
* `whatcheer.endpoint.oauth`: the endpoint for OAuth authorization. default: `authorize`
* `whatcheer.endpoint.proxy`: the endpoint for proxy requests. default: `proxy`
* `whatcheer.name`: the name of this server implementation. default: `whatcheer`
* `whatcheer.version`: the version of this server implementation. default: `0.1`
* `whatcheer.openregistrations`: is the server open to new registrations? default: `false`