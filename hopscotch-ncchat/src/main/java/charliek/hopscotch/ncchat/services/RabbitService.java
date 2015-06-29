package charliek.hopscotch.ncchat.services;

import charliek.hopscotch.ncchat.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitService {
	private static Logger LOG = LoggerFactory.getLogger(RabbitService.class);
	private static ObjectMapper MAPPER = new ObjectMapper();
	private static final String EXCHANGE_NAME = "hopscotch";

	private final Connection connection;
	private final Channel channel;
	private final UserBroadcastService broadcast;

	public RabbitService(UserBroadcastService broadcast, String host) throws Exception {
		this.broadcast = broadcast;
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		connection = factory.newConnection();
		channel = connection.createChannel();
		setupExchanges();
	}

	public void setupExchanges() throws Exception {
		channel.exchangeDeclare(EXCHANGE_NAME, "topic");
	}

	public void listen() throws Exception {
		new Thread(() -> {
			try {
				String queueName = channel.queueDeclare().getQueue();
				channel.queueBind(queueName, EXCHANGE_NAME, "#");
				QueueingConsumer consumer = new QueueingConsumer(channel);
				channel.basicConsume(queueName, true, consumer);
				LOG.info("Listening for rabbit messages");
				while (true) {
					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
					ChatMessage message = MAPPER.readValue(delivery.getBody(), ChatMessage.class);
					broadcast.broadcast(message);
				}
			} catch (ShutdownSignalException e) {
				LOG.info("Message listening has been shutdown");
			} catch (Exception e) {
				LOG.error("Unexpected error when listening to rabbit", e);
			}
		}).start();
	}

	public void sendMessage(ChatMessage message) throws Exception {
		channel.basicPublish(EXCHANGE_NAME, "hopscotch.tcp", null, MAPPER.writeValueAsBytes(message));
		LOG.info("Sent message {}", message);
	}

	public void close() throws Exception {
		channel.close();
		connection.close();
	}
}
