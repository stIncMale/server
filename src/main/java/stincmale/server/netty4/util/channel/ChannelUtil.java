package stincmale.server.netty4.util.channel;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import javax.annotation.Nullable;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides useful utility methods.
 */
public final class ChannelUtil {
  public static final Optional<String> getRemoteAddress(final Channel channel) {
    checkNotNull(channel, "The argument %s must not be null", "channel");
    @Nullable final String result;
    @Nullable final SocketAddress socketAddress = channel.remoteAddress();
    if (socketAddress instanceof InetSocketAddress) {
      result = ((InetSocketAddress)socketAddress).getAddress()
          .getHostAddress();
    } else {
      result = null;
    }
    return Optional.ofNullable(result);
  }

  private ChannelUtil() {
    throw new UnsupportedOperationException("This class is not designed to be instantiated");
  }
}
