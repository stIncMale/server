package stinc.male.server.netty4.tcp.http;

import java.nio.charset.StandardCharsets;
import stinc.male.server.netty4.RequestWithMetadata;
import stinc.male.server.reqres.Processor;
import stinc.male.server.reqres.RequestProcessor;
import stinc.male.server.reqres.RequestDispatcherByProcessorName;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link RequestDispatcherByProcessorName} that uses URI path of an HTTP request as the {@linkplain Processor#value() name} of a
 * {@link RequestProcessor}.
 */
@ThreadSafe
public class SimpleHttpRequestDispatcherByUrl
    extends RequestDispatcherByProcessorName<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
  private final String contextPath;

  /**
   * @param processors See {@link RequestDispatcherByProcessorName#RequestDispatcherByProcessorName(java.util.Map)}.
   * @param contextPath A portion of the HTTP request URI that indicates the context of the request.
   * The context path always comes first in a request URI.
   * The path starts with a {@code /} character but does not end with a {@code /} character.
   * Use empty string {@code ""} to specify the root context path.
   * <p>
   * {@code contextPath} is used by the
   * {@link #getProcessorName(RequestWithMetadata)} method.
   * The {@code contextPath} is subtracted from URI path of the HTTP request and the result is used as name of a
   * {@link RequestProcessor}:
   * <table summary = "">
   * <tr><td><b>contextPath</b></td><td><b>HTTP URL path</b></td><td><b>getProcessorName()</b></td></tr>
   * <tr><td>""</td><td>"/"</td><td>"/"</td></tr>
   * <tr><td>"/context/path"</td><td>"/context/path/req"</td><td>"/req"</td></tr>
   * <tr><td>"/context/path"</td><td>"/req"</td><td>"/req"</td></tr>
   * </table>
   */
  public SimpleHttpRequestDispatcherByUrl(
      final Map<String, ? extends RequestProcessor<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse>> processors,
      final String contextPath) {
    super(processors);
    checkNotNull(contextPath, "The argument %s must not be null", "contextPath");
    checkArgument(contextPath.isEmpty() || contextPath.startsWith("/"),
        "The argument %s must either be empty or start with /", "contextPath");
    checkArgument(!contextPath.endsWith("/"), "The argument %s must not end with /", "contextPath");
    this.contextPath = contextPath;
  }

  @Override
  protected String getProcessorName(final RequestWithMetadata<? extends FullHttpRequest> request) {
    checkNotNull(request, "The argument %s must not be null", "request");
    final FullHttpRequest httpRequest = request.request();
    final String strUri = httpRequest.uri();
    final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(strUri, StandardCharsets.UTF_8);
    final String path = queryStringDecoder.path();
    final String result;
    if (path.startsWith(contextPath)) {
      result = path.substring(contextPath.length());
    } else {
      result = path;
    }
    return result;
  }
}