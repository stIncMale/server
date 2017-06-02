package stinc.male.server.util.logging;

import java.util.concurrent.ForkJoinPool;
import org.junit.Test;
import org.slf4j.MDC;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;

public final class TransferableMdcTest {

  public TransferableMdcTest() {
  }

  @Test
  public final void idiom() {
    final String keyTransferred = "kTransferred";
    final String key = "k";
    final String valueTransferred = "vTransferred";
    MDC.put(keyTransferred, valueTransferred);
    final TransferableMdc mdc = TransferableMdc.current();
    ForkJoinPool.commonPool().submit(() -> {
      final String value = "v";
      MDC.put(key, value);
      try (@SuppressWarnings("unused") final TransferableMdc mdcTmp = mdc.apply()) {
        assertSame(valueTransferred, MDC.get(keyTransferred));
        assertSame(value, MDC.get(key));
      }
      assertNull(MDC.get(keyTransferred));
      assertSame(value, MDC.get(key));
      return null;
    }).join();
    assertSame(valueTransferred, MDC.get(keyTransferred));
    assertNull(MDC.get(key));
  }
}