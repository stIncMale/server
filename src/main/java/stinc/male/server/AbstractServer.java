package stinc.male.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public abstract class AbstractServer implements Server {
  private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

  private CompletableFuture<Void> futureStop;
  private final Object mutexStartShutdown;

  protected AbstractServer() {
    mutexStartShutdown = new Object();
  }

  /**
   * This method is called from {@link #start()} only when it is required,
   * so it does not need to care if the server was already started/stopped.
   *
   * @param futureStop The same {@link CompletableFuture} method {@link #start()} returns.
   */
  protected abstract void doStart(CompletableFuture<Void> futureStop);

  /**
   * This method is called from {@link #stop()} only when it is required,
   * so it does not need to care if the server was already started/stopped.
   *
   * @param futureStop The same {@link CompletableFuture} method {@link #start()} returns.
   */
  protected abstract void doStop(CompletableFuture<Void> futureStop);

  @Override
  public CompletableFuture<Void> start() {
    final CompletableFuture<Void> result;
    synchronized (mutexStartShutdown) {
      if (futureStop == null) {
        result = new CompletableFuture<>();
        futureStop = result;
        result.thenRun(() -> logger.info("{} stopped", this));
        if (Thread.currentThread().isInterrupted()) {
          result.completeExceptionally(new InterruptedException());
        } else {
          try {
            doStart(result);
            if (!result.isCompletedExceptionally()) {
              logger.info("{} started", this);
            }
          } catch (final RuntimeException e) {
            result.completeExceptionally(e);
          }
        }
      } else {
        result = futureStop;
      }
    }
    return result;
  }

  @Override
  public void stop() throws InterruptedException {
    synchronized (mutexStartShutdown) {
      if (futureStop == null) {
        //nothing to do
      } else {
        if (!futureStop.isDone()) {
          try {
            doStop(futureStop);
          } catch (final RuntimeException e) {
            futureStop.completeExceptionally(e);
          }
          try {
            futureStop.get();
          } catch (final InterruptedException e) {
            throw e;
          } catch (final ExecutionException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }
}