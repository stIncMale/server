package stincmale.server.util.throwable;

import javax.annotation.Nullable;
import java.util.Optional;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ThrowableUtil {
  /**
   * Extracts a {@link Throwable} of type {@code extractType} from {@code t}
   * and its {@linkplain Throwable#getCause() cause} chain.
   * This method returns empty {@link Optional} if {@code t} isn't of type
   * {@code extractType} and there is no {@code extractType} among its
   * {@linkplain Throwable#getSuppressed() suppressed} {@link Throwable}s and causes.
   * If there are multiple causes of type {@code extractType} among
   * {@linkplain Throwable#getSuppressed() suppressed} {@link Throwable}s or in the cause chain
   * then the closest one to {@code t} in the cause chain is returned.
   *
   * @param <T> Type of a {@link Throwable} to extract.
   * @param t {@link Throwable} from which to extract a {@link Throwable} of type {@code extractType}.
   * @param extractType An instance of {@link Class} that represents type of a {@link Throwable} to extract.
   *
   * @return {@link Optional} with extracted {@link Throwable} of type {@code extractType} or {@code null}.
   */
  public static final <T extends Throwable> Optional<T> extract(@Nullable Throwable t, final Class<T> extractType) {
    checkNotNull(extractType, "The argument %s must not be null", "extract");
    @Nullable
    T result = null;
    if (t != null) {
      do {
        if (extractType.isInstance(t)) {
          result = extractType.cast(t);
          break;
        } else {
          for (final Throwable suppressed : t.getSuppressed()) {
            if (extractType.isInstance(suppressed)) {
              result = extractType.cast(suppressed);
              break;
            }
          }
          t = t.getCause();
        }
      } while (t != null);
    }
    return Optional.ofNullable(result);
  }

  private ThrowableUtil() {
    throw new UnsupportedOperationException("This class is not designed to be instantiated");
  }
}
