package charliek.hopscotch.docproxy;

import charliek.hopscotch.docproxy.dto.AppConfig;
import charliek.hopscotch.docproxy.dto.CliConfig;
import charliek.hopscotch.docproxy.handlers.DocProxyHandler;
import charliek.hopscotch.docproxy.handlers.GithubAuthHandler;
import charliek.hopscotch.docproxy.services.GithubService;
import charliek.hopscotch.docproxy.services.S3Service;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class DocProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final AppConfig appConfig;
	private final S3Service s3Service;
	private final GithubService githubService;

	public DocProxyServerInitializer(GithubService githubService, AppConfig appConfig) {
		s3Service = new S3Service();
		this.githubService = githubService;
		this.appConfig = appConfig;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		p.addLast(new HttpServerCodec());
		p.addLast(new HttpObjectAggregator(1048576));
		p.addLast(new GithubAuthHandler(appConfig));
		p.addLast(new DocProxyHandler(s3Service, githubService, appConfig));
	}
}
