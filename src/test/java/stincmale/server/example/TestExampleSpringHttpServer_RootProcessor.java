package stincmale.server.example;

import stincmale.server.netty4.RequestWithMetadata;
import stincmale.server.netty4.tcp.http.HttpRequestProcessor;
import stincmale.server.netty4.tcp.http.util.HttpUtil;
import stincmale.server.reqres.Processor;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.springframework.stereotype.Component;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Processor("/")
@Component
@ThreadSafe
final class TestExampleSpringHttpServer_RootProcessor implements HttpRequestProcessor {
  static final String RESPONSE = "Move along, nothing to see here.";

  private TestExampleSpringHttpServer_RootProcessor() {
  }

  @Override
  public final CompletionStage<FullHttpResponse> process(final RequestWithMetadata<? extends FullHttpRequest> request) {
    //synchronous processing, see TestExampleSpringHttpServer_HelloWorldProcessor for an example of asynchronous processing
    return CompletableFuture.completedFuture(
        HttpUtil.setPlainTextUtf8Content(HttpUtil.createHttpResponse(HTTP_1_1, OK), RESPONSE));
  }
}
