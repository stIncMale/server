package stincmale.server.util.logging;

import java.util.Collections;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * A read-only {@link MDCAdapter} which allows to copy the state of {@link MDC} between {@linkplain Thread threads}.
 * <p>
 * This class is not thread-safe but correctly transfers {@link MDC} if used according to the provided idiom.
 * <p>
 * <b>Usage examples.</b>
 * <p>
 * <i>Correct:</i>
 * <pre>{@code
 * TransferableMdc outerMdc = TransferableMdc.current();
 * executor.submit(() -> {
 *  try (var transferredMdc = outerMdc.transfer()) {
 *      logger.info("This call can access contents of outerMdc via org.slf4j.MDC");
 *  }
 *  logger.info("This call cannot access contents of outerMdc via org.slf4j.MDC");
 * });
 * }</pre>
 * <p>
 * <i>Incorrect 1:</i>
 * <pre>{@code
 * executor.submit(() -> {
 *  try (var transferredMdc = TransferableMdc.current().transfer()) {
 *      //...
 *  }
 * });
 * }</pre>
 * This is incorrect because we do not have access to the context data of the "outer" thread.
 * <p>
 * <i>Incorrect 2:</i>
 * <pre>{@code
 * TransferableMdc outerMdc = TransferableMdc.current();
 * executor.submit(() -> {//task1
 *  try (var transferredMdc = outerMdc.transfer()) {
 *      //...
 *  }
 * });
 * executor.submit(() -> {//task2
 *  try (var transferredMdc = outerMdc.transfer()) {
 *      //...
 *  }
 * });
 * }</pre>
 * This is incorrect because {@code task1} and {@code task2} may be executed concurrently and hence
 * methods {@link #transfer()} and {@link #close()} may be executed concurrently, and these methods are not thread-safe.
 * In order to prevent such incorrect usages, {@link TransferableMdc#close()}
 * throws {@link IllegalStateException} if detects that the method is called more than once.
 * The correct usage idiom:
 * <pre>{@code
 * TransferableMdc outerMdc1 = TransferableMdc.current();
 * executor.submit(() -> {//task1
 *  try (var transferredMdc = outerMdc1.transfer()) {
 *      //...
 *  }
 * });
 * TransferableMdc outerMdc2 = TransferableMdc.current();
 * executor.submit(() -> {//task2
 *  try (var transferredMdc = outerMdc2.transfer()) {
 *      //...
 *  }
 * });
 * }</pre>
 */
@NotThreadSafe
public final class TransferableMdc implements MDCAdapter, Closeable {
  /**
   * Creates {@link TransferableMdc} which holds current {@linkplain Thread thread}'s
   * {@link MDC} {@linkplain MDC#getCopyOfContextMap() context map}.
   *
   * @return New {@link TransferableMdc} which state is identical to the current
   * {@linkplain Thread thread}'s state of {@link MDC}.
   */
  public static final TransferableMdc current() {
    return new TransferableMdc(MDC.getCopyOfContextMap());
  }

  private final Map<String, String> context;
  /**
   * null value is used to mark {@link TransferableMdc} as closed by {@link #close()}.
   * We could have used another field for this, but given the nature of this class, we should be as compact as possible.
   */
  @Nullable
  private Map<String, String> backup;

  private TransferableMdc(@Nullable final Map<String, String> contextMapCopy) {
    context = contextMapCopy == null ? Collections.emptyMap() : contextMapCopy;
  }

  /**
   * {@linkplain MDC#setContextMap(Map) Overrides} the {@linkplain Thread#currentThread() current thread's} {@link MDC}
   * with the {@linkplain MDC#getCopyOfContextMap() state} of {@link TransferableMdc}
   * and retains the original state of the current {@link MDC}.
   *
   * @return {@code this}.
   *
   * @see #close()
   */
  public final TransferableMdc transfer() {
    @Nullable
    final Map<String, String> currentContext = MDC.getCopyOfContextMap();
    backup = currentContext == null ? Collections.emptyMap() : currentContext;
    MDC.setContextMap(context);
    return this;
  }

  /**
   * {@linkplain MDC#setContextMap(Map) Rolls back} the {@linkplain Thread#currentThread() current thread's} {@link MDC} to its original state
   * (before {@link #transfer()} was called).
   *
   * @throws IllegalStateException if detects that the method is called more than once.
   * Such detection is not guaranteed and is provided on the best effort basis.
   */
  @Override
  public void close() throws IllegalStateException {
    /* An incorrect call of the transfer method may change this back to non-null;
     * this.backup is a plain variable so if the method close is incorrectly called by multiple threads,
     * the null value written below may not be observed.
     * These are the reasons why we specify in the docs that detection of incorrect usage is not guaranteed.
     * Obviously, it is possible to make it reliable, but not worth it because of the complexity and potential performance effects.
     * After all, all we are trying to accomplish here is to let a user know that the class is used incorrectly.*/
    if (backup == null) {
      throw new IllegalStateException(getClass().getSimpleName() + " must not be reused and cannot be closed more than once");
    }
    MDC.setContextMap(backup);
    backup = null;
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
    return (context == null) ? null : new HashMap<>(context);
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
