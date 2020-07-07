package stinc.male.server.netty4.tcp;

import stinc.male.server.util.logging.TransferableMdc;
import stinc.male.server.reqres.RequestDispatcher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCounted;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.timeout.IdleState.ALL_IDLE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Processes business logic.
 * Uses {@link RequestDispatcher} to {@linkplain RequestDispatcher#process(java.lang.Object) process} requests.
 * This handler must be placed in the {@link ChannelPipeline} above (after in the inbound/upstream evaluation order) any decoders,
 * and above (before in the outbound/downstream evaluation order) any encoders:
 * <pre>{@code
 *  ChannelPipeline p = ...;
 *  ...
 *  p.addLast(new MyDecoder());
 *  ...
 *  p.addLast(new MyEncoder());
 *  ...
 *  p.addLast(new DispatchMonoHandler(...));//handles business logic
 *  ...
 *  p.addLast(new MyExceptionHandler());
 *  ...
 * }</pre>
 * and must not be added to the {@link ChannelPipeline} more than once.
 * This handler adds {@link MonoHandler} in the {@link ChannelPipeline} right below itself in order to guarantee
 * that at any given moment not more than one request per {@link Channel} is being processed by
 * {@link RequestDispatcher}.
 * Note that {@link MonoHandler} disables {@linkplain ChannelConfig#setAutoRead(boolean) auto read}.
 *
 * @param <RQ> A type of the inbound message this {@link ChannelInboundHandlerAdapter} expects.
 * @param <RS> A type of the outbound message this {@link ChannelInboundHandlerAdapter} sends downstream.
 */
@ThreadSafe
@Sharable
public class DispatchMonoHandler<RQ, RS> extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DispatchMonoHandler.class);
  private static final Object DEFAULT_VOID_RESPONSE = MonoHandler.VOID_OUTBOUND_MESSAGE;
  private static final String IDLE_HANDLER_NAME
      = DispatchMonoHandler.class.getSimpleName() + "_" + IdleStateHandler.class.getSimpleName();
  private static final String MONO_HANDLER_NAME
      = DispatchMonoHandler.class.getSimpleName() + "_" + MonoHandler.class.getSimpleName();

  private final RequestDispatcher<RQ, RS> dispatcher;
  private final long connectionIdleTimeoutMillis;

  /**
   * @param connectionIdleTimeoutMillis An interval of inactivity of a TCP connection (no writes and no reads) in milliseconds after which the connection
   * will be closed.
   * Specify negative value to disable such a behavior;
   * specify 0 to close connections immediately after sending a response
   * (this particular behavior may be altered via overriding method {@link #closeChannelAfterResponse(Object, Object, Throwable)}).
   */
  public DispatchMonoHandler(RequestDispatcher<RQ, RS> dispatcher, long connectionIdleTimeoutMillis) {
    checkNotNull(dispatcher, "The argument %s must not be null", "dispatcher");
    this.dispatcher = dispatcher;
    this.connectionIdleTimeoutMillis = connectionIdleTimeoutMillis;
  }

  /**
   * Adds {@link IdleStateHandler} (if {@link #getConnectionIdleTimeoutMillis()} is positive), {@link MonoHandler} to
   * the {@link ChannelPipeline} right below this handler and
   * calls {@link ChannelInboundHandler#channelRegistered(io.netty.channel.ChannelHandlerContext)} method on these
   * handlers with the {@code ctx}.
   */
  @Override
  public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
    final String selfName = getSelfName(ctx.pipeline());
    final ChannelPipeline pipe = ctx.pipeline();
    if (connectionIdleTimeoutMillis > 0) {
      addIdleStateHandler(selfName, pipe).channelRegistered(ctx);
    }
    addMonoHandler(selfName, pipe).channelRegistered(ctx);
  }

  /**
   * Uses {@link RequestDispatcher} to {@linkplain RequestDispatcher#process(java.lang.Object) process} the
   * request ({@code msg}) and {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sends} response downstream the
   * {@link ChannelPipeline}. If response is {@code null},
   * then writes {@link MonoHandler#VOID_OUTBOUND_MESSAGE}, which results in no response being actually sent.
   * Does not {@linkplain ChannelHandlerContext#fireChannelRead(java.lang.Object) propagate} {@code msg} upstream.
   * <p>
   * Calls {@linkplain ReferenceCounted#release()} on the {@code msg} after completion of
   * processing and sending a response if {@code msg} is {@link ReferenceCounted}.
   */
  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    @SuppressWarnings("unchecked") final RQ request = (RQ)msg;
    CompletionStage<? extends RS> futureResponse;
    try {
      futureResponse = dispatcher.process(request);
    } catch (final Error e) {
      throw e;
    } catch (final Throwable e) {
      final CompletableFuture<? extends RS> failureResponse = new CompletableFuture<>();
      failureResponse.completeExceptionally(e);
      futureResponse = failureResponse;
    }
    respond(ctx, request, futureResponse);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent)evt;
      if (e.state() == ALL_IDLE && connectionIdleTimeoutMillis > 0) {
        final TransferableMdc mdc = TransferableMdc.current();
        ctx.close()
            .addListener((final ChannelFuture channelFuture) -> {
              try (final TransferableMdc ignored = mdc.apply()) {
                ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE.operationComplete(channelFuture);
              }
            });
      }
    }
    ctx.fireUserEventTriggered(evt);
  }

  /**
   * {@linkplain ChannelPipeline#remove(java.lang.String) Removes} {@link IdleStateHandler}, {@link MonoHandler} from
   * {@link ChannelPipeline}.
   */
  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
    final ChannelPipeline pipe = ctx.pipeline();
    if (connectionIdleTimeoutMillis > 0) {
      pipe.remove(IDLE_HANDLER_NAME);
    }
    pipe.remove(MONO_HANDLER_NAME);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable failure) throws Exception {
    final CompletableFuture<? extends RS> failureResponse = new CompletableFuture<>();
    failureResponse.completeExceptionally(failure);
    respond(ctx, null, failureResponse);
  }

  /**
   * Provides a response to {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) send} downstream if
   * {@linkplain RequestDispatcher#process(java.lang.Object) processing} of
   * the {@code request} has failed or some uncaught {@link Throwable} was thrown below in the {@link ChannelPipeline}.
   * This method is called from {@link #channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)},
   * {@link #exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)}
   * if current {@link Channel} is {@linkplain Channel#isActive() active}.
   *
   * @param request Provided if available.
   * @param failure A {@link Throwable} that was thrown during {@linkplain RequestDispatcher#process(java.lang.Object) processing}
   * the {@code request},or during {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sending}
   * a response, or during some other operations related to the {@code request} or the current {@link Channel}.
   *
   * @return {@code null}.
   */
  @Nullable
  protected RS failureResponse(@Nullable final RQ request, final Throwable failure) {
    logger.error(String.format("Processing of the %s has failed", request == null ? "<unknown request>" : request), failure);
    return null;
  }

  /**
   * Determines if {@link Channel} will be {@linkplain Channel#close() closed} after
   * {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sending} the {@code response}, or after
   * {@linkplain RequestDispatcher#process(java.lang.Object) processing} the {@code request} if there is no
   * {@code response}, or if some uncaught {@link Throwable} was thrown below in the {@link ChannelPipeline}.
   * <p>
   * This method is called from {@link #channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)},
   * {@link #exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)}
   * if current {@link Channel} is {@linkplain Channel#isActive() active}.
   *
   * @param request Provided if available.
   * @param response Provided if available.
   * @param failure A {@link Throwable} that was thrown during {@linkplain RequestDispatcher#process(java.lang.Object) processing}
   * the {@code request},or during {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sending}
   * the {@code response}, or during some other operations related to the {@code request} or the current {@link Channel}.
   *
   * @return {@code true} if {@linkplain #getConnectionIdleTimeoutMillis() connection idle timeout}
   * is {@code 0} and {@code failure} isn't {@code null}.
   */
  protected boolean closeChannelAfterResponse(
      @Nullable final RQ request, @Nullable final RS response, @Nullable final Throwable failure) {
    return connectionIdleTimeoutMillis == 0 || failure != null;
  }

  /**
   * Determines if {@link Throwable} must be {@linkplain ChannelPipeline#fireExceptionCaught(java.lang.Throwable) fired}
   * if it was thrown while {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sending} the {@code response} downstream
   * (in other words determines if the Netty 3 failure handling behavior must be preserved).
   *
   * @param request Provided if available.
   * @param response Provided if available.
   * @param sendFailure A {@link Throwable} that was thrown during {@linkplain ChannelHandlerContext#writeAndFlush(java.lang.Object) sending}
   * the {@code response}. This {@link Throwable} will be {@linkplain ChannelPipeline#fireExceptionCaught(java.lang.Throwable) fired}
   * if this method will return {@code true}.
   *
   * @return {@code true}.
   */
  protected boolean fireExceptionOnResponseWriteFailure(
      @Nullable final RQ request, final @Nullable RS response, final Throwable sendFailure) {
    return true;
  }

  protected final long getConnectionIdleTimeoutMillis() {
    return connectionIdleTimeoutMillis;
  }

  private final void respond(final ChannelHandlerContext ctx, @Nullable final RQ request, final CompletionStage<? extends RS> futureResponse) {
    final TransferableMdc mdc = TransferableMdc.current();
    futureResponse.whenComplete((response, failure) -> {
      try (final TransferableMdc ignored = mdc.apply()) {
        @Nullable
        ChannelFuture futureSend = null;
        try {
          final Channel channel = ctx.channel();
          if (channel.isActive()) {
            if (failure == null) {//request was processed successfully
              futureSend = ctx.writeAndFlush(response == null ? DEFAULT_VOID_RESPONSE : response);
            } else {//failed to process the request
              futureSend = ctx.writeAndFlush(internalFailureResponse(request, failure));
            }
          }
        } finally {
          if (futureSend == null) {//channel is inactive, or any unexpected situation has happened
            try {
              ctx.channel()
                  .close();
            } finally {
              release(request);
            }
          } else {
            final TransferableMdc mdc2 = TransferableMdc.current();
            futureSend.addListener((ChannelFuture future) -> {
              try (final TransferableMdc ignored2 = mdc2.apply()) {
                try {
                  if (future.isSuccess()) {
                    if (closeChannelAfterResponse(request, response, failure)) {
                      ChannelFutureListener.CLOSE.operationComplete(future);
                    }
                  } else {//everything is very bad for this channel
                    try {
                      ChannelFutureListener.CLOSE.operationComplete(future);
                    } finally {
                      if (fireExceptionOnResponseWriteFailure(request, response, future.cause())) {
                        ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE.operationComplete(future);
                      }
                    }
                  }
                } finally {
                  release(request);
                }
              }
            });
          }
        }
      }
    });
  }

  private Object internalFailureResponse(@Nullable final RQ request, final Throwable failure) {
    @Nullable
    Object result = failureResponse(request, failure);
    if (result == null) {
      result = DEFAULT_VOID_RESPONSE;
    }
    return result;
  }

  private final String getSelfName(final ChannelPipeline pipe) {
    @Nullable
    String result = null;
    for (final Map.Entry<String, ChannelHandler> channelHandlerEntry : pipe) {
      final ChannelHandler handler = channelHandlerEntry.getValue();
      if (handler instanceof DispatchMonoHandler) {
        if (handler == this) {
          result = channelHandlerEntry.getKey();
        } else {
          throw new RuntimeException(
              String.format("%s is already in the %s", DispatchMonoHandler.class.getSimpleName(), pipe));
        }
      }
    }
    if (result == null) {
      throw new RuntimeException("Can't find self in the pipeline");
    }
    return result;
  }

  private final IdleStateHandler addIdleStateHandler(final String selfName, final ChannelPipeline pipe) {
    @Nullable final ChannelHandler idleStateHandler = pipe.get(IdleStateHandler.class);
    if (idleStateHandler != null) {
      throw new RuntimeException(
          String.format("%s %s is already in the %s", IdleStateHandler.class.getSimpleName(), idleStateHandler, pipe));
    }
    final IdleStateHandler result = new IdleStateHandler(0, 0, connectionIdleTimeoutMillis, MILLISECONDS);
    pipe.addBefore(selfName, IDLE_HANDLER_NAME, result);
    return result;
  }

  private static MonoHandler addMonoHandler(final String selfName, final ChannelPipeline pipe) {
    @Nullable final ChannelHandler monoHandler = pipe.get(MonoHandler.class);
    if (monoHandler != null) {
      throw new RuntimeException(
          String.format("%s %s is already in the %s", MonoHandler.class.getSimpleName(), monoHandler, pipe));
    }
    final MonoHandler result = new MonoHandler();
    pipe.addBefore(selfName, MONO_HANDLER_NAME, result);
    return result;
  }

  private static final void release(@Nullable Object o) {
    if (o instanceof ReferenceCounted) {
      final ReferenceCounted rc = ((ReferenceCounted)o);
      if (rc.refCnt() > 0) {
        rc.release();
      }
    }
  }
}