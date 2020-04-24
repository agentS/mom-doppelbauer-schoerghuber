package io.weatherStation.message;

import io.weatherStation.dto.RecordDto;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class AmqpConsumer {

    @Incoming("sensor-data")
    public void process(String message) {
        System.out.println("received: " + message);
        Jsonb jsonBuilder = JsonbBuilder.create();
        RecordDto record = jsonBuilder.fromJson(message, RecordDto.class);

        System.out.println(record.getWeatherStationId());
        System.out.println(record.getMeasurementDto().getTemperature());
        System.out.println(record.getMeasurementDto().getHumidity());
        System.out.println(record.getMeasurementDto().getAirPressure());
    }
}
