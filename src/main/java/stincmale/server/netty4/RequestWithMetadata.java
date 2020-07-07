package stincmale.server.netty4;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import javax.annotation.Nullable;
import stincmale.server.netty4.tcp.DispatchMonoHandler;
import javax.annotation.concurrent.NotThreadSafe;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows to attach any data to the actual request before feeding it to the
 * {@link DispatchMonoHandler}, or for any other purpose.
 *
 * @param <RQ> A type of the actual request
 */
@NotThreadSafe
public final class RequestWithMetadata<RQ> implements ReferenceCounted {
  private final RQ request;
  private final Metadata metadata;

  /**
   * Constructs a new {@link RequestWithMetadata}.
   * <p>
   * Note that {@link RequestWithMetadata} is just a wrapper around the {@code request},
   * so all {@link ReferenceCounted} methods are translated to the {@code request}.
   */
  public RequestWithMetadata(final RQ request, final Metadata metadata) {
    this.request = checkNotNull(request, "The argument %s must not be null", "request");
    this.metadata = checkNotNull(metadata, "The argument %s must not be null", "metadata");
  }

  public RequestWithMetadata(final RQ request) {
    this(request, new MetadataMap());
  }

  public final RQ request() {
    return request;
  }

  public final Metadata metadata() {
    return metadata;
  }

  @Override
  public final int refCnt() {
    return request instanceof ReferenceCounted
        ? ((ReferenceCounted)request).refCnt()
        : 1;
  }

  @Override
  public final RequestWithMetadata<RQ> retain() {
    ReferenceCountUtil.retain(request);
    return this;
  }

  @Override
  public final RequestWithMetadata<RQ> retain(final int increment) {
    checkArgument(increment > 0, "The argument %s must be positive", "increment");
    ReferenceCountUtil.retain(request, increment);
    return this;
  }

  @Override
  public final ReferenceCounted touch() {
    ReferenceCountUtil.touch(request);
    return this;
  }

  @Override
  public final ReferenceCounted touch(@Nullable final Object hint) {
    ReferenceCountUtil.touch(request, hint);
    return this;
  }

  @Override
  public final boolean release() {
    return ReferenceCountUtil.release(request);
  }

  @Override
  public final boolean release(final int decrement) {
    checkArgument(decrement > 0, "The argument %s must be positive", "decrement");
    return ReferenceCountUtil.release(request, decrement);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(request=" + request
        + ", metadata=" + metadata
        + ')';
  }
}
