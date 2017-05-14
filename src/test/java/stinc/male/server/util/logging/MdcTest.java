package stinc.male.server.util.logging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.MDC;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;

public class MdcTest {
  private static volatile ExecutorService executor;

  public MdcTest() {
  }

  @BeforeClass
  public static final void beforeClass() {
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterClass
  public static final void afterClass() {
    executor.shutdownNow();
  }

  @Test
  public final void idiom() {
    final String keyTransferred = "kTransferred";
    final String valueTransferred = "vTransferred";
    MDC.put(keyTransferred, valueTransferred);
    final Mdc mdc = Mdc.current();
    executor.submit(() -> {
      final String key = "k";
      final String value = "v";
      MDC.put(key, value);
      try (@SuppressWarnings("unused") Mdc mdcTmp = mdc.apply()) {
        assertSame(valueTransferred, MDC.get(keyTransferred));
        assertSame(value, MDC.get(key));
      }
      assertNull(MDC.get(keyTransferred));
      assertSame(value, MDC.get(key));
      return null;
    });
  }
}