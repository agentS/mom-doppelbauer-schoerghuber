package io.weatherstation.persist.amqp;

import io.vertx.amqp.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.weatherstation.persist.EventBusAddresses;
import io.weatherstation.persist.postgresql.PostgresqlPersistenceVerticle;

public class AmqpConsumerVerticle extends AbstractVerticle {
	private AmqpClient amqpClient;
	private EventBus eventBus;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		this.eventBus = this.vertx.eventBus();
		var amqpConfiguration = new AmqpClientOptions()
			.setHost(this.config().getString("hostname"))
			.setPort(this.config().getInteger("port"))
			.setUsername(this.config().getString("username"))
			.setPassword(this.config().getString("password"))
			.setContainerId(this.config().getString("containerId"));
		this.amqpClient = AmqpClient.create(this.vertx, amqpConfiguration);
		this.amqpClient.connect(connectionResult -> {
			if (connectionResult.failed()) {
				startFuture.fail(connectionResult.cause());
			} else {
				AmqpConnection amqpConnection = connectionResult.result();
				var amqpConnectionOptions = new AmqpReceiverOptions()
					.setAutoAcknowledgement(false)
					.setDurable(true);
				amqpConnection.createReceiver(
					this.config().getString("queueName"),
					amqpConnectionOptions,
					receiverCreationResult -> {
						if (receiverCreationResult.failed()) {
							startFuture.fail(receiverCreationResult.cause());
						} else {
							AmqpReceiver receiver = receiverCreationResult.result();
							receiver
								.exceptionHandler(this::handleWeatherRecordMessageException)
								.handler(this::handleWeatherRecordMessage);
							startFuture.complete();
							System.out.println("AMQP receiver listening to queue " + this.config().getString("queueName"));
						}
					}
				);
			}
		});
	}

	private void handleWeatherRecordMessage(AmqpMessage amqpMessage) {
		JsonObject body = amqpMessage.bodyAsJsonObject();
		this.eventBus.send(
			EventBusAddresses.PERSISTENCE_POSTGRESQL,
			body,
			response -> {
				if (response.failed()) {
					ReplyException exception = ((ReplyException) response.cause());
					if (exception.failureCode() != PostgresqlPersistenceVerticle.FAILURE_CODE_DUPLICATED_RECORD) {
						amqpMessage.rejected();
						response.cause().printStackTrace(System.err);
					} else {
						amqpMessage.accepted();
					}
				} else {
					amqpMessage.accepted();
					System.out.println("Inserted record for " + body);
				}
			}
		);
	}

	private void handleWeatherRecordMessageException(Throwable exception) {
		exception.printStackTrace(System.err);
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		if (this.amqpClient != null) {
			this.amqpClient.close(closingResult -> {
				if (closingResult.failed()) {
					stopFuture.fail(closingResult.cause());
				} else {
					stopFuture.complete();
				}
			});
		}
	}
}
