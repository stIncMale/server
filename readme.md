# server
<p align="right">
<a href="https://docs.oracle.com/en/java/javase/14/"><img src="https://img.shields.io/badge/Java-14-blue.svg" alt="Java requirement"></a>
<a href="https://www.kovalenko.link/server/apidocs/"><img src="https://img.shields.io/badge/API_documentation-current-blue.svg" alt="API documentation"></a>
</p>

## About
A server framework based on [Netty](https://netty.io/).

### Main features
* [Auto-detecting](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/reqres/spring/SpringRequestDispatcher.html)
  [annotated](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/reqres/Processor.html)
  [request processors](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/reqres/RequestProcessor.html)
  via [Spring Framework](https://spring.io/projects/spring-framework).
* [Auto-assigning](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/netty4/tcp/http/SimpleHttpRequestDispatcherByUrl.html)
  [annotated](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/reqres/Processor.html)
  [HTTP request processors](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/netty4/tcp/http/HttpRequestProcessor.html) to URIs.
* [Asynchronous processing](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/reqres/RequestProcessor.html#process(RQ))
  of requests
  with [responses ordered the same as requests](https://www.kovalenko.link/server/apidocs/stinc.male.server/stinc/male/server/netty4/tcp/MonoHandler.html).

## [Building/contributing](https://github.com/stIncMale/server/blob/master/contributing.md)

##Example
[A simple HTTP server](https://github.com/stIncMale/server/tree/master/src/test/java/stinc/male/server/example)

---

Licensed under [WTFPL](http://www.wtfpl.net/), except where another license is explicitly specified.
