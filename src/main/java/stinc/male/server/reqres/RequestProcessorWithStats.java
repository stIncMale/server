package stinc.male.server.reqres;

import com.timgroup.statsd.StatsDClient;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionStage;
import static com.google.common.base.Preconditions.checkNotNull;
import stinc.male.server.util.logging.TransferableMdc;

/**
 * Wraps {@link RequestProcessor} and collects statistics via {@link StatsDClient}.
 */
@ThreadSafe
public final class RequestProcessorWithStats<RQ, RS> implements RequestProcessor<RQ, RS> {
  private static final Logger logger = LoggerFactory.getLogger(RequestProcessorWithStats.class);

  private final RequestProcessor<? super RQ, ? extends RS> processor;
  @Nullable
  private final StatsDClient statsDClient;
  private final String[] statsTags;

  public RequestProcessorWithStats(
      final RequestProcessor<? super RQ, ? extends RS> processor,
      @Nullable final StatsDClient statsDClient,
      @Nullable final Collection<String> statsTags) {
    checkNotNull(processor, "The argument %s must not be null", "processor");
    this.processor = processor;
    this.statsDClient = statsDClient;
    this.statsTags = statsTags == null || statsTags.isEmpty() || statsDClient == null
        ? new String[0]
        : statsTags.toArray(new String[statsTags.size()]);
  }

  @Override
  public final CompletionStage<RS> process(final RQ request) {
    checkNotNull(request, "The argument %s must not be null", "request");
    final long beginInstantMillis = System.currentTimeMillis();
    final TransferableMdc mdc = TransferableMdc.current();
    return processor.process(request)
        .handle((response, failure) -> {
          try (final TransferableMdc mdcTmp = mdc.apply()) {
            final long endInstantMillis = System.currentTimeMillis();
            collectStats(endInstantMillis - beginInstantMillis);
            if (failure != null) {
              throw new RuntimeException(failure);
            }
            return response;
          }
        });
  }

  private final void collectStats(final long processingTimeMillis) {
    @Nullable final String timeAspect;
    if (logger.isDebugEnabled() || statsDClient != null) {
      timeAspect = processor.getClass().getSimpleName() + ".processingTimeMillis";
    } else {
      timeAspect = null;
    }
    logger.debug("{}={}", timeAspect, processingTimeMillis);
    if (statsDClient != null) {
      String countAspect = processor.getClass().getSimpleName() + ".requestsCount";
      statsDClient.incrementCounter(countAspect, statsTags);
      statsDClient.recordExecutionTime(timeAspect, processingTimeMillis, statsTags);
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(processor=" + processor
        + ", statsDClient=" + statsDClient
        + ", statsTags=" + Arrays.toString(statsTags)
        + ')';
  }
}