package stinc.male.server.netty4.tcp;

import stinc.male.server.reqres.RequestDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.Test;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class TestDispatchMonoHandler {
  private static final class DHandler extends DispatchMonoHandler<Object, Object> {
    private static final Object FAILURE_RESPONSE = new Object();
    private static final Object CLOSE_REQUEST = new Object();

    private DHandler() {
      super(new Dispatcher(), -1);
    }

    @Override
    @Nullable
    protected final Object failureResponse(final Object request, final Throwable failure) {
      return FAILURE_RESPONSE;
    }

    @Override
    protected final boolean closeChannelAfterResponse(
        @Nullable final Object request, final @Nullable Object response, final @Nullable Throwable failure) {
      return request == CLOSE_REQUEST;
    }
  }

  private static final class Dispatcher implements RequestDispatcher<Object, Object> {
    private static final Object FAILURE_REQUEST = new Object();

    private Dispatcher() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public final CompletionStage<Object> process(final Object request) {
      final CompletionStage<Object> result;
      if (request instanceof CompletionStage) {
        result = (CompletionStage<Object>) request;
      } else if (request == FAILURE_REQUEST) {
        throw new RuntimeException();
      } else {
        result = CompletableFuture.completedFuture(request);
      }
      return result;
    }
  }

  public TestDispatchMonoHandler() {
  }

  @Test
  public final void channelRegistered() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), 1));
    assertTrue(testChannel.pipeline().first() instanceof IdleStateHandler);
    assertNotNull(testChannel.pipeline().get(MonoHandler.class));
    assertFalse(testChannel.config().isAutoRead());
  }

  @Test
  public final void requestRelease() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), 0));
    final ByteBuf msg = Unpooled.buffer();
    assertEquals(1, msg.refCnt());
    testChannel.writeInbound(msg);
    assertEquals(0, msg.refCnt());
  }

  @Test
  public final void idleTimeout() throws Exception {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), 5));
    testChannel.writeInbound(new Object());
    Thread.sleep(10L);//bad practice, but I believe sometimes acceptable in tests...
    testChannel.runScheduledPendingTasks();
    assertFalse(testChannel.isActive());
  }

  @Test
  public final void idleTimeoutInstantly() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), 0));
    testChannel.writeInbound(new Object());
    assertFalse(testChannel.isActive());
  }

  @Test
  public final void idleTimeoutDisabled() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), -1));
    testChannel.writeInbound(new Object());
    assertTrue(testChannel.isActive());
    assertNull(testChannel.pipeline().get(IdleStateHandler.class));
  }

  @Test
  public final void order1() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), -1));
    final CompletableFuture<Object> msg1 = new CompletableFuture<>();
    final Object msg2 = new Object();
    testChannel.writeInbound(msg1, msg2);
    assertNull(testChannel.readOutbound());
    msg1.complete(null);
    assertSame(msg2, testChannel.readOutbound());
  }

  @Test
  public final void order2() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), -1));
    final CompletableFuture<Object> msg1 = new CompletableFuture<>();
    final Object resp1 = new Object();
    final Object msg2 = new Object();
    testChannel.writeInbound(msg1, msg2);
    assertNull(testChannel.readOutbound());
    msg1.complete(resp1);
    assertSame(resp1, testChannel.readOutbound());
    assertSame(msg2, testChannel.readOutbound());
  }

  @Test
  public final void order3() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DispatchMonoHandler<>(new Dispatcher(), -1));
    final CompletableFuture<Object> msg1 = new CompletableFuture<>();
    final Object resp1 = new Object();
    final CompletableFuture<Object> msg2 = new CompletableFuture<>();
    final Object resp2 = new Object();
    testChannel.writeInbound(msg1, msg2);
    assertNull(testChannel.readOutbound());
    msg2.complete(resp2);
    assertNull(testChannel.readOutbound());
    msg1.complete(resp1);
    assertSame(resp1, testChannel.readOutbound());
    assertSame(resp2, testChannel.readOutbound());
  }

  @Test
  public final void failureResponse1() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DHandler());
    testChannel.writeInbound(Dispatcher.FAILURE_REQUEST);
    assertSame(DHandler.FAILURE_RESPONSE, testChannel.readOutbound());
    final Object msg = new Object();
    testChannel.writeInbound(msg);
    assertSame(msg, testChannel.readOutbound());
  }

  @Test
  public final void failureResponse2() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DHandler());
    final CompletableFuture<Object> msg1 = new CompletableFuture<>();
    msg1.completeExceptionally(new RuntimeException());
    testChannel.writeInbound(msg1);
    assertSame(DHandler.FAILURE_RESPONSE, testChannel.readOutbound());
    final Object msg = new Object();
    testChannel.writeInbound(msg);
    assertSame(msg, testChannel.readOutbound());
  }

  @Test
  public final void close() {
    final EmbeddedChannel testChannel = new EmbeddedChannel(new DHandler());
    assertTrue(testChannel.isOpen());
    testChannel.writeInbound(DHandler.CLOSE_REQUEST);
    assertSame(DHandler.CLOSE_REQUEST, testChannel.readOutbound());
    assertFalse(testChannel.isOpen());
  }
}