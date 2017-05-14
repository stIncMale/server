package stinc.male.server.reqres.spring;

import static org.junit.Assert.assertSame;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
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