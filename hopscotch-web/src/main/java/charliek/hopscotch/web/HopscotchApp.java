package charliek.hopscotch.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.config.ConfigData;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.guice.Guice;
import ratpack.jackson.JacksonModule;
import ratpack.rx.RxRatpack;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.Service;
import ratpack.server.StartEvent;

public class HopscotchApp {
	private final static Logger LOG = LoggerFactory.getLogger(HopscotchApp.class);

	public static void main(String[] args) throws Exception {
		ConfigData configData = ConfigData.of(d -> d
				.env().sysProps());

		ServerConfig.Builder serverConfig = ServerConfig.findBaseDir()
				.sysProps().env();

		RatpackServer.start(spec -> spec
				.serverConfig(serverConfig)
				.registry(Guice.registry(b -> b
					.bindInstance(ConfigData.class, configData)
					.module(HopscotchModule.class)
					.module(JacksonModule.class)
					.bindInstance(Service.class, new Service() {
						@Override
						public void onStart(StartEvent event) throws Exception {
							RxRatpack.initialize();
						}
					})
					.bind(ErrorHandler.class, DefaultDevelopmentErrorHandler.class)))
				.handlers(c -> {
					c.prefix("static", nested ->
							nested.assets("/static", "index.html"));
				}));
	}
}
