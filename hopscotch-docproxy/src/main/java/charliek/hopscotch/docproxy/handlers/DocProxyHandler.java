package charliek.hopscotch.docproxy.handlers;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.RenderObject;
import charliek.hopscotch.docproxy.dto.S3Host;
import charliek.hopscotch.docproxy.rx.EventLoopScheduler;
import charliek.hopscotch.docproxy.services.GithubService;
import charliek.hopscotch.docproxy.services.S3Service;
import com.google.common.base.Preconditions;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class DocProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {
	private static Logger LOG = LoggerFactory.getLogger(DocProxyHandler.class);

	private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
	private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
	private static final AsciiString CONNECTION = new AsciiString("Connection");
	private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

	private final S3Service s3Service;
	private final GithubService githubService;
	private final AppConfig appConfig;

	public DocProxyHandler(S3Service s3Service, GithubService githubService, AppConfig appConfig) {
		this.s3Service = s3Service;
		this.githubService = githubService;
		this.appConfig = appConfig;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (HttpHeaderUtil.is100ContinueExpected(req)) {
			ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
		}
		HttpMethod method = req.method();
		if (!method.equals(HttpMethod.GET)) {
			renderError(ctx, req, new IllegalArgumentException("Only get requests supported"));
			return;
		}
		String host = req.headers().get(HOST);
		Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
		if (!s3Host.isPresent()) {
			renderError(ctx, req, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
			return;
		}
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
		String path = queryStringDecoder.path();

		if (path.equals(GithubAuthHandler.AUTH_PATH)) {
			List<String> params = queryStringDecoder.parameters().get("code");
			if (params.size() == 1) {
				String code = params.get(0);
				githubService.authRequest(code, s3Host.get())
					.subscribeOn(new EventLoopScheduler(ctx))
					.flatMap(token -> {
						Preconditions.checkNotNull(token);
						FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
						response.headers().set(LOCATION, "/test.html");
						response.headers().set(CONTENT_LENGTH, 0);
						response.headers().set(CONTENT_TYPE, "text/html");
						DefaultCookie cookie = new DefaultCookie(GithubAuthHandler.AUTH_COOKIE, token);
						cookie.setHttpOnly(true);
						cookie.setMaxAge(Duration.ofDays(14).getSeconds());
						cookie.setPath("/");
						response.headers().add("Set-Cookie",
							ServerCookieEncoder.STRICT.encode(cookie));
						render(ctx, req, response);
						return Observable.empty();
					})
					.doOnError(t -> renderError(ctx, req, t))
					.subscribe();
			} else {
				renderError(ctx, req, new IllegalArgumentException("Missing code param"));
			}
		} else {
			s3Service.getRenderObject(s3Host.get().getBucket(), pathToS3Path(path))
				.subscribeOn(new EventLoopScheduler(ctx))
				.flatMap(renderObject -> renderResponse(ctx, req, renderObject))
				.doOnError(t -> renderError(ctx, req, t))
				.subscribe();
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

	static Observable<String> renderResponse(ChannelHandlerContext ctx, HttpRequest req, RenderObject renderObject) {
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
