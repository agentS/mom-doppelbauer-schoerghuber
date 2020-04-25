package io.weatherStation;

import javax.websocket.Session;

public interface SocketManager {
    public void remove(String stationId, Session session);
    public void add(String stationId, Session session);
    void broadcast(String stationId, String message);
}
