package stinc.male.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import stinc.male.server.netty4.NettyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import org.junit.Test;

public final class TestNettyServer {
  public TestNettyServer() {
  }

  @Test
  public final void startStop() throws Exception {
    final ServerBootstrap sBootstrap = new ServerBootstrap();
    sBootstrap.channel(NioServerSocketChannel.class);
    sBootstrap.group(new NioEventLoopGroup(1));
    sBootstrap.localAddress(new InetSocketAddress("localhost", 22875));
    sBootstrap.handler(new ChannelInitializer<Channel>() {
      @Override
      protected final void initChannel(final Channel channel) throws Exception {
        final ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(new LoggingHandler(LogLevel.DEBUG));
      }
    });
    sBootstrap.childHandler(new ChannelInitializer<Channel>() {
      @Override
      protected final void initChannel(final Channel channel) throws Exception {
        final ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst(new LoggingHandler(LogLevel.DEBUG));
      }
    });
    final NettyServer server = new NettyServer(sBootstrap);
    final Future<Void> futureCompletion = server.start();
    if (futureCompletion.isCancelled()) {
      fail();
    } else if (futureCompletion.isDone()) {
      futureCompletion.get();//an exception will be thrown if futureCompletion is completed exceptionally
    }
    assertFalse(futureCompletion.isDone());
    server.stop();
    assertTrue(futureCompletion.isDone());
  }
}