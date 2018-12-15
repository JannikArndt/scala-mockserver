# Scala MockServer

Test your external dependencies by spinning up a server that mocks them!

### Example

```scala
"This test" should "start a server and check its response" in {

  val routes: Route = path("foo") {
    complete("bar")
  }

  calling(routes) { (host, port) =>
    val load = Source.fromURL(s"http://$host:$port/foo").mkString
    load shouldBe "bar"
  }

}
```

### Configuration

Use `MockServer.calling()` to start a server. You can pass your own `Route` (using `akka http`),
`host` and `port` (defaults to `127.0.0.1` and `2020`).

### Creating Routes

Routes can be created using the fantastic `akka http` API. 
See [their docs](https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html) for more info!