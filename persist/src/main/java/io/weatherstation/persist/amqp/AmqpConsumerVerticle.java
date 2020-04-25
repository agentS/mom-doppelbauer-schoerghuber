package io.weatherstation.persist.amqp;

import io.vertx.amqp.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.weatherstation.dto.RecordDto;
import io.weatherstation.dto.RecordJsonConverter;

public class AmqpConsumerVerticle extends AbstractVerticle {
	private AmqpClient client;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		var amqpConfiguration = new AmqpClientOptions()
			.setHost(this.config().getString("host"))
			.setPort(this.config().getInteger("port"))
			.setUsername(this.config().getString("username"))
			.setPassword(this.config().getString("password"));
		this.client = AmqpClient.create(this.vertx, amqpConfiguration);
		this.client.connect(connectionResult -> {
			if (connectionResult.failed()) {
				startFuture.fail(connectionResult.cause());
			} else {
				AmqpConnection connection = connectionResult.result();
				var connectionOptions = new AmqpReceiverOptions();
				connection.createReceiver(
					this.config().getString("queueName"),
					receiverCreationResult -> {
						if (receiverCreationResult.failed()) {
							startFuture.fail(receiverCreationResult.cause());
						} else {
							AmqpReceiver receiver = receiverCreationResult.result();
							receiver
								.exceptionHandler(exception -> {
									exception.printStackTrace(System.err);
								})
								.handler(message -> {
									JsonObject body = message.bodyAsJsonObject();
									System.out.println(body);
									RecordDto record = RecordJsonConverter.fromJson(body);
								});
							startFuture.complete();
							System.out.println("AMQP receiver listening to queue " + this.config().getString("queueName"));
						}
					}
				);
			}
		});
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		if (this.client != null) {
			this.client.close(closingResult -> {
				if (closingResult.failed()) {
					stopFuture.fail(closingResult.cause());
				} else {
					stopFuture.complete();
				}
			});
		}
	}
}
