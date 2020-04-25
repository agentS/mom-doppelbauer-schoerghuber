package io.weatherStation;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSocketManager implements SocketManager {
    private Map<String, List<Session>> sessions;

    public WebSocketManager(){
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public void remove(String stationId, Session session) {
        if(sessions.containsKey(stationId)){
            List<Session> tmpSessions = sessions.get(stationId);
            tmpSessions.remove(session);
            if(tmpSessions.isEmpty()){
                sessions.remove(stationId);
            }
        }
    }

    @Override
    public void add(String stationId, Session session) {
        if(!sessions.containsKey(stationId)){
            sessions.put(stationId, new ArrayList<>());
        }
        sessions.get(stationId).add(session);
    }

    @Override
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
