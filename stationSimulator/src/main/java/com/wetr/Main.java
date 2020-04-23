package com.wetr;

import com.wetr.simulator.impl.*;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final int MQTT_MESSAGE_DELAY = 5000;

    public static void main(String[] args) throws Exception {
        List<StationSimulator> stationSimulators = createStationSimulators();
        MqttMessageGenerator messageGenerator = new MqttMessageGenerator(stationSimulators);
        while(true){
            messageGenerator.call();
            Thread.sleep(MQTT_MESSAGE_DELAY);
        }
    }

    private static List<StationSimulator> createStationSimulators() {
        List<StationSimulator> stationSimulators = new ArrayList<>();
        stationSimulators.add(createStationSimulator());
        stationSimulators.add(createStationSimulator());
        return stationSimulators;
    }

    public static StationSimulator createStationSimulator() {
        StationSimulator simulator = new StationSimulator();
        simulator.addSensor(new SensorSimulator()); // temperature
        simulator.addSensor(new SensorSimulator()); // humidity
        simulator.addSensor(new SensorSimulator()); // airPressure
        return simulator;
    }
}
