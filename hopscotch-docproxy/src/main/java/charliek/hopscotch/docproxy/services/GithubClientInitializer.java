package charliek.hopscotch.docproxy.services;

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
	private final Class klass;

	public GithubClientInitializer(SslContext sslCtx, Subject subscriber, Class klass) {
		this.sslCtx = sslCtx;
		this.subscriber = subscriber;
		this.klass = klass;
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
		p.addLast(new GithubClientHandler(subscriber, klass));
	}
}