package stinc.male.server.netty4.tcp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public final class TestMonoHandler {
  private static final class BusinessLogicHandler extends SimpleChannelInboundHandler<Object> {
    private static final Object MESSAGE_WITHOUT_RESPONSE = new Object();

    private BusinessLogicHandler() {
    }

    @Override
    protected final void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
      if (msg == MESSAGE_WITHOUT_RESPONSE) {
        ctx.writeAndFlush(MonoHandler.VOID_OUTBOUND_MESSAGE);
      } else {
        ctx.writeAndFlush(msg);
      }
    }
  }

  public TestMonoHandler() {
  }

  @Test
  public final void autoRead() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new MonoHandler());
    assertFalse(testChannel.config().isAutoRead());
  }

  @Test
  public final void singleRead() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new MonoHandler());
    final Object msg = new Object();
    assertTrue(testChannel.writeInbound(msg));
    assertSame(msg, testChannel.readInbound());
    assertNull(testChannel.readOutbound());
  }

  @Test
  public final void multyRead() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new MonoHandler());
    final Object msg = new Object();
    assertTrue(testChannel.writeInbound(msg, new Object()));
    assertSame(msg, testChannel.readInbound());
    assertNull(testChannel.readInbound());
    assertNull(testChannel.readOutbound());
  }

  @Test
  public final void multyReadWrite() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new MonoHandler(), new BusinessLogicHandler());
    final Object msg1 = new Object();
    final Object msg2 = new Object();
    final Object msg3 = new Object();
    testChannel.writeInbound(
        BusinessLogicHandler.MESSAGE_WITHOUT_RESPONSE,
        msg1,
        msg2,
        BusinessLogicHandler.MESSAGE_WITHOUT_RESPONSE,
        msg3
    );
    assertSame(msg1, testChannel.readOutbound());
    assertSame(msg2, testChannel.readOutbound());
    assertSame(msg3, testChannel.readOutbound());
    assertNull(testChannel.readOutbound());
  }

  @Test
  public final void sequentialReadWrite() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new MonoHandler(), new BusinessLogicHandler());
    final Object msg1 = new Object();
    final Object msg2 = new Object();
    final Object msg3 = new Object();
    testChannel.writeInbound(BusinessLogicHandler.MESSAGE_WITHOUT_RESPONSE);
    assertNull(testChannel.readOutbound());
    testChannel.writeInbound(msg1);
    assertSame(msg1, testChannel.readOutbound());
    testChannel.writeInbound(msg2);
    assertSame(msg2, testChannel.readOutbound());
    testChannel.writeInbound(BusinessLogicHandler.MESSAGE_WITHOUT_RESPONSE);
    assertNull(testChannel.readOutbound());
    testChannel.writeInbound(msg3);
    assertSame(msg3, testChannel.readOutbound());
    assertNull(testChannel.readOutbound());
  }

  @Test
  public final void tmp() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new MonoHandler());
      }
    });
    assertFalse(testChannel.config().isAutoRead());
  }
}