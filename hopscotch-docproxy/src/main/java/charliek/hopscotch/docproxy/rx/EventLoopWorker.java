package charliek.hopscotch.docproxy.rx;

import io.netty.channel.EventLoop;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

import java.util.concurrent.TimeUnit;

public class EventLoopWorker extends Scheduler.Worker {

	private final EventLoop eventLoop;

	public EventLoopWorker(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	@Override
	public Subscription schedule(Action0 action) {
		return new EventLoopSubscription(
			eventLoop.submit(action::call)
		);
	}

	@Override
	public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
		return new EventLoopSubscription(
			eventLoop.schedule(action::call, delayTime, unit)
		);
	}

	@Override
	public void unsubscribe() {
		throw new UnsupportedOperationException("Unsubscribe not yet supported on event loop worker");
	}

	@Override
	public boolean isUnsubscribed() {
		return false;
	}
}

