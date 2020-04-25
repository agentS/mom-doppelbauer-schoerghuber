package io.weatherstation.persist;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.weatherstation.persist.amqp.AmqpConsumerVerticle;
import io.weatherstation.persist.postgresql.PostgresqlPersistenceVerticle;

public class MainVerticle extends AbstractVerticle {
	public static final String YAML_CONFIGURATION_FILE_NAME = "configuration.yaml";

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		ConfigStoreOptions yamlConfigurationStore = new ConfigStoreOptions()
			.setType("file")
			.setFormat("yaml")
			.setConfig(
				new JsonObject()
					.put("path", YAML_CONFIGURATION_FILE_NAME)
			);
		ConfigRetriever configurationRetriever = ConfigRetriever.create(
			this.vertx,
			new ConfigRetrieverOptions()
				.addStore(yamlConfigurationStore)
		);
		configurationRetriever.getConfig(configurationResult -> {
			if (configurationResult.failed()) {
				throw new RuntimeException(configurationResult.cause());
			} else {
				JsonObject configuration = configurationResult.result();

				var amqpConsumerVerticle = new AmqpConsumerVerticle();
				JsonObject amqpConsumerConfiguration = configuration
					.getJsonObject("persistence")
					.getJsonObject("amqpConsumer");
				var amqpConsumerVerticleDeploymentOptions = new DeploymentOptions()
					.setConfig(amqpConsumerConfiguration);
				this.vertx.deployVerticle(
					amqpConsumerVerticle, amqpConsumerVerticleDeploymentOptions,
					amqpConsumerStartFuture -> {
						if (amqpConsumerStartFuture.failed()) {
							startFuture.fail(amqpConsumerStartFuture.cause());
						} else {
							var postgresqlPersistenceVerticle = new PostgresqlPersistenceVerticle();
							JsonObject postgresqlPersistenceConfiguration = configuration
								.getJsonObject("persistence")
								.getJsonObject("postgresql");
							var postgresqlPersistenceVerticleOptions = new DeploymentOptions()
								.setConfig(postgresqlPersistenceConfiguration);
							this.vertx.deployVerticle(
								postgresqlPersistenceVerticle, postgresqlPersistenceVerticleOptions,
								postgresqlPersistenceStartFuture -> {
									if (postgresqlPersistenceStartFuture.failed()) {
										startFuture.fail(postgresqlPersistenceStartFuture.cause());
									} else {
										startFuture.complete();
									}
								}
							);
						}
					}
				);
			}
		});

	}
}
