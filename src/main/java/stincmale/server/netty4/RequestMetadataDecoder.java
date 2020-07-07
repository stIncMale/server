package stincmale.server.netty4;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * A {@link MessageToMessageDecoder} that wraps messages of type {@code RQ} into messages of type
 * {@link RequestWithMetadata}{@code <RQ>}.
 *
 * @param <RQ> A type of the request this {@link io.netty.channel.ChannelInboundHandlerAdapter} expects.
 */
@Sharable
@ThreadSafe
public abstract class RequestMetadataDecoder<RQ> extends MessageToMessageDecoder<RQ> {
  protected RequestMetadataDecoder() {
  }

  /**
   * @param request Note that {@linkplain ReferenceCountUtil#retain(java.lang.Object)} is called for the {@code msg}.
   * This method automatically calls {@link ReferenceCounted#retain() retain} on the {@code request}.
   */
  @Override
  protected final void decode(final ChannelHandlerContext ctx, final RQ request, final List<Object> out) throws Exception {
    final Metadata metadata = createMetadata(ctx, request);
    final RequestWithMetadata<RQ> requestWithMetadata = new RequestWithMetadata<>(request, metadata);
    out.add(requestWithMetadata);
  }

  /**
   * Is called from {@link #decode(ChannelHandlerContext, Object, List)}.
   */
  protected abstract Metadata createMetadata(ChannelHandlerContext ctx, RQ request);
}
