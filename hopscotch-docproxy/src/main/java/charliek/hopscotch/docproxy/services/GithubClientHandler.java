package charliek.hopscotch.docproxy.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import rx.subjects.Subject;

import java.nio.charset.StandardCharsets;

public class GithubClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
	private static final ObjectMapper mapper = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	private final Subject subscriber;
	private final Class klass;

	public GithubClientHandler(Subject subscriber, Class klass) {
		this.subscriber = subscriber;
		this.klass = klass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
		try {
			String payload = msg.content().toString(StandardCharsets.UTF_8);
			Object o = mapper.readValue(payload, klass);
			subscriber.onNext(o);
			subscriber.onCompleted();
			ctx.close();
		} catch (Exception e) {
			subscriber.onError(e);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
