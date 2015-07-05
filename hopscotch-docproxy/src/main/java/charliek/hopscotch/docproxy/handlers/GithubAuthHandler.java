package charliek.hopscotch.docproxy.handlers;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.S3Host;
import com.google.common.base.CharMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.ReferenceCountUtil;

import java.util.Optional;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class GithubAuthHandler extends ChannelInboundHandlerAdapter {

	private static final String SCOPE = "read:org";
	public static final String AUTH_COOKIE = "github_token";
	public static final String AUTH_PATH = "/auth/register";
	private static final CharMatcher hexMatcher = CharMatcher.anyOf("0123456789abcdef");

	private final AppConfig appConfig;

	public GithubAuthHandler(AppConfig appConfig) {
		this.appConfig = appConfig;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FullHttpRequest request = (FullHttpRequest) msg;

		Optional<Cookie> cookie = getAuthCookie(request);
		if (isAuthPath(request) || isValidUser(cookie)) {
			// if the user is authenticated we pass on the message for downstream handlers
			ctx.fireChannelRead(msg);
		} else {
			String host = request.headers().get(HOST);
			Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
			if (!s3Host.isPresent()) {
				DocProxyHandler.renderError(ctx, request, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
				ReferenceCountUtil.release(msg);
				return;
			}
			// if the user isn't authenticated we send them to github for auth
			String redirectUrl = String.format("https://github.com/login/oauth/authorize?client_id=%s&scope=%s", s3Host.get().getClientId(), SCOPE);
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
			response.headers().set(LOCATION, redirectUrl);
			ctx.writeAndFlush(response);
			ReferenceCountUtil.release(msg);
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
