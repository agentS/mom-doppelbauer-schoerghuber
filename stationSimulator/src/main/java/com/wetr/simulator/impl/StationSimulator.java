package com.wetr.simulator.impl;

import com.wetr.simulator.Simulator;

import java.util.ArrayList;
import java.util.List;

public class StationSimulator implements Simulator {
    private static int stationId = 0;
    private int id;
    private List<Simulator> sensors;

    public StationSimulator(){
        sensors = new ArrayList<>();
        stationId++;
        id = stationId;
        //id = 1; // TODO: uncomment for testing insertion of duplicated records
    }

    public int getId() {
        return id;
    }

    public List<Simulator> getSensors() {
        return sensors;
    }

    public void addSensor(Simulator sensor) {
        sensors.add(sensor);
    }

    @Override
    public String generateMessage() {
        // {temp};{humidity};{airPressure}
        StringBuilder sb = new StringBuilder();
        for(Simulator sensor : sensors){
            String sensorMessage = sensor.generateMessage();
            sb.append(sensorMessage);
            sb.append(";");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
