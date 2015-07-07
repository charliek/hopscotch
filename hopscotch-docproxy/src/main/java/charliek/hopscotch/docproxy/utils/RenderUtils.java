package charliek.hopscotch.docproxy.utils;

import charliek.hopscotch.docproxy.dto.RenderObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class RenderUtils {
	private static Logger LOG = LoggerFactory.getLogger(RenderUtils.class);

	private RenderUtils() {
		// only static
	}

	public static Observable<String> renderResponse(ChannelHandlerContext ctx, HttpRequest req, RenderObject renderObject) {
		byte[] bytes = renderObject.getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, renderObject.getStatus(), Unpooled.wrappedBuffer(bytes));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, renderObject.getContentType());
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
		return Observable.just("rendered");
	}

	public static Observable<String> renderError(ChannelHandlerContext ctx, HttpRequest req, Throwable t) {
		LOG.error("Rendering error page due to error", t);
		byte[] bytes = "Error fetching content".getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, "text/plain");
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
		return Observable.just("errored");
	}

	public static void render(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
		boolean keepAlive = HttpHeaderUtil.isKeepAlive(req);
		if (!keepAlive) {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			response.headers().set(CONNECTION, KEEP_ALIVE);
			ctx.writeAndFlush(response);
		}
	}
}
