package stincmale.server;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This {@link RuntimeException} is intended to provide a error message
 * that may be made visible outside the application.
 */
public class ExternallyVisibleException extends RuntimeException {
  private static final long serialVersionUID = 0;

  /**
   * Message that may be made visible outside the application.
   */
  private final String externalMessage;

  /**
   * @param externalMessage Message that may be made visible outside the application.
   */
  public ExternallyVisibleException(final String externalMessage) {
    super(externalMessage);
    checkNotNull(externalMessage, "The argument %s must not be null", "externalMessage");
    this.externalMessage = externalMessage;
  }

  /**
   * @param message The detail message which is saved for later retrieval by the {@link #getMessage()} method.
   * @param externalMessage Message that may be made visible outside the application.
   */
  public ExternallyVisibleException(@Nullable final String message, final String externalMessage) {
    super(message);
    checkNotNull(externalMessage, "The argument %s must not be null", "externalMessage");
    this.externalMessage = externalMessage;
  }

  /**
   * @param message The detail message which is saved for later retrieval by the {@link #getMessage()} method.
   * @param cause The cause which is saved for later retrieval by the {@link #getCause()} method.
   * @param externalMessage Message that may be made visible outside the application.
   */
  public ExternallyVisibleException(@Nullable final String message, @Nullable final Throwable cause, final String externalMessage) {
    super(message, cause);
    checkNotNull(externalMessage, "The argument %s must not be null", "externalMessage");
    this.externalMessage = externalMessage;
  }

  /**
   * @param externalMessage Message that may be made visible outside the application.
   * @param cause The cause which is saved for later retrieval by the {@link #getCause()} method.
   */
  public ExternallyVisibleException(final String externalMessage, @Nullable final Throwable cause) {
    super(externalMessage, cause);
    checkNotNull(externalMessage, "The argument %s must not be null", "externalMessage");
    this.externalMessage = externalMessage;
  }

  /**
   * @return Message that may be made visible outside the application.
   */
  public final String getExternalMessage() {
    return externalMessage;
  }

  /**
   * A special deserialization method.
   *
   * @throws InvalidObjectException Always.
   */
  private final void readObjectNoData() throws InvalidObjectException {
    throw new InvalidObjectException("Stream data required");
  }

  /**
   * A special serialization method.
   *
   * @param out {@link ObjectOutputStream}.
   *
   * @throws IOException If I/O error occur.
   */
  private final void writeObject(final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * A special deserialization method.
   *
   * @param in {@link ObjectInputStream}.
   *
   * @throws InvalidObjectException If {@link #externalMessage} is {@code null}.
   * @throws ClassNotFoundException If the class of a {@link java.io.Serializable} object could not be found.
   */
  private final void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (externalMessage == null) {
      throw new InvalidObjectException("External message must not be null");
    }
  }
}
