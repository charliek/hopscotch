package charliek.hopscotch.docproxy.handlers;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.S3Host;
import charliek.hopscotch.docproxy.rx.EventLoopScheduler;
import charliek.hopscotch.docproxy.services.GithubService;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.ReferenceCountUtil;
import rx.Observable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GithubAuthHandler extends ChannelInboundHandlerAdapter {

	private static final String SCOPE = "read:org";
	public static final String AUTH_COOKIE = "github_token";
	public static final String AUTH_PATH = "/auth/register";
	private static final CharMatcher hexMatcher = CharMatcher.anyOf("0123456789abcdef");

	private final AppConfig appConfig;
	private final GithubService githubService;

	public GithubAuthHandler(GithubService githubService, AppConfig appConfig) {
		this.githubService = githubService;
		this.appConfig = appConfig;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FullHttpRequest request = (FullHttpRequest) msg;
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
		String path = queryStringDecoder.path();
		if (path.equals(GithubAuthHandler.AUTH_PATH)) {
			handleAuthPath(ctx, request);
		} else {
			authenticate(ctx, request);
		}
	}

	private void authenticate(ChannelHandlerContext ctx, FullHttpRequest request) {
		Optional<Cookie> cookie = getAuthCookie(request);
		if (isAuthPath(request) || isValidUser(cookie)) {
			// if the user is authenticated we pass on the message for downstream handlers
			ctx.fireChannelRead(request);
		} else {
			String host = request.headers().get(HOST);
			Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
			if (!s3Host.isPresent()) {
				DocProxyHandler.renderError(ctx, request, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
				ReferenceCountUtil.release(request);
				return;
			}
			// if the user isn't authenticated we send them to github for auth
			String redirectUrl = String.format("https://github.com/login/oauth/authorize?client_id=%s&scope=%s", s3Host.get().getClientId(), SCOPE);
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
			response.headers().set(LOCATION, redirectUrl);
			ctx.writeAndFlush(response);
			ReferenceCountUtil.release(request);
		}
	}

	private void handleAuthPath(ChannelHandlerContext ctx, FullHttpRequest req) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
		List<String> params = queryStringDecoder.parameters().get("code");
		String host = req.headers().get(HOST);
		Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
		if (!s3Host.isPresent()) {
			DocProxyHandler.renderError(ctx, req, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
			ReferenceCountUtil.release(req);
			return;
		}
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
						io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT.encode(cookie));
					DocProxyHandler.render(ctx, req, response);
					ReferenceCountUtil.release(req);
					return Observable.empty();
				})
				.doOnError(t -> DocProxyHandler.renderError(ctx, req, t))
				.subscribe();
		} else {
			DocProxyHandler.renderError(ctx, req, new IllegalArgumentException("Missing code param"));
			ReferenceCountUtil.release(req);
		}
	}

	private boolean isAuthPath(FullHttpRequest request) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
		String path = queryStringDecoder.path();
		return path.equals(AUTH_PATH);
	}

	private boolean isValidUser(Optional<Cookie> cookie) {
		if (cookie.isPresent()) {
			String value = cookie.get().value();
			return value.length() == 40 && hexMatcher.matchesAllOf(value);
		}
		return false;
	}

	private Optional<Cookie> getAuthCookie(FullHttpRequest request) {
		Optional<Cookie> tokenValue = Optional.empty();
		String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
		if (cookieString != null) {
			Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
			tokenValue = cookies.stream()
				.filter(c -> c.name().equals(AUTH_COOKIE))
				.findFirst();
		}
		return tokenValue;
	}
}
