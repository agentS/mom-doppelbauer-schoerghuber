package io.weatherStation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/stations/socket/{stationId}")
@ApplicationScoped
public class WeatherStationSocket {
    private SocketManager socketManager;

    @Inject
    public WeatherStationSocket(SocketManager socketManager){
        this.socketManager = socketManager;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("stationId") String stationId) {
        socketManager.add(stationId, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("stationId") String stationId) {
        socketManager.remove(stationId, session);
    }

    @OnError
    public void onError(Session session, @PathParam("stationId") String stationId,
                        Throwable throwable) {
        socketManager.remove(stationId, session);
    }
}
