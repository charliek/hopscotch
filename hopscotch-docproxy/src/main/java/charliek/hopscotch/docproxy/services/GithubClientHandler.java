package charliek.hopscotch.docproxy.services;

import charliek.hopscotch.docproxy.utils.ObjectMapperBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import rx.subjects.Subject;

import java.nio.charset.StandardCharsets;

public class GithubClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
	private static final ObjectMapper mapper = ObjectMapperBuilder.build();

	private final Subject subscriber;
	private final TypeReference ref;

	public GithubClientHandler(Subject subscriber, TypeReference ref) {
		this.subscriber = subscriber;
		this.ref = ref;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
		try {
			String payload = msg.content().toString(StandardCharsets.UTF_8);
			Object o = mapper.readValue(payload, ref);
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
