package charliek.hopscotch.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.config.ConfigData;
import ratpack.error.ClientErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.guice.Guice;
import ratpack.jackson.guice.JacksonModule;
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
				.module(JacksonModule.class, c -> c
					.modules(new Jdk7Module(), new Jdk8Module(), new GuavaModule(), new JSR310Module())
					.withMapper(m -> m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
					.prettyPrint(false))
				.bindInstance(Service.class, new Service() {
					@Override
					public void onStart(StartEvent event) throws Exception {
						RxRatpack.initialize();
					}
				})
				.bind(ClientErrorHandler.class, DefaultDevelopmentErrorHandler.class)))
			.handlers(c -> {
				c.prefix("static", nested ->
					nested.files(f -> f.dir("static").indexFiles("index.html")));
			}));
	}
}
