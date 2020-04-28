package com.wetr;

import com.wetr.simulator.impl.*;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final int MQTT_MESSAGE_DELAY = 5000;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("USAGE: programName weatherStationIdOffset mqttBrokerHostname mqttBrokerPort");
        }

        final int weatherStationIdOffset = Integer.parseInt(args[0]);
        final String mqttBrokerHostname = args[1];
        final int mqttBrokerPort = Integer.parseInt(args[2]);
        List<StationSimulator> stationSimulators = createStationSimulators(weatherStationIdOffset);
        MqttMessageGenerator messageGenerator = new MqttMessageGenerator(stationSimulators, mqttBrokerHostname, mqttBrokerPort);
        while(true){
            messageGenerator.call();
            Thread.sleep(MQTT_MESSAGE_DELAY);
        }
    }

    private static List<StationSimulator> createStationSimulators(final int weatherStationIdOffset) {
        List<StationSimulator> stationSimulators = new ArrayList<>();
        stationSimulators.add(createStationSimulator(weatherStationIdOffset));
        stationSimulators.add(createStationSimulator(weatherStationIdOffset));
        stationSimulators.add(createStationSimulator(weatherStationIdOffset));
        return stationSimulators;
    }

    public static StationSimulator createStationSimulator(final int weatherStationIdOffset) {
        StationSimulator simulator = new StationSimulator(weatherStationIdOffset);
        simulator.addSensor(new SensorSimulator()); // temperature
        simulator.addSensor(new SensorSimulator()); // humidity
        simulator.addSensor(new SensorSimulator()); // airPressure
        return simulator;
    }
}
