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
        // {id};{temp};{humidity};{airPressure}
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        for(Simulator sensor : sensors){
            sb.append(";");
            String sensorMessage = sensor.generateMessage();
            sb.append(sensorMessage);
        }
        return sb.toString();
    }
}
