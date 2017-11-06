package stinc.male.server.netty4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import java.net.SocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stinc.male.server.AbstractServer;
import stinc.male.server.Server;
import stinc.male.server.netty4.tcp.DispatchMonoHandler;
import stinc.male.server.util.logging.TransferableMdc;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <a href="http://netty.io/">Netty</a>-based implementation of {@link Server} interface.
 */
@ThreadSafe
public class NettyServer extends AbstractServer {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

  private final ServerBootstrap sBootstrap;

  /**
   * @param sBootstrap Should be used to specify server options
   * as well as the {@linkplain ServerBootstrap#localAddress(SocketAddress) address to listen}.
   */
  public NettyServer(final ServerBootstrap sBootstrap) {
    checkNotNull(sBootstrap, "The argument %s must not be null", "sBootstrap");
    this.sBootstrap = sBootstrap;
  }

  @Override
  protected void doStart(final CompletableFuture<Void> futureStop) {
    checkNotNull(futureStop, "The argument %s must not be null", "futureStop");
    @Nullable
    ChannelFuture futureBind = null;
    try {
      futureBind = sBootstrap.bind()
          .await();
    } catch (final InterruptedException e) {
      Thread.currentThread()
          .interrupt();
      futureStop.completeExceptionally(e);
    }
    if (futureBind != null) {
      if (futureBind.isSuccess()) {
        final TransferableMdc mdc = TransferableMdc.current();
        final Channel channel = futureBind.channel();
        logger.info("{} is listening to {}", this, channel.localAddress());
        channel.closeFuture()
            .addListener(futureClose -> {
              try (final TransferableMdc mdcTmp = mdc.apply()) {
                if (futureClose.isSuccess()) {
                  futureStop.complete(null);
                } else if (futureClose.isCancelled()) {
                  futureStop.cancel(true);
                } else {
                  futureStop.completeExceptionally(futureClose.cause());
                }
              }
            });
      } else if (futureBind.isCancelled()) {
        futureBind.cancel(true);
      } else {
        futureStop.completeExceptionally(futureBind.cause());
      }
    }
  }

  protected void doStop(final CompletableFuture<Void> futureStop) {
    checkNotNull(futureStop, "The argument %s must not be null", "futureStop");
    final TransferableMdc mdc = TransferableMdc.current();
    CompletableFuture.allOf(
        shutdownEventLoopGroup(sBootstrap.config()
            .group()),
        shutdownEventLoopGroup(sBootstrap.config()
            .childGroup()))
        .whenComplete((nothing, cause) -> {
          try (final TransferableMdc mdcTmp = mdc.apply()) {
            if (cause == null) {
              futureStop.complete(null);
            } else if (cause instanceof CancellationException) {
              futureStop.cancel(true);
            } else {
              futureStop.completeExceptionally(cause);
            }
          }
        });
  }

  private static final CompletableFuture<Void> shutdownEventLoopGroup(@Nullable final EventLoopGroup eventLoopGroup) {
    final CompletableFuture<Void> result = new CompletableFuture<>();
    if (eventLoopGroup == null) {
      result.complete(null);
    } else {
      final TransferableMdc mdc = TransferableMdc.current();
      eventLoopGroup.shutdownGracefully()
          .addListener(futureShutdown -> {
            try (final TransferableMdc mdcTmp = mdc.apply()) {
              if (futureShutdown.isSuccess()) {
                result.complete(null);
              } else if (futureShutdown.isCancelled()) {
                result.cancel(true);
              } else {
                result.completeExceptionally(futureShutdown.cause());
              }
            }
          });
    }
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(sBootstrap=" + sBootstrap
        + ')';
  }

  /**
   * Creates a default {@link ServerBootstrap}.
   * <p>
   * Server (parent) channel options:
   * <ul>
   * <li>{@link ChannelOption#TCP_NODELAY} true</li>
   * <li>{@link ChannelOption#SO_REUSEADDR} true</li>
   * <li>{@link ChannelOption#SO_BACKLOG} 50</li>
   * <li>{@link ChannelOption#ALLOCATOR} {@link UnpooledByteBufAllocator#DEFAULT}</li>
   * </ul>
   * Accepted (child) channel options:
   * <ul>
   * <li>{@link ChannelOption#WRITE_BUFFER_LOW_WATER_MARK} 8KiB</li>
   * <li>{@link ChannelOption#WRITE_BUFFER_HIGH_WATER_MARK} 32KiB</li>
   * <li>{@link ChannelOption#ALLOCATOR} {@link UnpooledByteBufAllocator#DEFAULT}</li>
   * </ul>
   * Note: if you want to use {@link PooledByteBufAllocator}, you have to release {@link ByteBuf} objects in your code.
   * {@link DispatchMonoHandler} releases request after response was sent.
   */
  public static final ServerBootstrap newDefaultSBootstrap() {
    return new ServerBootstrap()
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_BACKLOG, 50)
        .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
        .childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
  }
}