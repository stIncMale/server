module stinc.male.server {
  requires transitive jsr305;
  requires slf4j.api;
  requires com.google.common;
  requires transitive netty.all;
  requires transitive java.dogstatsd.client;
  requires transitive spring.context;
  requires org.apache.commons.lang3;
  exports stinc.male.server;
  exports stinc.male.server.netty4;
  exports stinc.male.server.netty4.tcp;
  exports stinc.male.server.netty4.tcp.http;
  exports stinc.male.server.reqres;
  exports stinc.male.server.reqres.spring;
  exports stinc.male.server.reqres.spring.http;
}