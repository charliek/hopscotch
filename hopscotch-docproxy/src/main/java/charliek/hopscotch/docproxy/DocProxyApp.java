package charliek.hopscotch.docproxy;

import charliek.hopscotch.docproxy.dto.Config;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocProxyApp {
	private static final Logger LOG = LoggerFactory.getLogger(DocProxyApp.class);

	private Config config;

	public DocProxyApp() {
	}

	public static void main(String[] args) throws Exception {
		new DocProxyApp().run(args);
	}

	public void run(String[] args) throws Exception {
		parseCli(args);
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new DocProxyServerInitializer(config));

			Channel ch = b.bind(config.getPort()).sync().channel();
			LOG.info("Open your web browser and navigate to http://127.0.0.1:{}/", config.getPort());
			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	private void parseCli(String[] args) {
		config = new Config();
		JCommander commander = new JCommander(config);
		try {
			commander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage() + "\n");
			commander.usage();
			System.exit(1);
		}
	}

}
