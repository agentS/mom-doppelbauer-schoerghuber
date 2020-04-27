package io.weatherstation.persist.postgresql;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import io.weatherstation.dto.RecordDto;
import io.weatherstation.dto.RecordJsonConverter;
import io.weatherstation.persist.EventBusAddresses;

public class PostgresqlPersistenceVerticle extends AbstractVerticle {
	public static final int DEFAULT_PORT = 5432;
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 5;

	public static final int FAILURE_CODE_NO_CONNECTION = 0;
	public static final int FAILURE_CODE_INSERT_FAILED = 1;
	public static final int FAILURE_CODE_COMMIT_FAILED = 2;
	public static final int FAILURE_CODE_DUPLICATED_RECORD = -1;

	public static final int DUPLICATED_KEY_POSTGRESQL_ERROR_CODE = 23505;

	private PgPool postgresqlPool;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		var postgresqlConnectionOptions = new PgConnectOptions()
			.setPort(this.config().getInteger("port", DEFAULT_PORT))
			.setHost(this.config().getString("hostname"))
			.setDatabase(this.config().getString("database"))
			.setUser(this.config().getString("username"))
			.setPassword(this.config().getString("password"));
		var postgresqlConnectionPoolOptions = new PoolOptions()
			.setMaxSize(this.config().getInteger("poolSize", DEFAULT_CONNECTION_POOL_SIZE));
		this.postgresqlPool = PgPool.pool(
			this.vertx,
			postgresqlConnectionOptions,
			postgresqlConnectionPoolOptions
		);

		EventBus eventBus = this.vertx.eventBus();
		eventBus.consumer(
			EventBusAddresses.PERSISTENCE_POSTGRESQL,
			this::persistRecord
		);
		startFuture.complete();
	}

	private static final String STATEMENT_INSERT_RECORD =
		"INSERT INTO measurement"
		+ "(station_id, created_at, temperature, humidity, air_pressure)"
		+ " VALUES ($1, $2, $3, $4, $5)";

	private void persistRecord(Message<JsonObject> message) {
		RecordDto recordDto = RecordJsonConverter.fromJson(message.body());

		this.postgresqlPool.getConnection(databaseConnectionEstablishmentResult -> {
			if (databaseConnectionEstablishmentResult.failed()) {
				message.fail(
					FAILURE_CODE_NO_CONNECTION,
					databaseConnectionEstablishmentResult.cause().getMessage()
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
									databaseConnection.close();
									this.handlePostgresqlExcpetion(
										message,
										(PgException) databaseInsertResult.cause(),
										FAILURE_CODE_INSERT_FAILED
									);
								});
							} else {
								transaction.commit(transactionCommitResult -> {
									if (transactionCommitResult.failed()) {
										transaction.rollback(transactionRollbackResult -> {
											databaseConnection.close();
											this.handlePostgresqlExcpetion(
												message,
												(PgException) transactionCommitResult.cause(),
												FAILURE_CODE_COMMIT_FAILED
											);
										});
									} else {
										databaseConnection.close();
										message.reply(message.body());
									}
								});
							}
						}
					);
			}
		});
	}

	private void handlePostgresqlExcpetion(
		Message<JsonObject> message,
		PgException exception,
		final int nonDuplicateFailureCode
	) {
		try {
			int errorCode = Integer.parseInt(exception.getCode());
			if (errorCode == DUPLICATED_KEY_POSTGRESQL_ERROR_CODE) {
				message.fail(
					FAILURE_CODE_DUPLICATED_RECORD,
					exception.getMessage()
				);
			} else {
				message.fail(
					nonDuplicateFailureCode,
					exception.getMessage()
				);
			}
		} catch (NumberFormatException conversionException) {
			message.fail(
				nonDuplicateFailureCode,
				exception.getMessage()
			);
		}
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		if (this.postgresqlPool != null) {
			this.postgresqlPool.close();
		}
	}
}
