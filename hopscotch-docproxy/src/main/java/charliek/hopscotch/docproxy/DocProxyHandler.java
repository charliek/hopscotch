package charliek.hopscotch.docproxy;

import com.google.common.base.Preconditions;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class DocProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {
	private static Logger LOG = LoggerFactory.getLogger(DocProxyHandler.class);

	private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
	private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
	private static final AsciiString CONNECTION = new AsciiString("Connection");
	private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

	private final S3Service s3Service;
	private final Config config;

	public DocProxyHandler(S3Service s3Service, Config config) {
		this.s3Service = s3Service;
		this.config = config;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	public String getBucket(HttpRequest req) {
		String host = req.headers().get(HOST);
		Preconditions.checkNotNull(host, "Host header is required");
		return config.getBucket();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (HttpHeaderUtil.is100ContinueExpected(req)) {
			ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
		}
		HttpMethod method = req.method();
		if (!method.equals(HttpMethod.GET)) {
			renderError(ctx, req, new IllegalArgumentException("Invalid host name"));
			return;
		}
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
		String path = queryStringDecoder.path();

		s3Service.getRenderObject(getBucket(req), pathToS3Path(path))
			.subscribeOn(new EventLoopScheduler(ctx))
			.flatMap(renderObject -> renderResponse(ctx, req, renderObject))
			.doOnError(t -> renderError(ctx, req, t))
			.subscribe();
	}

	static void render(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
		boolean keepAlive = HttpHeaderUtil.isKeepAlive(req);
		if (!keepAlive) {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			response.headers().set(CONNECTION, KEEP_ALIVE);
			ctx.writeAndFlush(response);
		}
	}

	static Observable<String> renderResponse(ChannelHandlerContext ctx, HttpRequest req, RenderObject renderObject) {
		LOG.error("Rendering response for path");
		byte[] bytes = renderObject.getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, renderObject.getStatus(), Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, renderObject.getContentType());
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
		return Observable.just("rendered");
	}


	static Observable<String> renderError(ChannelHandlerContext ctx, HttpRequest req, Throwable t) {
		LOG.error("Rendering error page due to error", t);
		byte[] bytes = "Error fetching content".getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, "text/plain");
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
		return Observable.just("errored");
	}

	static String pathToS3Path(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
