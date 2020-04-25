package io.weatherstation.persist.postgresql;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class PostgresqlPersistenceVerticle extends AbstractVerticle {
	public static final int DEFAULT_PORT = 5432;
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 5;

	private PgPool client;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		var connectionOptions = new PgConnectOptions()
			.setPort(this.config().getInteger("port", DEFAULT_PORT))
			.setHost(this.config().getString("hostname"))
			.setDatabase(this.config().getString("database"))
			.setUser(this.config().getString("username"))
			.setPassword(this.config().getString("password"));
		var connectionPoolOptions = new PoolOptions()
			.setMaxSize(this.config().getInteger("poolSize", DEFAULT_CONNECTION_POOL_SIZE));

		this.client = PgPool.pool(this.vertx, connectionOptions, connectionPoolOptions);
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		if (this.client != null) {
			this.client.close();
		}
	}
}
