package stinc.male.server.netty4.tcp.http;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import stinc.male.server.reqres.RequestDispatcher;
import stinc.male.server.netty4.RequestWithMetadata;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class TestHttpDispatchMonoHandler {
  private static final class Dispatcher implements RequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
    private Dispatcher() {
    }

    @Override
    public final CompletionStage<FullHttpResponse> process(RequestWithMetadata<? extends FullHttpRequest> request) {
      return CompletableFuture.completedFuture(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK));
    }
  }

  public TestHttpDispatchMonoHandler() {
  }

  @Test
  public final void keepAlive1() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new HttpDispatchMonoHandler(new Dispatcher(), -1));
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_0, GET, "/");
    HttpHeaders.setKeepAlive(httpRequest, true);
    testChannel.writeInbound(new RequestWithMetadata<>(httpRequest));
    final FullHttpResponse response = (FullHttpResponse) testChannel.readOutbound();
    assertNotNull(response);
    assertTrue(HttpHeaders.isKeepAlive(response));
  }

  @Test
  public final void keepAlive2() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new HttpDispatchMonoHandler(new Dispatcher(), -1));
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
    testChannel.writeInbound(new RequestWithMetadata<>(httpRequest));
    final FullHttpResponse response = (FullHttpResponse) testChannel.readOutbound();
    assertNotNull(response);
    assertTrue(HttpHeaders.isKeepAlive(response));
  }

  @Test
  public final void noKeepAlive1() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new HttpDispatchMonoHandler(new Dispatcher(), -1));
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_0, GET, "/");
    testChannel.writeInbound(new RequestWithMetadata<>(httpRequest));
    final FullHttpResponse response = (FullHttpResponse) testChannel.readOutbound();
    assertNotNull(response);
    assertFalse(HttpHeaders.isKeepAlive(response));
  }

  @Test
  public final void noKeepAlive2() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new HttpDispatchMonoHandler(new Dispatcher(), -1));
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
    HttpHeaders.setKeepAlive(httpRequest, false);
    testChannel.writeInbound(new RequestWithMetadata<>(httpRequest));
    final FullHttpResponse response = (FullHttpResponse) testChannel.readOutbound();
    assertNotNull(response);
    assertFalse(HttpHeaders.isKeepAlive(response));
  }
}