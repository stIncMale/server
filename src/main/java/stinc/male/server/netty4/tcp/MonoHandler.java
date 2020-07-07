package stinc.male.server.netty4.tcp;

import static com.google.common.base.Preconditions.checkState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import java.util.LinkedList;
import java.util.Queue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.server.reqres.RequestDispatcher;

/**
 * The purpose of this {@link ChannelDuplexHandler} is to allow asynchronous processing of inbound messages
 * while preserving the order of outbound response messages (which must correlate to the order of inbound messages)
 * and allowing a client to send a next request before receiving an answer to the previous request.
 * If you want to use the {@link RequestDispatcher} functionality, then use this handler indirectly via {@link DispatchMonoHandler}.
 * <p>
 * Disables {@linkplain ChannelConfig#setAutoRead(boolean) auto read} and controls read operations by itself
 * (auto read is disabled in {@link #channelRegistered(ChannelHandlerContext)} and is enabled back again in
 * {@link #handlerRemoved(ChannelHandlerContext)} if it was enabled before disabling).
 * This handler must be placed in the {@link ChannelPipeline} above (after in the inbound/upstream evaluation order) any decoders,
 * and above (before in the outbound/downstream evaluation order) any encoders:
 * <pre>{@code
 *  ChannelPipeline p = ...;
 *  ...
 *  p.addLast(new MyDecoder());
 *  ...
 *  p.addLast(new MyEncoder());
 *  ...
 *  p.addLast(new MonoHandler());
 *  ...
 *  p.addLast(new MyBusinessLogicHandler());
 *  ...
 *  p.addLast(new MyExceptionHandler());
 *  ...
 * }</pre>
 * and must not be added to the {@link ChannelPipeline} more than once.
 * <p>
 * The handler guarantees that at any given moment not more than one decoded message is processed by
 * {@link ChannelInboundHandler}s which are above (after) this handler in the {@link ChannelPipeline}.
 * In order this handler to function properly there must be a single outbound message
 * {@linkplain ChannelHandlerContext#write(java.lang.Object) written} per each decoded inbound message that was
 * {@linkplain ChannelHandlerContext#fireChannelRead(java.lang.Object) fired upstream} by this handler,
 * and there must not be any other outbound messages.
 * Such an outbound message may later be encoded to an empty message, or just ignored
 * (one should use the {@link #VOID_OUTBOUND_MESSAGE} to accomplish this).
 *
 * @see DispatchMonoHandler
 */
@NotThreadSafe
public final class MonoHandler extends ChannelDuplexHandler {
  private static final AttributeKey<Boolean> INITIAL_AUTO_READ_ATTR_KEY
      = AttributeKey.valueOf(MonoHandler.class.getSimpleName() + ".initialAutoRead");

  /**
   * This {@link Object} may be used as an empty message when there is no response needed.
   *
   * @see #write(io.netty.channel.ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  static final Object VOID_OUTBOUND_MESSAGE = new Object();

  private final Queue<Object> accumulatedInboundMessages;
  private boolean upstreamOpen;

  public MonoHandler() {
    accumulatedInboundMessages = new LinkedList<>();
    upstreamOpen = true;
  }

  /**
   * Calls {@link ChannelHandlerContext#fireChannelRegistered()} and then {@link ChannelHandlerContext#read()}.
   * Disables {@linkplain ChannelConfig#setAutoRead(boolean) auto read}.
   */
  @Override
  public final void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
    disableAutoRead(ctx.channel());
    ctx.fireChannelRegistered();
    ctx.read();
  }

  @Override
  public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (upstreamOpen) {
      upstreamOpen = false;
      ctx.fireChannelRead(msg);
    } else {
      accumulatedInboundMessages.add(msg);
    }
  }

  @Override
  public final void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
    {
      /* A workaround for the problem described at https://github.com/netty/netty/commit/3e70b4ed99cbd29798a869bb3d7b3415ca8416e0.
       * The problem is that ChannelHandlerContext.read may not produce any message,
       * hence ChannelInboundHandlerAdapter.channelRead may not be called after ChannelHandlerContext.read.
       * In order to work around this problem we have to call ChannelHandlerContext.read again and again
       * until it produces a message and causes ChannelInboundHandlerAdapter.channelRead to be called, which sets upstreamOpen to false.*/
      if (upstreamOpen) {
        ctx.read();
      }
    }
    super.channelReadComplete(ctx);
  }

  /**
   * Calls {@link ChannelHandlerContext#write(java.lang.Object, io.netty.channel.ChannelPromise)}
   * if {@code msg} isn't {@link #VOID_OUTBOUND_MESSAGE}
   * and then either calls {@link ChannelHandlerContext#read()}
   * or {@link ChannelHandlerContext#fireChannelRead(java.lang.Object)}
   * if this handler has any accumulated inbound messages.
   */
  @Override
  public final void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
    try {
      if (msg != VOID_OUTBOUND_MESSAGE) {
        ctx.write(msg, promise);
      }
    } finally {
      @Nullable final Object accumulatedInboundMessage = accumulatedInboundMessages.poll();
      if (accumulatedInboundMessage == null) {
        upstreamOpen = true;
        ctx.read();
      } else {
        ctx.fireChannelRead(accumulatedInboundMessage);
      }
    }
  }

  /**
   * Returns {@linkplain ChannelConfig#setAutoRead(boolean) auto read} back as it was before
   * {@link #channelRegistered(io.netty.channel.ChannelHandlerContext)}.
   */
  @Override
  public final void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
    returnAutoReadBack(ctx);
  }

  private final void disableAutoRead(final Channel channel) {
    checkState(
        channel.attr(INITIAL_AUTO_READ_ATTR_KEY)
            .get() == null,
        "{} was already added to {}",
        getClass().getSimpleName(),
        channel.pipeline());
    channel.attr(INITIAL_AUTO_READ_ATTR_KEY)
        .set(channel.config()
            .isAutoRead());
    channel.config()
        .setAutoRead(false);
  }

  private final void returnAutoReadBack(final ChannelHandlerContext ctx) {
    @Nullable
    Channel channel = ctx.channel();
    if (channel != null) {
      @Nullable final Boolean initialAutoRead = channel.attr(INITIAL_AUTO_READ_ATTR_KEY)
          .get();
      checkState(initialAutoRead != null,
          "Internal error, there is no %s attribute in the %s", INITIAL_AUTO_READ_ATTR_KEY, channel);
      channel.config()
          .setAutoRead(initialAutoRead);
    }
  }
}