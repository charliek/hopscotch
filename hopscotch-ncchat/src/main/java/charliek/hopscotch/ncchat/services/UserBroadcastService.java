package charliek.hopscotch.ncchat.services;

import charliek.hopscotch.ncchat.dto.ChatMessage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class UserBroadcastService {
	static Logger LOG = LoggerFactory.getLogger(UserBroadcastService.class);

	private final ConcurrentHashMap<String, Channel> users = new ConcurrentHashMap<>();

	public boolean addUser(String user, Channel channel) {
		return users.putIfAbsent(user, channel) == null;
	}

	public int count() {
		return users.size();
	}

	public void removeUser(String user) {
		users.remove(user);
	}

	public void broadcast(ChatMessage msg) {
		users.forEach((user, channel) -> {
			LOG.info("Sending message to {} from {}", user, msg.getUser());
			channel.pipeline().fireUserEventTriggered(msg);
		});
	}
}
