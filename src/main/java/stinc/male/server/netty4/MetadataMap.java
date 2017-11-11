package stinc.male.server.netty4;

import io.netty.util.ReferenceCounted;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@NotThreadSafe
public final class MetadataMap implements Metadata {
  @Nullable
  private Map<MetadataKey<?>, Object> metadata;

  public MetadataMap() {
  }

  /**
   * @param value Must not be of type {@link ReferenceCounted}.
   */
  public final <T> MetadataMap set(final MetadataKey<T> key, final T value) {
    checkNotNull(key, "The argument %s must not be null", "key");
    checkNotNull(value, "The argument %s must not be null", "value");
    checkArgument(!(value instanceof ReferenceCounted),
        "The argument %s must not be of type %s", "value", ReferenceCounted.class.getSimpleName());
    if (metadata == null) {
      metadata = new HashMap<>();
    }
    metadata.put(key, value);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final <T> Optional<T> get(final MetadataKey<T> key) {
    checkNotNull(key, "The argument %s must not be null", "key");
    return metadata == null
        ? Optional.empty()
        : Optional.ofNullable((T)metadata.get(key));
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(metadata=" + metadata
        + ')';
  }
}