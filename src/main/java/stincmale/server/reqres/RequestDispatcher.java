package stincmale.server.reqres;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A special {@link RequestProcessor} that doesn't perform processing by itself
 * but rather delegates it to a more specific {@link RequestProcessor}.
 *
 * @param <RQ> A type of the request this {@link RequestDispatcher} {@linkplain #process(java.lang.Object) dispatches}.
 * @param <RS> A type of the response.
 */
@ThreadSafe
public interface RequestDispatcher<RQ, RS> extends RequestProcessor<RQ, RS> {
}
