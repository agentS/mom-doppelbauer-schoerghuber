package com.wetr.simulator.impl;

import com.wetr.simulator.Simulator;

import java.util.Random;

public class SensorSimulator implements Simulator {
    private static int sensorId = 0;
    private int id;
    private Random rnd;

    public SensorSimulator(){
        sensorId++;
        id = sensorId;
        rnd = new Random();
    }

    public int getId() {
        return id;
    }

    @Override
    public String generateMessage() {
        double sensorValue =  80 + rnd.nextDouble() * 20.0;
        String message = String.format("%04.2f", sensorValue);
        message = message.replaceAll(",", ".");
        return message;
    }
}
