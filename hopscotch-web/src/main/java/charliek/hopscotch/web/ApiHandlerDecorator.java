package charliek.hopscotch.web;

import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;

public class ApiHandlerDecorator implements HandlerDecorator {
	@Override
	public Handler decorate(Registry serverRegistry, Handler rest) throws Exception {
		return Handlers.chain(serverRegistry, c -> {
			c.get("api/timed", TimedHandler.class);
			// Add other api endpoints here
		});
	}
}
