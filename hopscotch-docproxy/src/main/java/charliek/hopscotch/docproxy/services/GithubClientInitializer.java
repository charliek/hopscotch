package charliek.hopscotch.docproxy.services;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import rx.subjects.Subject;

public class GithubClientInitializer extends ChannelInitializer<SocketChannel> {

	private final SslContext sslCtx;
	private final Subject subscriber;
	private final TypeReference ref;

	public GithubClientInitializer(SslContext sslCtx, Subject subscriber, TypeReference ref) {
		this.sslCtx = sslCtx;
		this.subscriber = subscriber;
		this.ref = ref;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		if (sslCtx != null) {
			p.addLast(sslCtx.newHandler(ch.alloc()));
		}
		p.addLast(new HttpClientCodec());
		p.addLast(new HttpContentDecompressor());
		p.addLast(new HttpObjectAggregator(1048576));
		p.addLast(new GithubClientHandler(subscriber, ref));
	}
}