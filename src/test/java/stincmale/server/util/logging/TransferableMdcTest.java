package stincmale.server.util.logging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public final class TransferableMdcTest {
  private static final String KEY_OUTER = "KEY_OUTER";
  private static final String VALUE_OUTER = "VALUE_OUTER";
  private static final String KEY_SHARED = "KEY_SHARED";
  private static final String VALUE_SHARED_OUTER = "VALUE_SHARED_OUTER";
  private static final String VALUE_SHARED_INNER = "VALUE_SHARED_INNER";
  private static final String KEY_INNER = "KEY_INNER";
  private static final String VALUE_INNER = "VALUE_INNER";

  private ExecutorService executor;

  public TransferableMdcTest() {
  }

  @BeforeEach
  public final void before() {
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  public final void after() {
    executor.shutdownNow();
  }

  @Test
  public final void apply() throws ExecutionException, InterruptedException {
    {//prepare outer MDC
      MDC.clear();
      MDC.put(KEY_OUTER, VALUE_OUTER);
      MDC.put(KEY_SHARED, VALUE_SHARED_OUTER);
    }
    final TransferableMdc outerMdc = TransferableMdc.current();
    assertSame(VALUE_OUTER, MDC.get(KEY_OUTER));
    assertSame(VALUE_SHARED_OUTER, MDC.get(KEY_SHARED));
    assertNull(MDC.get(KEY_INNER));
    executor.submit(() -> {
      {//prepare inner MDC
        MDC.clear();
        MDC.put(KEY_SHARED, VALUE_SHARED_INNER);
        MDC.put(KEY_INNER, VALUE_INNER);
      }
      try (var transferredMdc = outerMdc.transfer()) {
        assertSame(VALUE_OUTER, MDC.get(KEY_OUTER));
        assertSame(VALUE_SHARED_OUTER, MDC.get(KEY_SHARED));
        assertNull(MDC.get(KEY_INNER));
      }
      assertNull(MDC.get(KEY_OUTER));
      assertSame(VALUE_SHARED_INNER, MDC.get(KEY_SHARED));
      assertSame(VALUE_INNER, MDC.get(KEY_INNER));
    }).get();
  }

  @Test
  public final void applyEmpty() throws ExecutionException, InterruptedException {
    {//prepare outer MDC
      MDC.clear();
    }
    final TransferableMdc outerMdc = TransferableMdc.current();
    executor.submit(() -> {
      {//prepare inner MDC
        MDC.clear();
        MDC.put(KEY_SHARED, VALUE_SHARED_INNER);
        MDC.put(KEY_INNER, VALUE_INNER);
      }
      try (var transferredMdc = outerMdc.transfer()) {
        assertNull(MDC.get(KEY_OUTER));
        assertNull(MDC.get(KEY_SHARED));
        assertNull(MDC.get(KEY_INNER));
      }
      assertNull(MDC.get(KEY_OUTER));
      assertSame(VALUE_SHARED_INNER, MDC.get(KEY_SHARED));
      assertSame(VALUE_INNER, MDC.get(KEY_INNER));
    }).get();
  }

  @Test
  public final void restoreEmpty() throws ExecutionException, InterruptedException {
    {//prepare outer MDC
      MDC.clear();
      MDC.put(KEY_OUTER, VALUE_OUTER);
      MDC.put(KEY_SHARED, VALUE_SHARED_OUTER);
    }
    final TransferableMdc outerMdc = TransferableMdc.current();
    executor.submit(() -> {
      {//prepare inner MDC
        MDC.clear();
      }
      try (var transferredMdc = outerMdc.transfer()) {
        assertSame(VALUE_OUTER, MDC.get(KEY_OUTER));
        assertSame(VALUE_SHARED_OUTER, MDC.get(KEY_SHARED));
        assertNull(MDC.get(KEY_INNER));
      }
      assertNull(MDC.get(KEY_OUTER));
      assertNull(MDC.get(KEY_SHARED));
      assertNull(MDC.get(KEY_INNER));
    }).get();
  }

  @Test
  public final void applyInTheSameThread() {
    {//prepare outer MDC
      MDC.clear();
      MDC.put(KEY_OUTER, VALUE_OUTER);
      MDC.put(KEY_SHARED, VALUE_SHARED_OUTER);
    }
    final TransferableMdc outerMdc = TransferableMdc.current();
    assertSame(VALUE_OUTER, MDC.get(KEY_OUTER));
    assertSame(VALUE_SHARED_OUTER, MDC.get(KEY_SHARED));
    assertNull(MDC.get(KEY_INNER));
    {//prepare inner MDC
      MDC.clear();
      MDC.put(KEY_SHARED, VALUE_SHARED_INNER);
      MDC.put(KEY_INNER, VALUE_INNER);
    }
    try (var transferredMdc = outerMdc.transfer()) {
      assertSame(VALUE_OUTER, MDC.get(KEY_OUTER));
      assertSame(VALUE_SHARED_OUTER, MDC.get(KEY_SHARED));
      assertNull(MDC.get(KEY_INNER));
    }
    //once transferredMdc is closed, we must see the inner MDC
    assertNull(MDC.get(KEY_OUTER));
    assertSame(VALUE_SHARED_INNER, MDC.get(KEY_SHARED));
    assertSame(VALUE_INNER, MDC.get(KEY_INNER));
  }

  @Test
  public final void closeMultipleTimes() {
    final TransferableMdc transferredMdc = TransferableMdc.current().transfer();
    transferredMdc.close();
    assertThrows(IllegalStateException.class, transferredMdc::close);
  }
}
