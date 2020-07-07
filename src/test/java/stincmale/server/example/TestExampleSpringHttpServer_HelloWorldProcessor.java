package stincmale.server.example;

import stincmale.server.netty4.tcp.http.HttpRequestProcessor;
import stincmale.server.netty4.tcp.http.util.HttpUtil;
import stincmale.server.reqres.Processor;
import stincmale.server.netty4.RequestWithMetadata;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.springframework.stereotype.Component;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Processor("/helloWorld")
@Component
@ThreadSafe
final class TestExampleSpringHttpServer_HelloWorldProcessor implements HttpRequestProcessor {
  static final String RESPONSE = "Hello World!";

  private TestExampleSpringHttpServer_HelloWorldProcessor() {
  }

  @Override
  public final CompletionStage<FullHttpResponse> process(final RequestWithMetadata<? extends FullHttpRequest> request) {
    //asynchronous processing, see TestExampleSpringHttpServer_RootProcessor for an example of synchronous processing
    return CompletableFuture.supplyAsync(() -> {//async computation
      return HttpUtil.setPlainTextUtf8Content(HttpUtil.createHttpResponse(HTTP_1_1, OK), RESPONSE);
    });
  }
}
