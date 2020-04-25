package io.weatherStation.message;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.weatherStation.WeatherStationSocket;
import io.weatherStation.dto.RecordDto;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class AmqpConsumer {

    private WeatherStationSocket weatherStationSocket;

    @Inject
    public AmqpConsumer(WeatherStationSocket weatherStationSocket){
        this.weatherStationSocket = weatherStationSocket;
    }

    @Incoming("sensor-data")
    // @Outgoing("record-stream")
    // @Broadcast
    public void process(String message) {
        System.out.println("received: " + message);
        Jsonb jsonBuilder = JsonbBuilder.create();
        RecordDto record = jsonBuilder.fromJson(message, RecordDto.class);

        // System.out.println(record.getWeatherStationId());
        // System.out.println(record.getMeasurementDto().getTemperature());
        // System.out.println(record.getMeasurementDto().getHumidity());
        // System.out.println(record.getMeasurementDto().getAirPressure());

        weatherStationSocket.broadcast(String.valueOf(record.getWeatherStationId()), message);
        //return message;
    }
}
