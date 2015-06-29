package charliek.hopscotch.ncchat.handlers;

import charliek.hopscotch.ncchat.dto.ChatEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class GreetingHandler extends ChannelInboundHandlerAdapter {
	static Logger LOG = LoggerFactory.getLogger(GreetingHandler.class);
	static private ByteBuf welcomeMessage = Unpooled.unreleasableBuffer(
		Unpooled.copiedBuffer("Welcome. What is your chat username?\n", StandardCharsets.UTF_8));

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		ctx.writeAndFlush(welcomeMessage);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOG.error("Unhandled exception in pipeline. Closing connection.", cause);
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireUserEventTriggered(ChatEvent.LOGOUT);
	}
}
