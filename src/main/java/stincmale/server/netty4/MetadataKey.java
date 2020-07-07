package stincmale.server.netty4;

import javax.annotation.concurrent.Immutable;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key that is used to store and retrieve data in the {@link RequestWithMetadata}.
 *
 * @param <T> A type of data that can be stored/retrieved by using the {@link MetadataKey}.
 */
@Immutable
public final class MetadataKey<T> {
  private final String id;

  /**
   * @param <T> A type of data that can be stored/retrieved by using the {@link MetadataKey}.
   * @param id Identifier of the {@link MetadataKey}.
   */
  public static final <T> MetadataKey<T> of(final String id) {
    checkNotNull(id, "The argument %s must not be null", "id");
    return new MetadataKey<>(id);
  }

  /**
   * @param id Identifier of the {@link MetadataKey}.
   */
  private MetadataKey(final String id) {
    this.id = id;
  }

  @Override
  public final boolean equals(final Object o) {
    final boolean result;
    if (this == o) {
      result = true;
    } else if (o instanceof MetadataKey) {
      final MetadataKey<?> that = (MetadataKey<?>)o;
      result = id.equals(that.id);
    } else {
      result = false;
    }
    return result;
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }

  @Override
  public final String toString() {
    return id;
  }
}
