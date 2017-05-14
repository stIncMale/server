package stinc.male.server.netty4.tcp.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.server.netty4.util.channel.ChannelUtil;
import stinc.male.server.netty4.tcp.http.util.http.HttpUtil;

@ThreadSafe
@ChannelHandler.Sharable
public final class ClientAddressMdcHandler extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(ClientAddressMdcHandler.class);

  @Override
  public final void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    try {
      final String keyClientAddress = "clientAddress";
      final String unknownClientAddress = "<unknown client address>";
      MDC.remove(keyClientAddress);
      if (msg instanceof HttpRequest) {
        MDC.put(keyClientAddress, HttpUtil.getRemoteAddress(((HttpRequest) msg).headers(), ctx.channel())
            .orElse(unknownClientAddress));
      } else {
        MDC.put(keyClientAddress, ChannelUtil.getRemoteAddress(ctx.channel())
            .orElse(unknownClientAddress));
      }
    } catch (final RuntimeException e) {
      logger.error("Failed to add useful information to MDC", e);
    }
    super.channelRead(ctx, msg);
  }
}