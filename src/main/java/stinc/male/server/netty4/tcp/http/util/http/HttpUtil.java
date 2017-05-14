package stinc.male.server.netty4.tcp.http.util.http;

import com.google.common.base.Charsets;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.lang3.StringUtils;
import javax.annotation.Nullable;
import java.util.Optional;
import stinc.male.server.netty4.util.channel.ChannelUtil;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides useful utility methods.
 */
public final class HttpUtil {
  public static final FullHttpResponse createHttpResponse(
      final HttpVersion version, final HttpResponseStatus status) {
    checkNotNull(version, "The argument %s must not be null", "version");
    checkNotNull(status, "The argument %s must not be null", "status");
    final FullHttpResponse result = new DefaultFullHttpResponse(version, status);
    final HttpHeaders responseHeaders = result.headers();
    responseHeaders.clear();
    responseHeaders.add(HttpHeaders.Names.CONTENT_LENGTH, 0);
    return result;
  }

  public static final FullHttpResponse setPlainTextUtf8Content(
      final FullHttpResponse httpResponse, @Nullable final String content) {
    checkNotNull(httpResponse, "The argument %s must not be null", "httpResponse");
    final HttpHeaders responseHeaders = httpResponse.headers();
    if (content != null) {
      final byte[] byteHttpResponseContent = content.getBytes(Charsets.UTF_8);
      httpResponse.content()
          .clear()
          .writeBytes(byteHttpResponseContent);
      responseHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, byteHttpResponseContent.length);
    } else {
      httpResponse.content()
          .clear();
      responseHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, 0);
    }
    responseHeaders.add(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8");
    return httpResponse;
  }

  public static final Optional<String> getRemoteAddress(final HttpHeaders headers) {
    checkNotNull(headers, "The argument %s must not be null", "headers");
    @Nullable
    String result = headers.get("x-forwarded-for");
    if (result == null) {
      result = headers.get("X-Real-IP");
    }
    return StringUtils.isBlank(result)
        ? Optional.empty()
        : Optional.of(result.split("[:,\\s]")[0].trim());
  }

  public static Optional<String> getRemoteAddress(final HttpHeaders headers, final Channel channel) {
    checkNotNull(headers, "The argument %s must not be null", "headers");
    checkNotNull(channel, "The argument %s must not be null", "channel");
    return Optional.ofNullable(getRemoteAddress(headers)
        .orElseGet(() -> ChannelUtil.getRemoteAddress(channel)
            .orElse(null)));
  }

  private HttpUtil() {
    throw new UnsupportedOperationException("This class is not designed to be instantiated");
  }
}