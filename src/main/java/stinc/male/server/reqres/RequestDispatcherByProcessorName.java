package stinc.male.server.reqres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link RequestDispatcher} that chooses {@link RequestProcessor}s by names.
 */
@ThreadSafe
public abstract class RequestDispatcherByProcessorName<RQ, RS> implements RequestDispatcher<RQ, RS> {
  private static final Logger logger = LoggerFactory.getLogger(RequestDispatcherByProcessorName.class);

  private final Map<String, ? extends RequestProcessor<RQ, RS>> processors;

  /**
   * @param processors {@link Map} that contains available {@link RequestProcessor}s as {@linkplain Entry#getValue() values}
   * and names of {@link RequestProcessor}s as {@linkplain Entry#getKey() keys}.
   */
  protected RequestDispatcherByProcessorName(Map<String, ? extends RequestProcessor<RQ, RS>> processors) {
    this.processors = processors;
  }

  /**
   * Calls {@link #getProcessorName(java.lang.Object)} to determine {@link RequestProcessor}'s name,
   * finds the {@link RequestProcessor} by this name among available processors and delegates processing of the
   * {@code request} to the found {@link RequestProcessor}.
   */
  @Override
  public final CompletionStage<RS> process(final RQ request) {
    checkNotNull(request, "The argument must not be null", "request");
    final String processorName = getProcessorName(request);
    @Nullable final RequestProcessor<? super RQ, RS> processor = processors.get(processorName);
    if (processor == null) {
      throw new RuntimeException(String.format("Unknown %s's name %s. Available processors are %s",
          RequestProcessor.class.getSimpleName(), processorName, processors));
    }
    logger.debug("{} will be processed by processor {} with name {}", request, processor, processorName);
    return processor.process(request);
  }

  protected Map<String, RequestProcessor<RQ, RS>> getProcessors() {
    return Collections.unmodifiableMap(processors);
  }

  /**
   * This method must return name of a {@link RequestProcessor} which can be found among available processors.
   */
  protected abstract String getProcessorName(RQ request);
}