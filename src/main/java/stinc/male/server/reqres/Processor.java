package stinc.male.server.reqres;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import stinc.male.server.reqres.spring.SpringRequestDispatcher;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * In some cases {@link RequestProcessor}s must be annotated with this annotation in order to be automatically detected.
 * {@link RequestDispatcher} must not be annotated with this annotation.
 *
 * @see SpringRequestDispatcher
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface Processor {
  /**
   * @return Unique within a server name of the {@link RequestDispatcherByProcessorName}.
   */
  String value();
}