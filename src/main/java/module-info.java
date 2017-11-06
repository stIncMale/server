module stinc.male.server {
  requires transitive jsr305;
  requires slf4j.api;
  requires com.google.common;
  requires netty.all;
  requires java.dogstatsd.client;
  requires spring.context;
  requires org.apache.commons.lang3;
// TODO in case of exporting javac takes 7GB of RAM and fails with OOM (requires transitive may be the cause)
//  exports stinc.male.server;
//  exports stinc.male.server.netty4;
//  exports stinc.male.server.netty4.tcp;
//  exports stinc.male.server.netty4.tcp.http;
//  exports stinc.male.server.reqres;
//  exports stinc.male.server.reqres.spring;
//  exports stinc.male.server.reqres.spring.http;
}