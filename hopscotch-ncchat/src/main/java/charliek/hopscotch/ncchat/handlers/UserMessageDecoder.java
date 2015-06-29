package charliek.hopscotch.ncchat.handlers;

import charliek.hopscotch.ncchat.dto.ChatEvent;
import charliek.hopscotch.ncchat.dto.ChatMessage;
import charliek.hopscotch.ncchat.services.UserBroadcastService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class UserMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
	private static Logger LOG = LoggerFactory.getLogger(UserMessageDecoder.class);

	private static final String ANSI_RESET = "\u001B[0m";
	private static final String OTHER_COLOR = "\u001B[32m";
	private static final String OWN_COLOR = "\u001B[33m";

	private static ByteBuf USER_NAME_TAKEN = Unpooled.unreleasableBuffer(
		Unpooled.copiedBuffer("That username is taken please try another?\n", StandardCharsets.UTF_8));

	private Optional<String> userName = Optional.empty();
	private final UserBroadcastService broadcastService;

	public UserMessageDecoder(UserBroadcastService broadcastService) {
		this.broadcastService = broadcastService;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		String s = msg.toString(StandardCharsets.UTF_8).trim();
		if (s.isEmpty()) {
			return;
		}
		if (!userName.isPresent()) {
			if (broadcastService.addUser(s, ctx.channel())) {
				userName = Optional.of(s);
				ctx.writeAndFlush(Unpooled.copiedBuffer(
					String.format("Welcome %s you are logged in with %d others\n",
						userName.get(), broadcastService.count() - 1), StandardCharsets.UTF_8));
			} else {
				ctx.writeAndFlush(USER_NAME_TAKEN);
			}
			return;
		}
		out.add(new ChatMessage(userName.get(), s));
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (userName.isPresent()) {
			if (evt instanceof ChatEvent && evt == ChatEvent.LOGOUT) {
				LOG.info("Removing user {}", userName.get());
				broadcastService.removeUser(userName.get());
				userName = Optional.empty();
			} else if (evt instanceof ChatMessage) {
				ChatMessage cm = (ChatMessage) evt;

				ctx.writeAndFlush(Unpooled.copiedBuffer(formattedMessage(cm), StandardCharsets.UTF_8));
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	private String formattedMessage(ChatMessage cm) {
		String from = String.format("%-15s", cm.getUser());
		if (userName.get().equals(cm.getUser())) {
			from = String.format("%s%s%s", OWN_COLOR, from, ANSI_RESET);
		} else {
			from = String.format("%s%s%s", OTHER_COLOR, from, ANSI_RESET);
		}
		return String.format("%s: %s\n", from, cm.getMessage());
	}
}
