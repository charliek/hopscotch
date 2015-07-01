package charliek.hopscotch.docproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class DocProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	public DocProxyServerInitializer() {
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		p.addLast(new HttpServerCodec());
		p.addLast(new DocProxyHandler());
	}
}
