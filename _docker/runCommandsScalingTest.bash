# Collector services
java -Dquarkus.http.port=8085 -Dmp.messaging.incoming.mqtt-sensor-data.port=1883 -Dmp.messaging.incoming.mqtt-sensor-data.client-id=eu.collector.mqtt -Dmp.messaging.outgoing.amqp-measurement-records.containerId=eu.collector -jar collector.jar
java -Dquarkus.http.port=8086 -Dmp.messaging.incoming.mqtt-sensor-data.port=1884 -Dmp.messaging.incoming.mqtt-sensor-data.client-id=us.collector.mqtt -Dmp.messaging.outgoing.amqp-measurement-records.containerId=us.collector -jar collector.jar
java -Dquarkus.http.port=8087 -Dmp.messaging.incoming.mqtt-sensor-data.port=1885 -Dmp.messaging.incoming.mqtt-sensor-data.client-id=ca.collector.mqtt -Dmp.messaging.outgoing.amqp-measurement-records.containerId=ca.collector -jar collector.jar

# Web frontend services
java -Dquarkus.http.port=8080 -Dmp.messaging.incoming.measurement-records.containerId=frontend.charlie -jar frontend.jar
java -Dquarkus.http.port=8081 -Dmp.messaging.incoming.measurement-records.containerId=frontend.delta -jar frontend.jar

# Persistence services
java -jar persist.jar
java -jar persist.jar

# Dashboard
bokeh serve --show main.py

# Weather station simulators
java -jar stationSimulator-0.jar 0 127.0.0.1 1883
java -jar stationSimulator-0.jar 3 127.0.0.1 1884
java -jar stationSimulator-0.jar 6 127.0.0.1 1885
