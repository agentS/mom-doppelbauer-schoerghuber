package io.weatherStation;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/stations/socket/{stationId}")
@ApplicationScoped
public class WeatherStationSocket {

    Map<String, List<Session>> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("stationId") String stationId) {
        if(!sessions.containsKey(stationId)){
            sessions.put(stationId, new ArrayList<>());
        }
        sessions.get(stationId).add(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("stationId") String stationId) {
        if(sessions.containsKey(stationId)){
            List<Session> tmpSessions = sessions.get(stationId);
            tmpSessions.remove(session);
            if(tmpSessions.isEmpty()){
                sessions.remove(stationId);
            }
        }
    }

    @OnError
    public void onError(Session session, @PathParam("stationId") String stationId, Throwable throwable) {
        if(sessions.containsKey(stationId)){
            List<Session> tmpSessions = sessions.get(stationId);
            tmpSessions.remove(session);
            if(tmpSessions.isEmpty()){
                sessions.remove(stationId);
            }
        }
    }

    public void broadcast(String stationId, String message) {
        for(Map.Entry<String, List<Session>> entry : sessions.entrySet()){
            if(entry.getKey().equals(stationId)){
                for(Session session : entry.getValue()){
                    session.getAsyncRemote().sendObject(message, result -> {
                        if (result.getException() != null) {
                            System.out.println("Unable to send message: " + result.getException());
                        }
                    });
                }
            }
        }
    }
}
