package stinc.male.server.util.logging;

import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * An read-only {@link MDCAdapter} which allows to copy state of {@link MDC} between {@linkplain Thread threads}.
 * <p>
 * This class is not thread-safe, but it correctly transfers {@link MDC} if used according the provided idiom.
 * <p>
 * <b>Usage examples.</b>
 * <p>
 * <i>Correct</i>:
 * <pre>{@code
 * final Mdc mdc = Mdc.current();
 * executor.submit(() -> {
 *  try (@SuppressWarnings("unused") Mdc mdcTmp = mdc.apply()) {
 *      logger.info("This call can access contents of mdc via org.slf4j.MDC");
 *  }
 *  logger.info("This call can not access contents of mdc via org.slf4j.MDC");
 * });
 * }</pre>
 * <i>Incorrect</i>:
 * <pre>{@code
 * final Mdc mdc = Mdc.current();
 * executor.submit(() -> {//task1
 *  try (Mdc mdcTmp = mdc.apply()) {
 *      //...
 *  }
 * });
 * executor.submit(() -> {//task2
 *  try (Mdc mdcTmp = mdc.apply()) {
 *      //...
 *  }
 * });
 * }</pre>
 * The latter example is incorrect because {@code task1} and {@code task2} may be executed concurrently and hence
 * methods {@link #apply()} and {@link #close()} may be executed concurrently, but these methods are not thread-safe.
 * The simplest way to fix the incorrect example is as follows:
 * <pre>{@code
 * final Mdc mdc1 = Mdc.current();
 * executor.submit(() -> {//task1
 *  try (Mdc mdcTmp = mdc1.apply()) {
 *      //...
 *  }
 * });
 * final Mdc mdc2 = Mdc.current();
 * executor.submit(() -> {//task2
 *  try (Mdc mdcTmp = mdc2.apply()) {
 *      //...
 *  }
 * });
 * }</pre>
 */
@NotThreadSafe
public final class Mdc implements MDCAdapter, Closeable {
  /**
   * Creates {@link Mdc} which holds current {@linkplain Thread thread}'s
   * {@link MDC} {@linkplain MDC#getCopyOfContextMap() context map}.
   *
   * @return New {@link Mdc} which state is identical to the current
   * {@linkplain Thread thread}'s state of {@link MDC}.
   */
  public static final Mdc current() {
    return new Mdc(MDC.getCopyOfContextMap());
  }

  @Nullable
  private final Map<String, String> context;
  @Nullable
  private Map<String, String> backup;

  private Mdc(@Nullable final Map<String, String> contextMap) {
    context = (contextMap == null || contextMap.isEmpty()) ? null : new HashMap<String, String>(contextMap);
  }

  /**
   * Merges this {@link Mdc} into the current {@link Thread}'s {@link MDC}
   * and retains original state of the current {@link MDC}.
   *
   * @return {@code this}.
   */
  public final Mdc apply() {
    if (context != null) {
      @Nullable final Map<String, String> currentContext = MDC.getCopyOfContextMap();
      backup = (currentContext == null || currentContext.isEmpty()) ? null : currentContext;
      context.forEach(MDC::put);
    }
    return this;
  }

  /**
   * Restores current {@link Thread}'s {@link MDC} to its state before {@link #apply()}
   * (this state was retained by {@link #apply()}).
   */
  @Override
  public void close() {
    if (context != null) {
      MDC.clear();
      if (backup != null) {
        MDC.setContextMap(backup);
        backup = null;
      }
    }
  }

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public final void put(final String key, final String val) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public final String get(final String key) {
    if (key == null) {
      throw new NullPointerException("The argument key must not be null");
    }
    return (context == null) ? null : context.get(key);
  }

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public final void remove(final String key) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public final void clear() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public final Map<String, String> getCopyOfContextMap() {
    return (context == null) ? null : new HashMap<String, String>(context);
  }

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  public final void setContextMap(@Nullable final Map<String, String> contextMap) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(context=" + context
        + ')';
  }
}