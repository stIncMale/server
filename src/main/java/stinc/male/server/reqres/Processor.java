package stinc.male.server.reqres;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import stinc.male.server.Server;
import stinc.male.server.netty4.tcp.http.SimpleHttpRequestDispatcherByUrl;
import stinc.male.server.reqres.spring.SpringRequestDispatcher;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import stinc.male.server.reqres.spring.http.SimpleSpringHttpRequestDispatcherByUrl;

/**
 * {@link RequestProcessor}s may be annotated with this annotation in order to be automatically detected.
 * The <a href="{@docRoot}/index.html">{@code stinc.male.server}</a> module implements automatic detection via {@link SpringRequestDispatcher},
 * other mechanisms may be implemented by a user.
 * {@link RequestDispatcher} must not be annotated with this annotation.
 *
 * @see SpringRequestDispatcher
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface Processor {
  /**
   * @return The name of a {@link RequestProcessor} which is unique within a given {@link Server}.
   * The semantics of the name depends on the {@link RequestDispatcher} implementation.
   * For example, {@link RequestDispatcherByProcessorName} simply {@linkplain RequestDispatcherByProcessorName#getProcessorName(Object) extracts}
   * the name from a request and delegates processing to a {@link RequestProcessor} with this name;
   * {@link SimpleHttpRequestDispatcherByUrl} is a {@link RequestDispatcherByProcessorName}
   * that uses URI path from a request as the name of a {@link RequestProcessor}.
   *
   * @see RequestDispatcherByProcessorName
   * @see SimpleHttpRequestDispatcherByUrl
   * @see SimpleSpringHttpRequestDispatcherByUrl
   */
  String value();
}