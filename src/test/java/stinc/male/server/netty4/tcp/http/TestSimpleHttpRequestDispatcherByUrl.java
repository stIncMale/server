package stinc.male.server.netty4.tcp.http;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static org.junit.Assert.assertEquals;
import stinc.male.server.netty4.RequestWithMetadata;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.HashMap;
import org.junit.Test;

public final class TestSimpleHttpRequestDispatcherByUrl {
  @Test
  public final void getProcessorName1() {
    final SimpleHttpRequestDispatcherByUrl dispatcher = new SimpleHttpRequestDispatcherByUrl(new HashMap<>(), "");
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_0, GET, "/");
    assertEquals("/", dispatcher.getProcessorName(new RequestWithMetadata<>(httpRequest)));
  }

  @Test
  public final void getProcessorName2() {
    final SimpleHttpRequestDispatcherByUrl dispatcher
        = new SimpleHttpRequestDispatcherByUrl(new HashMap<>(), "/context/path");
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_0, GET, "/context/path/req");
    assertEquals("/req", dispatcher.getProcessorName(new RequestWithMetadata<>(httpRequest)));
  }

  @Test
  public final void getProcessorName3() {
    final SimpleHttpRequestDispatcherByUrl dispatcher
        = new SimpleHttpRequestDispatcherByUrl(new HashMap<>(), "/context/path");
    final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_0, GET, "/req");
    assertEquals("/req", dispatcher.getProcessorName(new RequestWithMetadata<>(httpRequest)));
  }
}