package stinc.male.server.reqres;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletionStage;

/**
 * Processes requests.
 *
 * @param <RQ> A type of the request this {@link RequestProcessor} {@linkplain #process(java.lang.Object) processes}.
 * @param <RS> A type of the response.
 */
@ThreadSafe
public interface RequestProcessor<RQ, RS> {
  /**
   * Processes the {@code request}.
   *
   * @return {@link CompletionStage} that will be completed when the result is ready.
   * If there is no any result, then this {@link CompletionStage} may be completed with {@code null}.
   */
  CompletionStage<RS> process(RQ request);
}