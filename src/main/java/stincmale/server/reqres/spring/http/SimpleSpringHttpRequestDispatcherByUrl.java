package stincmale.server.reqres.spring.http;

import com.timgroup.statsd.StatsDClient;
import stincmale.server.reqres.spring.SpringRequestDispatcher;
import stincmale.server.netty4.RequestWithMetadata;
import stincmale.server.netty4.tcp.http.SimpleHttpRequestDispatcherByUrl;
import stincmale.server.reqres.RequestDispatcher;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.springframework.context.ApplicationContext;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This {@link RequestDispatcher} combines functionality of the {@link SpringRequestDispatcher} and
 * {@link SimpleHttpRequestDispatcherByUrl}.
 */
@ThreadSafe
public class SimpleSpringHttpRequestDispatcherByUrl
    extends SpringRequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
  private static final class InnerUrlDispatcher extends SimpleHttpRequestDispatcherByUrl {
    private InnerUrlDispatcher(final String contextPath) {
      super(Collections.emptyMap(), contextPath);
    }

    @Override
    protected final String getProcessorName(RequestWithMetadata<? extends FullHttpRequest> request) {
      return super.getProcessorName(request);
    }
  }

  private final InnerUrlDispatcher urlDispatcher;

  /**
   * @param appCtx See
   * {@link SpringRequestDispatcher#SpringRequestDispatcher(ApplicationContext, Collection, boolean, StatsDClient)}.
   * @param packageNames See
   * {@link SpringRequestDispatcher#SpringRequestDispatcher(ApplicationContext, Collection, boolean, StatsDClient)}.
   * @param searchRecursively See {@link SpringRequestDispatcher#SpringRequestDispatcher(ApplicationContext, Collection, boolean, StatsDClient)}.
   * @param contextPath See {@link SimpleHttpRequestDispatcherByUrl#SimpleHttpRequestDispatcherByUrl(java.util.Map, java.lang.String)}.
   * @param statsDClient See {@link SimpleHttpRequestDispatcherByUrl#SimpleHttpRequestDispatcherByUrl(java.util.Map, java.lang.String)}.
   */
  public SimpleSpringHttpRequestDispatcherByUrl(
      final ApplicationContext appCtx,
      @Nullable final Collection<String> packageNames,
      final boolean searchRecursively,
      @Nullable StatsDClient statsDClient,
      final String contextPath) {
    super(appCtx, packageNames, searchRecursively, statsDClient);
    this.urlDispatcher = new InnerUrlDispatcher(contextPath);
  }

  @Override
  protected String getProcessorName(final RequestWithMetadata<? extends FullHttpRequest> request) {
    checkNotNull(request, "The argument %s must not be null", "request");
    return urlDispatcher.getProcessorName(request);
  }
}
