package charliek.hopscotch.docproxy;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class DocProxyHandler extends ChannelInboundHandlerAdapter {
	private static Logger LOG = LoggerFactory.getLogger(DocProxyHandler.class);
	private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
	private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
	private static final AsciiString CONNECTION = new AsciiString("Connection");
	private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

	private final EventExecutorGroup eventExecutors;
	private final AmazonS3 s3client;
	private final Config config;

	public DocProxyHandler(EventExecutorGroup eventExecutors, Config config) {
		this.eventExecutors = eventExecutors;
		this.s3client = new AmazonS3Client();
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
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) msg;
			if (HttpHeaderUtil.is100ContinueExpected(req)) {
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
			}
			HttpMethod method = req.method();
			if (!method.equals(HttpMethod.GET)) {
				renderError(ctx, req);
				return;
			}
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
			String path = queryStringDecoder.path();

			eventExecutors.submit(() -> {
				S3Object object = s3client.getObject(new GetObjectRequest(getBucket(req), pathToS3Path(path)));
				String contentType = object.getObjectMetadata().getContentType();
				InputStream objectData = object.getObjectContent();
				byte[] bytes = ByteStreams.toByteArray(objectData);
				objectData.close();
				return new RenderObject(contentType, bytes);
			}).addListener((Future<RenderObject> f) -> {
				if (f.isSuccess()) {
					renderResponse(ctx, req, f.get());
				} else {
					renderError(ctx, req);
				}
			});
		}
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

	static void renderResponse(ChannelHandlerContext ctx, HttpRequest req, RenderObject renderObject) {
		byte[] bytes = renderObject.getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, renderObject.getContentType());
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
	}


	static void renderError(ChannelHandlerContext ctx, HttpRequest req) {
		byte[] bytes = "Error fetching content".getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, "text/plain");
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		render(ctx, req, response);
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
