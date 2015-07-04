package charliek.hopscotch.docproxy.rx;

import io.netty.util.concurrent.Future;
import rx.Subscription;

class EventLoopSubscription implements Subscription {
	private final Future future;
	private boolean unsubscribed = false;

	public EventLoopSubscription(Future future) {
		this.future = future;
	}

	@Override
	public void unsubscribe() {
		unsubscribed = true;
		future.cancel(true);
	}

	@Override
	public boolean isUnsubscribed() {
		return unsubscribed;
	}
}
