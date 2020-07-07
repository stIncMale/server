package stinc.male.server.reqres.spring.test;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import stinc.male.server.reqres.spring.SpringRequestDispatcher;

@Component
@ThreadSafe
final class TestSpringRequestDispatcher_TestRequestDispatcher extends SpringRequestDispatcher<Object, Object> {
  @Inject
  private TestSpringRequestDispatcher_TestRequestDispatcher(final ApplicationContext appCtx) {
    super(appCtx, null, true, null);
  }

  @Override
  protected final String getProcessorName(final Object request) {
    return "test request processor";
  }
}