package stincmale.server.reqres.spring.test;

import stincmale.server.reqres.Processor;
import stincmale.server.reqres.RequestProcessor;
import org.springframework.stereotype.Component;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Processor("test request processor")
@Component
@ThreadSafe
final class TestSpringRequestDispatcher_RequestProcessor implements RequestProcessor<Object, Object> {
  private TestSpringRequestDispatcher_RequestProcessor() {
  }

  @Override
  public final CompletionStage<Object> process(final Object request) {
    return CompletableFuture.completedFuture(request);
  }
}
