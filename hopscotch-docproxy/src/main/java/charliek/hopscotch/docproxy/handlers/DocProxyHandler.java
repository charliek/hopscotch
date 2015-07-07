package charliek.hopscotch.docproxy.handlers;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.S3Host;
import charliek.hopscotch.docproxy.rx.EventLoopScheduler;
import charliek.hopscotch.docproxy.services.S3Service;
import charliek.hopscotch.docproxy.utils.RenderUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class DocProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {
	private static Logger LOG = LoggerFactory.getLogger(DocProxyHandler.class);

	private final S3Service s3Service;
	private final AppConfig appConfig;

	public DocProxyHandler(S3Service s3Service, AppConfig appConfig) {
		this.s3Service = s3Service;
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
			RenderUtils.renderError(ctx, req, new IllegalArgumentException("Only get requests supported"));
			return;
		}
		String host = req.headers().get(HOST);
		Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
		if (!s3Host.isPresent()) {
			RenderUtils.renderError(ctx, req, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
			return;
		}
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
		String path = queryStringDecoder.path();
		s3Service.getRenderObject(s3Host.get().getBucket(), pathToS3Path(path))
			.subscribeOn(new EventLoopScheduler(ctx))
			.flatMap(renderObject -> RenderUtils.renderResponse(ctx, req, renderObject))
			.doOnError(t -> RenderUtils.renderError(ctx, req, t))
			.subscribe();
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
