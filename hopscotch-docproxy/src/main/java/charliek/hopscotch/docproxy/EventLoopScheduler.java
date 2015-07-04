package charliek.hopscotch.docproxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import rx.Scheduler;

public class EventLoopScheduler extends Scheduler {
	private final EventLoop eventLoop;

	public EventLoopScheduler(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public EventLoopScheduler(ChannelHandlerContext ctx) {
		this(ctx.channel().eventLoop());
	}

	@Override
	public Worker createWorker() {
		return new EventLoopWorker(this.eventLoop);
	}
}
