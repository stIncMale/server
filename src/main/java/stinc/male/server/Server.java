package stinc.male.server;

import java.util.concurrent.CompletionStage;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Represents an I/O server.
 */
@ThreadSafe
public interface Server {
  /**
   * Enables the {@link Server} to perform I/O
   * operations until {@link #stop()} is invoked or until an implementation decided to cease all operations.
   * This method is allowed to be called multiple times.
   *
   * @return {@link CompletionStage} that may be used to get notified when the server stops operating,
   * or if the server fails to start.
   * @see #start()
   * @see #stop()
   */
  CompletionStage<Void> start();

  /**
   * Disables the {@link Server} so it does not perform I/O and any other
   * operations anymore if the {@link Server} was {@link #start() started}, otherwise does nothing.
   * This method is allowed to be called multiple times.
   *
   * @see #start()
   */
  void stop() throws InterruptedException;
}