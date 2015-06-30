package charliek.hopscotch.web;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import ratpack.handling.HandlerDecorator;

public class HopscotchModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(TimedHandler.class).in(Scopes.SINGLETON);

		Multibinder
			.newSetBinder(binder(), HandlerDecorator.class)
			.addBinding()
			.to(ApiHandlerDecorator.class);
	}
}
