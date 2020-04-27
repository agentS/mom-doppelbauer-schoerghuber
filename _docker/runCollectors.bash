java -Dquarkus.http.port=8081 -Dmp.messaging.incoming.mqtt-sensor-data.port=1883 -jar collector.jar
java -Dquarkus.http.port=8082 -Dmp.messaging.incoming.mqtt-sensor-data.port=1884 -jar collector.jar
java -Dquarkus.http.port=8083 -Dmp.messaging.incoming.mqtt-sensor-data.port=1885 -jar collector.jar
