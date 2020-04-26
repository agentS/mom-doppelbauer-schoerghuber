package io.weatherStation.message;

import io.weatherStation.SocketManager;
import io.weatherStation.dto.RecordDto;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class AmqpConsumer {
    private SocketManager socketManager;

    @Inject
    public AmqpConsumer(SocketManager socketManager){
        this.socketManager = socketManager;
    }

    @Incoming("sensor-data")
    public void process(String message) {
        System.out.println("received: " + message);
        Jsonb jsonBuilder = JsonbBuilder.create();
        RecordDto record = jsonBuilder.fromJson(message, RecordDto.class);
        socketManager.broadcast(String.valueOf(record.getWeatherStationId()), message);
    }
}
