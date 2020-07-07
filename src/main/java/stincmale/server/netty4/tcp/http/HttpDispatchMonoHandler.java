package stincmale.server.netty4.tcp.http;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stincmale.server.netty4.RequestWithMetadata;
import stincmale.server.netty4.tcp.DispatchMonoHandler;
import stincmale.server.netty4.tcp.http.util.HttpUtil;
import stincmale.server.reqres.RequestDispatcher;
import stincmale.server.util.logging.TransferableMdc;
import stincmale.server.ExternallyVisibleException;
import stincmale.server.util.throwable.ThrowableUtil;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * {@link DispatchMonoHandler} that expects {@link RequestWithMetadata}{@code <? extends }{@link FullHttpRequest}{@code >}
 * messages and sends {@link FullHttpResponse}s downstream.
 * <p>
 * {@link HttpDispatchMonoHandler} handles {@code Connection} HTTP header (a.k.a. {@code keep-alive}) so that
 * {@link RequestDispatcher} SHOULD NOT care about it.
 */
@ThreadSafe
@Sharable
public class HttpDispatchMonoHandler extends DispatchMonoHandler<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
  private static final Logger logger = LoggerFactory.getLogger(HttpDispatchMonoHandler.class);

  /**
   * @param dispatcher See {@link DispatchMonoHandler#DispatchMonoHandler(RequestDispatcher, long)}.
   * @param connectionIdleTimeoutMillis See {@link DispatchMonoHandler#DispatchMonoHandler(RequestDispatcher, long)}.
   */
  public HttpDispatchMonoHandler(
      final RequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> dispatcher,
      final long connectionIdleTimeoutMillis) {
    super(new HttpRequestDispatcherWrapper(dispatcher), connectionIdleTimeoutMillis);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Wraps {@code msg} into {@link RequestWithMetadata} if it's not already a {@link RequestWithMetadata}.
   */
  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof RequestWithMetadata) {
      super.channelRead(ctx, msg);
    } else {
      super.channelRead(ctx, new RequestWithMetadata<>((FullHttpRequest)msg));
    }
  }

  @Override
  protected FullHttpResponse failureResponse(@Nullable final RequestWithMetadata<? extends FullHttpRequest> request, final Throwable failure) {
    final FullHttpResponse result = HttpUtil.createHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    ThrowableUtil.extract(failure, IllegalArgumentException.class)
        .ifPresent(e -> result.setStatus(BAD_REQUEST));
    ThrowableUtil.extract(failure, ExternallyVisibleException.class)
        .ifPresent(e -> HttpUtil.setPlainTextUtf8Content(result, e.getExternalMessage()));
    logger.error(
        String.format("Processing of the %s has failed. Responding with %s", request == null ? "<unknown request>" : request, result),
        failure);
    return result;
  }

  private static final class HttpRequestDispatcherWrapper
      implements RequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
    private final RequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> dispatcher;

    private HttpRequestDispatcherWrapper(final RequestDispatcher<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> dispatcher) {
      checkNotNull(dispatcher, "The argument %s must not be null", "dispatcher");
      this.dispatcher = dispatcher;
    }

    @Override
    public final CompletionStage<FullHttpResponse> process(final RequestWithMetadata<? extends FullHttpRequest> request) {
      final TransferableMdc mdc = TransferableMdc.current();
      return dispatcher.process(request)
          .handle((httpResponse, failure) -> {
            try (var transferredMdc = mdc.transfer()) {
              if (failure != null) {
                throw new RuntimeException(failure);
              }
              final HttpMessage httpRequest = request.request();
              io.netty.handler.codec.http.HttpUtil.setKeepAlive(httpResponse, io.netty.handler.codec.http.HttpUtil.isKeepAlive(httpRequest));
              return httpResponse;
            }
          });
    }
  }
}
