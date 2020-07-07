package stincmale.server.netty4.tcp.http;

import stincmale.server.reqres.RequestProcessor;
import stincmale.server.netty4.RequestWithMetadata;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * {@link RequestProcessor} that expects {@link RequestWithMetadata}{@code <? extends }{@link FullHttpRequest}{@code >}
 * requests and produces {@link FullHttpResponse}s.
 */
public interface HttpRequestProcessor extends RequestProcessor<RequestWithMetadata<? extends FullHttpRequest>, FullHttpResponse> {
}
