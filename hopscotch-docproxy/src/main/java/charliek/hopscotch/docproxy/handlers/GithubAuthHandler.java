package charliek.hopscotch.docproxy.handlers;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.S3Host;
import charliek.hopscotch.docproxy.exceptions.HopscotchException;
import charliek.hopscotch.docproxy.rx.EventLoopScheduler;
import charliek.hopscotch.docproxy.services.GithubService;
import charliek.hopscotch.docproxy.services.github.AuthCookie;
import charliek.hopscotch.docproxy.services.github.Org;
import charliek.hopscotch.docproxy.services.github.User;
import charliek.hopscotch.docproxy.utils.ObjectMapperBuilder;
import charliek.hopscotch.docproxy.utils.RenderUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
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
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GithubAuthHandler extends ChannelInboundHandlerAdapter {
	private static final ObjectMapper mapper = ObjectMapperBuilder.build();
	private static final String SCOPE = "read:org";
	public static final String AUTH_COOKIE = "github_token";
	public static final String AUTH_PATH = "/auth/register";

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
			authenticateRequest(ctx, request);
		}
	}

	private void authenticateRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
		String host = request.headers().get(HOST);
		Optional<S3Host> s3Host = appConfig.getConfiguredHost(host);
		if (!s3Host.isPresent()) {
			RenderUtils.renderError(ctx, request, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
			ReferenceCountUtil.release(request);
			return;
		}
		Optional<Cookie> cookie = getAuthCookie(request);
		if (isAuthPath(request) || isValidCookie(s3Host.get(), cookie)) {
			// if the user is authenticated we pass on the message for downstream handlers
			ctx.fireChannelRead(request);
		} else {
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
			RenderUtils.renderError(ctx, req, new IllegalArgumentException(String.format("Host name '%s' not configured", host)));
			ReferenceCountUtil.release(req);
			return;
		}

		if (params.size() == 1) {
			String code = params.get(0);
			githubService.authRequest(code, s3Host.get())
				.subscribeOn(new EventLoopScheduler(ctx))
				.flatMap(this::buildAuthCookie)
				.doOnError(t -> RenderUtils.renderError(ctx, req, t))
				.subscribe(authCookie -> {
					boolean x = githubService.isValidLogin(s3Host.get(), authCookie);
					if (x) {
						doLogin(ctx, req, s3Host.get(), authCookie);
					} else {
						notAuthorized(ctx, req);
					}
				});
		} else {
			RenderUtils.renderError(ctx, req, new IllegalArgumentException("Missing code param"));
			ReferenceCountUtil.release(req);
		}
	}

	private void notAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
		byte[] bytes = "Not Authorized".getBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, Unpooled.wrappedBuffer(bytes));
		response.headers().set(CONTENT_TYPE, "text/plain");
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
		RenderUtils.render(ctx, req, response);
		ReferenceCountUtil.release(req);
	}

	private void doLogin(ChannelHandlerContext ctx, FullHttpRequest req, S3Host s3Host, AuthCookie authCookie) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
		response.headers().set(LOCATION, "/test.html");
		response.headers().set(CONTENT_LENGTH, 0);
		response.headers().set(CONTENT_TYPE, "text/html");
		DefaultCookie cookie = new DefaultCookie(GithubAuthHandler.AUTH_COOKIE, generateCookieValue(s3Host, authCookie));
		cookie.setHttpOnly(true);
		cookie.setMaxAge(Duration.ofDays(14).getSeconds());
		cookie.setPath("/");
		response.headers().add("Set-Cookie",
			io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT.encode(cookie));
		RenderUtils.render(ctx, req, response);
		ReferenceCountUtil.release(req);
	}

	private String generateCookieValue(S3Host s3Host, AuthCookie cookie) {
		String json;
		try {
			json = mapper.writeValueAsString(cookie);
		} catch (JsonProcessingException e) {
			throw new HopscotchException("Error when generating authcookie json", e);
		}
		return s3Host.getEncryptionService().encrypt(json);
	}

	private Observable<AuthCookie> buildAuthCookie(String token) {
		Observable<User> oUser = githubService.getUser(token);
		Observable<List<Org>> oOrgs = oUser.flatMap(user -> {
			String login = user.getLogin();
			if (login == null || login.length() < 1) {
				throw new IllegalArgumentException("Unable to locate a valid login name");
			}
			return githubService.getOrgs(user.getLogin(), token);
		});
		return Observable.zip(oUser, oOrgs, (user, orgs) -> {
			List<String> orgNames = orgs.stream().map(Org::getLogin).collect(Collectors.toList());
			return new AuthCookie(user.getLogin(), orgNames, token);
		});
	}

	private boolean isAuthPath(FullHttpRequest request) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
		String path = queryStringDecoder.path();
		return path.equals(AUTH_PATH);
	}

	private boolean isValidCookie(S3Host s3Host, Optional<Cookie> cookie) {
		if (cookie.isPresent()) {
			String value = cookie.get().value();
			try {
				String decrypted = s3Host.getEncryptionService().decrypt(value);
				AuthCookie auth = mapper.readValue(decrypted, AuthCookie.class);
				return githubService.isValidLogin(s3Host, auth);
			} catch (Exception e) {
				return false;
			}
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
