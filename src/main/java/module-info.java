import stinc.male.server.netty4.tcp.MonoHandler;
import stinc.male.server.netty4.tcp.http.HttpRequestProcessor;
import stinc.male.server.netty4.tcp.http.SimpleHttpRequestDispatcherByUrl;
import stinc.male.server.reqres.Processor;
import stinc.male.server.reqres.RequestProcessor;
import stinc.male.server.reqres.spring.SpringRequestDispatcher;

/**
 * A server framework based on <a href="https://netty.io/">Netty</a>.
 * <p>
 * Main features:
 * <ul>
 *   <li>{@linkplain SpringRequestDispatcher auto-detecting} {@linkplain Processor annotated} {@linkplain RequestProcessor request processors}
 *   via <a href="https://spring.io/projects/spring-framework">Spring Framework</a></li>
 *   <li>{@linkplain SimpleHttpRequestDispatcherByUrl auto-assigning} {@linkplain Processor annotated}
 *   {@linkplain HttpRequestProcessor HTTP request processors} to URIs;</li>
 *   <li>{@linkplain RequestProcessor#process(Object) asynchronous processing} of requests
 *   with {@linkplain MonoHandler responses ordered the same as requests}.</li>
 * </ul>
 */
module stinc.male.server {
  requires org.slf4j;
  requires com.google.common;
  requires org.apache.commons.lang3;
  requires transitive jsr305;
  requires transitive io.netty.all;
  requires transitive java.dogstatsd.client;
  requires transitive spring.context;
  exports stinc.male.server;
  exports stinc.male.server.netty4;
  exports stinc.male.server.netty4.tcp;
  exports stinc.male.server.netty4.tcp.http;
  exports stinc.male.server.reqres;
  exports stinc.male.server.reqres.spring;
  exports stinc.male.server.reqres.spring.http;
}