package charliek.hopscotch.ncchat.handlers;

import charliek.hopscotch.ncchat.dto.ChatMessage;
import charliek.hopscotch.ncchat.services.RabbitService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePublishingHandler extends ChannelInboundHandlerAdapter {

	static Logger LOG = LoggerFactory.getLogger(MessagePublishingHandler.class);
	private final RabbitService publisher;

	public MessagePublishingHandler(RabbitService publisher) {
		this.publisher = publisher;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ChatMessage) {
			ChatMessage chatMessage = (ChatMessage) msg;
			publisher.sendMessage(chatMessage);
		} else {
			LOG.error("Unexpected message type of {}", msg.getClass());
		}
	}

}
