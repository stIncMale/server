package stinc.male.server.netty4;

import java.util.Optional;

/**
 * Just a typed key-value representation of data.
 */
public interface Metadata {
  <T> Optional<T> get(MetadataKey<T> key);
}