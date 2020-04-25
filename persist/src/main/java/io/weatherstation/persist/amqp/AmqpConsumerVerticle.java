package io.weatherstation.persist.amqp;

import io.vertx.amqp.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import io.weatherstation.dto.RecordDto;
import io.weatherstation.dto.RecordJsonConverter;

public class AmqpConsumerVerticle extends AbstractVerticle {
	public static final int DEFAULT_PORT = 5432;
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 5;

	private AmqpClient amqpClient;
	private PgPool postgresqlPool;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		EventBus eventBus = this.vertx.eventBus();
		JsonObject amqpConsumerConfiguration = this.config().getJsonObject("amqpConsumer");
		var amqpConfiguration = new AmqpClientOptions()
			.setHost(amqpConsumerConfiguration.getString("hostname"))
			.setPort(amqpConsumerConfiguration.getInteger("port"))
			.setUsername(amqpConsumerConfiguration.getString("username"))
			.setPassword(amqpConsumerConfiguration.getString("password"));
		this.amqpClient = AmqpClient.create(this.vertx, amqpConfiguration);
		this.amqpClient.connect(connectionResult -> {
			if (connectionResult.failed()) {
				startFuture.fail(connectionResult.cause());
			} else {
				AmqpConnection amqpConnection = connectionResult.result();
				var amqpConnectionOptions = new AmqpReceiverOptions();
				amqpConnection.createReceiver(
					amqpConsumerConfiguration.getString("queueName"),
					receiverCreationResult -> {
						if (receiverCreationResult.failed()) {
							startFuture.fail(receiverCreationResult.cause());
						} else {
							JsonObject postgresqlConfiguration = this.config().getJsonObject("postgresql");
							var postgresqlConnectionOptions = new PgConnectOptions()
								.setPort(postgresqlConfiguration.getInteger("port", DEFAULT_PORT))
								.setHost(postgresqlConfiguration.getString("hostname"))
								.setDatabase(postgresqlConfiguration.getString("database"))
								.setUser(postgresqlConfiguration.getString("username"))
								.setPassword(postgresqlConfiguration.getString("password"));
							var postgresqlConnectionPoolOptions = new PoolOptions()
								.setMaxSize(postgresqlConfiguration.getInteger("poolSize", DEFAULT_CONNECTION_POOL_SIZE));
							this.postgresqlPool = PgPool.pool(
								this.vertx,
								postgresqlConnectionOptions,
								postgresqlConnectionPoolOptions
							);

							AmqpReceiver receiver = receiverCreationResult.result();
							receiver
								.exceptionHandler(this::handleWeatherRecordMessageException)
								.handler(this::handleWeatherRecordMessage);

							startFuture.complete();
							System.out.println("AMQP receiver listening to queue " + amqpConsumerConfiguration.getString("queueName"));
						}
					}
				);
			}
		});
	}

	private static final String STATEMENT_INSERT_RECORD =
		"INSERT INTO measurement"
		+ "(station_id, created_at, temperature, humidity, air_pressure)"
		+ " VALUES ($1, $2, $3, $4, $5)";

	private void handleWeatherRecordMessage(AmqpMessage amqpMessage) {
		JsonObject body = amqpMessage.bodyAsJsonObject();
		RecordDto recordDto = RecordJsonConverter.fromJson(body);

		this.postgresqlPool.getConnection(databaseConnectionEstablishmentResult -> {
			if (databaseConnectionEstablishmentResult.failed()) {
				this.handleWeatherRecordMessageException(
					databaseConnectionEstablishmentResult.cause()
				);
			} else {
				SqlConnection databaseConnection = databaseConnectionEstablishmentResult.result();
				Transaction transaction = databaseConnection.begin();
				databaseConnection.preparedQuery(STATEMENT_INSERT_RECORD)
					.execute(
						Tuple.of(
							recordDto.getWeatherStationId(),
							recordDto.getTimestamp(),
							recordDto.getMeasurementDto().getTemperature(),
							recordDto.getMeasurementDto().getHumidity(),
							recordDto.getMeasurementDto().getAirPressure()
						),
						databaseInsertResult -> {
							if (databaseInsertResult.failed()) {
								transaction.rollback(transactionRollbackResult -> {
									this.handleWeatherRecordMessageException(databaseInsertResult.cause());
								});
							} else {
								transaction.commit(transactionCommitResult -> {
									if (transactionCommitResult.failed()) {
										transaction.rollback(transactionRollbackResult -> {
											this.handleWeatherRecordMessageException(transactionCommitResult.cause());
										});
									} else {
										System.out.println("Inserted record for " + body);
									}
								});
							}
						}
					);
			}
		});
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
					this.postgresqlPool.close();
					stopFuture.complete();
				}
			});
		}
	}
}
