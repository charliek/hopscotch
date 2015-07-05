package charliek.hopscotch.docproxy.services;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.GithubCredentials;
import charliek.hopscotch.docproxy.services.github.Auth;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GithubService {

	private final EventLoopGroup group;
	private final SslContext sslCtx;

	public GithubService(EventLoopGroup group) {
		this.group = group;
		try {
			sslCtx = SslContextBuilder.forClient()
				.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} catch (SSLException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String encodeValue(String k) {
		try {
			return URLEncoder.encode(k, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@FunctionalInterface
	private interface JsonRequest {
		HttpRequest buildRequest();
	}

	public Observable<String> authRequest(String code, GithubCredentials creds) {
		Map<String, String> params = new HashMap<>();
		params.put("client_id", creds.getClientId());
		params.put("client_secret", creds.getClientSecret());
		params.put("code", code);
		String url = "https://github.com/login/oauth/access_token";
		return postApiCall(url, params, Auth.class)
			.map(Auth::getAccessToken);
	}

	private <T> Observable<T> doRequest(URI uri, Class<T> klass, JsonRequest req) {
		// TODO there is a race condition since the publish subject might not be subscribed to yet
		PublishSubject<T> subject = PublishSubject.create();
		String scheme = uri.getScheme();
		String host = uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}
		Bootstrap b = new Bootstrap();
		b.group(group)
			.channel(NioSocketChannel.class)
			.handler(new GithubClientInitializer(sslCtx, subject, klass));
		b.connect(host, port).addListener((ChannelFuture f) -> {
			if (f.isSuccess()) {
				Channel ch = f.channel();
				HttpRequest request = req.buildRequest();
				request.headers().set(HttpHeaderNames.HOST, host);
				ch.writeAndFlush(request).addListener(f2 -> {
					if (!f.isSuccess()) {
						subject.onError(f2.cause());
					}
				});
			} else {
				subject.onError(f.cause());
			}
		});
		return subject;
	}

	public <T> Observable<T> getApiCall(String url, Class<T> klass) {
		// TODO there is a race condition since the publish subject might not be subscribed to yet
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		return doRequest(uri, klass, () -> {
			HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString());
			request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
			request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
			request.headers().set(HttpHeaderNames.USER_AGENT, "repodoc-netty");
			request.headers().set(HttpHeaderNames.ACCEPT, "application/json");
			return request;
		});
	}

	public <T> Observable<T> postApiCall(String url, Map<String, String> params, Class<T> klass) {
		// TODO there is a race condition since the publish subject might not be subscribed to yet
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		return doRequest(uri, klass, () -> {
			StringBuilder sb = new StringBuilder();
			params.forEach((k, v) -> {
				sb.append(encodeValue(k))
					.append("=")
					.append(encodeValue(v))
					.append("&");
			});
			ByteBuf cb = Unpooled.copiedBuffer(sb.toString(), StandardCharsets.UTF_8);
			HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString(), cb);
			request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
			request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
			request.headers().set(HttpHeaderNames.USER_AGENT, "repodoc-netty");
			request.headers().set(HttpHeaderNames.ACCEPT, "application/json");
			request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
			request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, cb.readableBytes());
			return request;
		});
	}
}
