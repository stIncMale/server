package stinc.male.server.reqres.spring;

import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestSpringRequestDispatcher.SpringConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public final class TestSpringRequestDispatcher {
  @Configuration
  @ComponentScan(basePackages = {"stinc.male.server.reqres.spring"})
  static class SpringConfig {
    SpringConfig() {
    }
  }

  @Inject
  SpringRequestDispatcher<Object, Object> requestDispatcher;

  @Test
  public final void test() throws Exception {
    final Object request = new Object();
    final Object response = requestDispatcher.process(request)
        .toCompletableFuture()
        .get();
    assertSame(request, response);
  }
}