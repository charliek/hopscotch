package charliek.hopscotch.docproxy;

import charliek.hopscotch.docproxy.dto.Config;
import charliek.hopscotch.docproxy.handlers.DocProxyHandler;
import charliek.hopscotch.docproxy.services.S3Service;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class DocProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final Config config;
	private final S3Service s3Service;

	public DocProxyServerInitializer(Config config) {
		s3Service = new S3Service();
		this.config = config;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		p.addLast(new HttpServerCodec());
		p.addLast(new DocProxyHandler(s3Service, config));
	}
}
