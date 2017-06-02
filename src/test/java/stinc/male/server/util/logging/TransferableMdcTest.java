package stinc.male.server.util.logging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.MDC;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;

public final class TransferableMdcTest {
  private static volatile ExecutorService executor;

  static {
    executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> {});//this task forces the executor to pre-initialize a thread
  }

  @AfterClass
  public static final void afterClass() {
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
      try (final TransferableMdc mdcTmp = mdc.apply()) {
        assertSame(valueTransferred, MDC.get(keyTransferred));
        assertSame(value, MDC.get(key));
      }
      assertNull(MDC.get(keyTransferred));
      assertSame(value, MDC.get(key));
    }).get();
    assertSame(valueTransferred, MDC.get(keyTransferred));
    assertNull(MDC.get(key));
  }
}