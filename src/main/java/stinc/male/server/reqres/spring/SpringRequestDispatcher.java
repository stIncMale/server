package stinc.male.server.reqres.spring;

import com.timgroup.statsd.StatsDClient;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import stinc.male.server.reqres.Processor;
import stinc.male.server.reqres.RequestDispatcher;
import stinc.male.server.reqres.RequestDispatcherByProcessorName;
import stinc.male.server.reqres.RequestProcessor;
import stinc.male.server.reqres.RequestProcessorWithStats;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link RequestDispatcherByProcessorName} that automatically searches for {@link RequestProcessor}s annotated with
 * {@code @}{@link Processor}.
 */
@ThreadSafe
public abstract class SpringRequestDispatcher<RQ, RS> extends RequestDispatcherByProcessorName<RQ, RS> {
  private static final Logger logger = LoggerFactory.getLogger(SpringRequestDispatcher.class);

  /**
   * @param packageNames A {@link Collection} of Java packages where to search for Spring beans annotated with {@code @}{@link Processor}.
   * If {@code null} then search will be performed in all packages known to {@code appCtx}.
   * @param searchRecursively Specifies if subpackages of {@code packageNames} must be searched.
   */
  protected SpringRequestDispatcher(
      final ApplicationContext appCtx,
      @Nullable final Collection<String> packageNames,
      final boolean searchRecursively,
      @Nullable final StatsDClient statsDClient) {
    super(getProcessors(appCtx, packageNames, searchRecursively, statsDClient));
  }

  /*
   * This method doesn't account nested classes. It may be improved in the future if there will be such a need.
   */
  private static final boolean isInPackage(final Class<?> klass, final String packageName, final boolean recursively) {
    final String className = klass.getName();
    final boolean result;
    if (className.contains(".")) {
      final String classPackageName = className.substring(0, className.lastIndexOf('.'));
      result = recursively ? classPackageName.startsWith(packageName) : classPackageName.equals(packageName);
    } else {//klass is in the default package
      result = packageName.isEmpty();
    }
    return result;
  }

  private static final boolean isInPackages(final Class<?> klass, final Collection<String> packageNames, final boolean recursively) {
    boolean result = false;
    for (final String packageName : packageNames) {
      if (isInPackage(klass, packageName, recursively)) {
        result = true;
        break;
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static final <RQ, RS> Map<String, RequestProcessor<RQ, RS>> getProcessors(
      final ApplicationContext appCtx,
      @Nullable final Collection<String> packageNames,
      final boolean searchRecursively,
      @Nullable final StatsDClient statsDClient) {
    checkNotNull(appCtx, "The argument %s must not be null", "appCtx");
    final Map<String, RequestProcessor<RQ, RS>> result = new HashMap<>();
    appCtx.getBeansWithAnnotation(Processor.class)
        .entrySet()
        .forEach(beanEntry -> {
          final Object bean = beanEntry.getValue();
          if (bean instanceof RequestProcessor) {
            if (bean instanceof RequestDispatcher) {
              throw new RuntimeException(String.format(
                  "Bean %s is annotated with @%s but is of type %s",
                  bean, Processor.class.getSimpleName(), RequestDispatcher.class.getSimpleName()));
            }
            final Class<?> processorClass = bean.getClass();
            if (packageNames == null || isInPackages(processorClass, packageNames, searchRecursively)) {
              final Processor processorAnnotation = processorClass.getAnnotation(Processor.class);
              final String processorName = processorAnnotation.value();
              final RequestProcessorWithStats<RQ, RS> processor
                  = addStats((RequestProcessor<RQ, RS>) bean, statsDClient, Collections.singleton(String.format("type:%s", processorName)));
              result.put(processorName, processor);
            }
          } else {
            throw new RuntimeException(String.format(
                "Bean %s is annotated with @%s but isn't of type %s",
                bean, Processor.class.getSimpleName(), RequestProcessor.class.getSimpleName()));
          }
        });
    logger.info("Processors {} were found in packages {}", result, packageNames);
    return result;
  }

  @SuppressWarnings("unchecked")
  private static <RQ, RS> RequestProcessorWithStats<RQ, RS> addStats(
      final RequestProcessor<RQ, RS> processor,
      @Nullable final StatsDClient statsDClient,
      final Collection<String> statsTags) {
    return (processor instanceof RequestProcessorWithStats)
        ? (RequestProcessorWithStats<RQ, RS>) processor
        : new RequestProcessorWithStats<>(processor, statsDClient, statsTags);
  }
}