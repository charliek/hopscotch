package charliek.hopscotch.docproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

public class DocProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final EventExecutorGroup eventExecutors;
	private final Config config;

	public DocProxyServerInitializer(Config config) {
		eventExecutors = new DefaultEventExecutorGroup(20);
		this.config = config;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		p.addLast(new HttpServerCodec());
		p.addLast(new DocProxyHandler(eventExecutors, config));
	}
}
