package stincmale.server.util.logging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A fully functional implementation of SLF4J is required for these tests to succeed.
 */
public final class TransferableMdcTest {
  private static final ExecutorService executor;

  static {
    executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> {});//this task forces the executor to pre-initialize a thread
  }

  @AfterAll
  public static final void afterAll() {
    executor.shutdownNow();
  }

  public TransferableMdcTest() {
  }

  @Test
  public final void idiom() throws Exception {
    final String keyTransferred = "kTransferred";
    final String key = "k";
    final String valueTransferred = "vTransferred";
    MDC.put(keyTransferred, valueTransferred);
    final TransferableMdc mdc = TransferableMdc.current();
    executor.submit(() -> {
      final String value = "v";
      MDC.put(key, value);
      try (final TransferableMdc ignored = mdc.apply()) {
        assertSame(valueTransferred, MDC.get(keyTransferred));
        assertSame(value, MDC.get(key));
      }
      assertNull(MDC.get(keyTransferred));
      assertSame(value, MDC.get(key));
    })
        .get();
    assertSame(valueTransferred, MDC.get(keyTransferred));
    assertNull(MDC.get(key));
  }
}
