package charliek.hopscotch.ncchat;

import charliek.hopscotch.ncchat.handlers.GreetingHandler;
import charliek.hopscotch.ncchat.handlers.MessagePublishingHandler;
import charliek.hopscotch.ncchat.handlers.UserMessageDecoder;
import charliek.hopscotch.ncchat.services.RabbitService;
import charliek.hopscotch.ncchat.services.UserBroadcastService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

public class TcpListenApp {
	static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
	static final String RABBIT_HOST = System.getProperty("rabbit", "localhost");

	public static void main(String[] args) throws Exception {
		UserBroadcastService userBroadcastService = new UserBroadcastService();
		RabbitService publisher = new RabbitService(userBroadcastService, RABBIT_HOST);
		publisher.listen();

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		DefaultEventExecutorGroup eventExecutors = new DefaultEventExecutorGroup(20);

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 100)
				.handler(new LoggingHandler(LogLevel.DEBUG))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						p.addLast(new GreetingHandler());
						p.addLast(new LineBasedFrameDecoder(1000));
						p.addLast(new UserMessageDecoder(userBroadcastService));
						p.addLast(eventExecutors, new MessagePublishingHandler(publisher));
					}
				});

			// Start the server.
			ChannelFuture f = b.bind(PORT).sync();

			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			publisher.close();
		}
	}
}
