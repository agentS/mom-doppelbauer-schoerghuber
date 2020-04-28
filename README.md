# Architektur

Die folgende Abbildung zeigt die Architektur des Systems.
Die Wetterstationen generieren Wetteraufzeichnungen und senden diese an den MQTT-Broker.
Der Collector-Service hört auf die Nachrichten, welcher der MQTT-Broker veröffentlicht, konvertiert diese und leitet sie an den AMQP-Broker weiter.
Die Web-Frontend-Services schicken die aktuellsten Wetterdaten der jeweils ausgewählten Station über eine WebSocket-Verbindung an eine beliebige Anzahl an Browser, wobei jede Browser-Verbindung getrennt über eine Session behandelt wird.
Das Dashboard bietet eine Übersicht über die aktuellsten Wetterdaten einer Station.
Die Persistierung der Wetterdaten in eine PostgreSQL-Datenbank übernehmen die Persistence-Services mittels eines asynchronen JDBC-Treibers, was eine hohe Performance ermöglichen soll.

![Architekturdiagramm](doc/architecture.png)

## MQTT mit Eclipse Mosqitto

Alexander

### Collector (aus Architektursicht)

- MQTT-QoS Level 2 --> Funktioniert das überhaupt so, dass der Publisher mit QoS 0 sendet und der Consumer mit QoS 2 Nachrichten empfängt?

Alexander

## AMQP

AMQP 1.0 stellt im Vergleich zu AMQP 0-9-1 ein komplett verändertes Protokoll dar, welches überhaupt nicht mehr kompatibel ist.
Die Hauptunterschiede umgeben sich aus dem Umfang der Protokolle.
AMQP 0-9-1 definiert neben dem Binärprotokoll noch die Broker-Architektur, was zur Folge hat, dass sich Unterstützung für AMQP 0-9-1 nur bedingt in bestehende Broker integrieren lässt.
AMQP 1.0 definiert lediglich das Binärprotokoll und ist von den Konzepten wie Brokern, Exchanges, Bindings, Queues unabhängig, obwohl es diese weiterhin geben kann, allerdings implementierungsabhängig.
Deshalb bietet AMQP 1.0 ebenfalls keine Befehle zum Verwalten des Brokers, wie z.B. das Definieren und Löschen von Queues.

AMQP 1.0 geht dabei sogar so weit, dass es nicht einmal die Existenz von Brokern vorschreibt.
Zwei Clients können auch direkt miteinander ohne Broker kommunizieren.
Dies erlaubt die Erstellung von Zwischenkomponenten, die keine Broker im eigentlichen Sinn sind.
Ein Beispiel hierfür ist der [Apache Qpid Dispatch Router](https://qpid.apache.org/components/dispatch-router/).

Da viele Implementierungen mittlerweile nur noch AMQP 1.0 unterstützen, wie z.B. MicroProfile Reactive Messaging, für welches Quarkus die [Implementierung von SmallRye](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/index.html) verwendet, haben wir uns entschieden, diese Aufgabe mit AMQP 1.0 umzusetzen.

AMQP 1.0 definiert die folgenden Komponenten

- Eine Verbindung ist eine vollduplex, verlässliche, geordnete Sequenz von Frames, die über den Kommunikationskanal übertragen werden.
- Eine Session fasst eine beliebige Anzahl von unabhängigen Links zusammen. Die Session übernimmt dabei den Sequenz- und Kontrollfluss auf Frameübertragungsebene.
- Ein Link ist eine unidirektionale Route zwischen einer Quelle und einem Ziel, über welche die Nachrichten übertragen werden. Ein Link übernimmt dabei die Flusskontrolle auf Nachrichtenebene.

Aus Perspektive der AMQP-Clients sind die Namen des Quell- und Zielobjekts Bezeichner aus dem Namensraum des Verbindungspartners.
So sind diese für unser Projekt Bezeichner aus dem Namesraum des Apache-Artemis-Brokers, nämlich Addressen und Queues.

### AMQP mit Apache ActiveMQ Artemis

Apache ActiveMQ Artemis ist ein hochperformantes, asynchrones Open-Source-Messaging-System, welches eine Vielzahl an Protokollen unterstützt und Clusterfähigkeiten bietet.
Artemis unterstützt dabei zwei Arten von Messaging: Message-Queue-Messaging, welches auch als Punkt-zu-Punkt-Messaging bezeichnet wird, und Publish-Subscribe-Messaging.

Bei Punkt-zu-Punkt-Messaging wird die Nachricht an genau einen der Consumer weitergeleitet, welcher die Nachricht abarbeitet und bestätigt oder ablehnt.
Bei einer Bestätigung wird die Nachricht aus der Queue entfernt, während sie bei einer Ablehnung wieder in die Queue gestellt wird und somit später verarbeitet werden kann.
Da eine Nachricht nur einmal an einen Consumer zugestellt wird, ist sichergestellt, dass die Nachricht nicht mehrmals verarbeitet wird.
Deshalb wird Punkt-zu-Punkt-Messaging zur Verteilung von Nachrichten an mehrere Persistence-Services eingesetzt.

Das Publish-Subscribe-Model entspricht Topics in MQTT und JMS: Jede der beliebig vielen Subscriptions für ein Topic erhält eine Kopie jeder Nachricht zugestellt.
Darüber hinaus kann zusätzlich die Durable-Eigenschaft für eine Queue aktiviert werden, die bewirkt, dass eine Kopie einer Nachricht bis zur Zustellung an den Subscriber im Broker gespeichert wird, wodurch Offline-Zeiten der Subscriber (z.B. durch Wartung oder Versionsupgrades) überbrückt werden können.
Das Publish-Subscribe-Modell wird für die Fronten-Services (Web-Frontend und Dashboard) verwendet, da diese einen Zugriff auf die Wetterdaten aller Wetterstationen ermöglichen sollen.
Die Queues des Dashboard sind als durable gekennzeichnet, um auch Wetterdaten, die während einer Offline-Zeit einer Dashboard-Instanz eingehen, anzeigen zu können.
Da das Web-Frontend immer nur die aktuellsten Werte anzeigen soll und Wetterdaten, die während eines Offline-Fensters eingehen, als veraltet gelten, werden wird der Non-Durable-Mode verwendet.

Das Adressmodell von Apache ActiveMQ Artemis, welches aufgrund der erhöhten Flexibilität in AMQP 1.0 auch auf die Clients angewandt werden kann, besteht aus drei wesentlichen Komponenten: Adressen, Queues und Routing-Types.

Eine Adresse repräsentiert einen Messaging-Endpunkt und verfügt über einen eindeutigen Namen, beliebig viele zugeordnete Queues und einen Routing-Type.
Ein Producer sendet die Nachrichten an eine Adresse, welche dann an die Queues weitergeroutet werden.

Eine Queue ist mit einer Adresse asoziiert, wobei es beliebig viele Queues pro Adresse geben kann.
Sobald eine Nachricht an eine Adresse angelangt ist, wird sie abhängig vom Routing-Type an die Queues weitergeleitet.

Der Routing-Type entscheidet, wie für eine Adresse eingegange Nachrichten an die mit der Adresse assoziierten Queues weitergeleitet werden.
ActiveMQ Artemis kennt dabei zwei verschiedene Routing-Typen: Anycast, welcher dem Punkt-zu-Punkt-Messaging-Modell entspricht, und Multicast, welcher dem Publish-Subscribe-Messaging-Modell entspricht.
Bei Anycast wird die Nachricht also nur an eine der asoziierten Queues weitergeleitet, während sie bei Multicast an alle Queues verteilt wird.
Für weitere Details hierzu sei [auf die sehr gute Dokumentation verwiesen](https://activemq.apache.org/components/artemis/documentation/latest/address-model.html)-

Nun ergibt sich das Problem, dass für eine Adresse zwei verschiedene Routing-Types verwendet werden sollen, da der Persistence-Service andere Anforderungen als die Frontend-Services haben.
Während dies z.B. für JMS möglich ist, da es möglich ist, sowohl eine Queue als auch ein Topic mit gleichem Namen zu haben und durch die konzeptionelle Trennung unterschieden wird, ist dies für AMQP 1.0 nicht mehr möglich.
Laut Dokumentation wird sich ein Client bei der Verwendung von beiden Routing-Types mit AMQP 1.0 standardmäßig auf die Anycast-Queue verbinden, was sich in Tests auch bewahrheitet hat.
Für Details hierzu [siehe den Abschnitt "Point-to-Point and Publish-Subscribe Addresses" in der Dokumentation](https://activemq.apache.org/components/artemis/documentation/latest/address-model.html).

Eine Möglichkeit wäre gewesen, den Collector-Service zu erweitern, was jedoch der losen Kopplung, die mit einem Message-Queueing-Protokoll erreicht werden soll, widerspricht.
Daher haben wir uns entschieden, für den Anycast-Betrieb eine zusätzliche Adresse zu definieren und mittels eines [Diverts](https://activemq.apache.org/components/artemis/documentation/latest/diverts.html) den Nachrichtenfluss für die ursprüngliche Adresse so aufzuteilen, dass Nachrichten, die für die ursprüngliche Adresse bestimmt sind weiterhin an diese zugestellt werden und zusätzlich an die neue Adresse für den Anycast-Betrieb kopiert werden.
Hierfür wird die Semantik eines nichtexklusiven Diverts verwendet.
Da wir einige Stunden zum Finden der Lösung verbracht haben, zeigt sich hier ein Nachteil in der nichtstandardisierten Broker-Konfiguration von AMQP 1.0 im Vergleich zu AMQP 0-9-1.

Abschließend zeigt das folgende Snippet noch die Konfiguration des Apache ActiveMQ Artemis-Brokers für AMQP.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<configuration xmlns="urn:activemq" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:activemq /schema/artemis-configuration.xsd">
	<core xmlns="urn:activemq:core" xsi:schemaLocation="urn:activemq:core ">
		<addresses>
			<address name="measurement-records">
				<multicast />
			</address>
			<address name="measurement-records-persistence">
				<anycast>
					<queue name="measurement-records-persistence" />
				</anycast>
			</address>
		</addresses>
		<diverts>
			<divert name="measurement-records-persistence-forwarding">
				<address>measurement-records</address>
				<forwarding-address>measurement-records-persistence</forwarding-address>
				<exclusive>false</exclusive>
			</divert>
		</diverts>
	</core>
</configuration>
```

### Bestätigungen

Das Festlegen, welche Nachrichten auf Sender- und Empfänger-Seite ob und wie bestätigt werden, ist ein essentieller Teil eines Messaging-Protokolls.
AMQP definiert zur Festlegung der Zuverflässigkeits- und Performanceanforderungen zwei verschiedene QoS-Stufen (Quality-of-Service) sowohl auf Sender- als auch auf Empfängerseite.
Für beide Seiten sind die folgenden QoS-Level definiert:

- At-most-Once (Level 0)
- At-least-Once (Level 1)

Auf Publisher-Seite bedeutet Level 0, dass der Publisher nicht auf eine Bestätigung vom anderen Endpunkt wartet, bevor die Nachricht verworfen wird, wohingegen bei Level 1 der Publisher eine Nachricht nur nach erhaltener Besätigung aus der Queue entfernt.
Auf Subscriber-Seite hat der QoS-Level 0 zur Folge, dass der Queue-Manager nach dem Senden einer Nachricht diese sofort aus der Queue entfernt, egal ob eine Besätigung eintrifft oder nicht, während bei QoS-Level 1 das Entfernen einer Nachricht aus der Queue nur nach Erhalt einer Bestätigung durch den Subscriber erfolgt.

Da Wetterdaten, welche der Collector über MQTT empfängt nicht verloren gehen sollen, verwendet der Collector auf AMQP-Seite QoS-Level 1.
Ebenso soll gewährleistet werden, dass Wetterdaten verlässlich in der Datenbank gespeichert werden, weswegen der Persistence-Service ebenfalls QoS-Level 1 verwendet.
Wenn auf Seiten des Web-Frontends oder des Dashboards Nachrichten verloren gehen, hat dies keine wesentlichen Auswirkungen und daher arbeiten diese Systeme mit QoS-Level 0, um die Performance zu verbessern.

### Serialisierungsformat

Wir haben versucht das in [AMQP 1.0 definierte Typsystem](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol#Type_system) zu verwenden.
Leider ist die Quarkus-Implementierung von MicroProfile Reactive Messaging, welche im Collector-Service zum Einsatz kommt, nicht in der Lage, Java-Objekte in entsprechende AMQP-Datentypen zu konvertieren.
Für diese wird beim Erstellen von Nachrichten einfach die Methode `toString` aufgerufen.
Eine Alternative wäre gewesen, eine [Library, welche die Serialisierung vornimmt](https://github.com/xinchen10/amqp-io) zu verwenden.
Allerdings führt dieser Ansatz oft mit neueren Datentypen (z.B. das in Java 8 hinzugekommene `LocalDateTime`) zu Problemen.
Weiters ist so eine Serialisierung zusätzlicher Code-Aufwand, welcher einen Cross-Cutting-Concern darstellt.
Bei solchen Anforderungen wäre es aus unserer Sicht besser, ein weiter verbreitetes Serialiserungsprotokoll wie [ProtocolBuffers](https://developers.google.com/protocol-buffers/) oder [Cap'n Proto](https://capnproto.org/) zu verwenden.

## Skalierbarkeit

Die Architektur ist so ausgelegt, dass eine Skalierung durch Hoch- und Runterfahren zusätzlicher Instanzen der Komponenten erreicht werden kann.
Im Folgenden wird auf die Skalierbarkeit der einzelnen Komponenten eingegangen und abschließend die Ergebnisse einer kleinen Demonstration festgehalten.

### Collector

Für jeden zusätzlichen Collector muss ein zusätzlicher MQTT-Broker betrieben werden.
Dies ergibt sich daraus, dass MQTT nur Topics besitzt und daher die Nachrichten nur im Multicast-Verfahren an alle Teilnehmer gesendet werden.
Würden an einem MQTT-Broker mehrere Collector sich auf Nachrichten registrieren, würde daher jeder Collector jede Nachricht erhalten und diese an den AMQP-Broker weiterleiten.
Dies würde zu vielen duplizierten Nachrichten führen, welche sich stark negativ auf die Performance auswirken.

Aus diesen Gründen haben wir uns für die Architektur mit genau einem Collector pro Broker entschieden.
Da die MQTT-Broker ebenfalls relativ leichtgewichtig sind, ist dies auch kein allzu großes Problem.

### Frontend und Dashboard

Da Nachrichten von der AMQP-Queue aufgrund des in Apache Artemis konfigurierten Multicast-Verhalten an alle Frontend-Services (Web-Frontend und Dashboard) weitergeleitet werden, können von diesen je nach Last die benötigte Anzahl an Instanzen gestartet und gestoppt werden.
Dadurch können Lastspitzen sehr gut abgedeckt werden.

### Persistence

Die Persistence-Services verwenden eine Queue, die von Apache Artemis intern als Anycast-Queue verwaltet wird.
Dadurch wird eine Nachricht immer nur an einen Persistence-Service weitergeleitet, wodurch die Last auf mehrere Services aufgeteilt werden kann.
Duplikate werden dadurch vermieden.

### Demonstration

Im Folgenden wird nun demonstriert, wie mehrere Instanzen eines jeden Services betrieben werden können.
In der folgenden Demonstration wird ebenfalls auf die Funktionalität der einzelnen Komponenten eingegangen.

- 3 Collector-Services
- 2 Persistence-Services
- 2 Frontend-Services
	- 1 Web-Frontend-Service
	- 1 Dashboard-Service

**Status in Apache Artemis erwähnen**

Lukas

# Wetterstationen

## Nachrichtenformat

Alexander

CSV-Format und Topic-Namen beschreiben

## Simulator

Alexander

## ESP8266-basierte Wetterstation

Die unten folgende Abbildung zeigt ein Foto der hardwarebasierten Wetterstation.
Diese verwendet einen DHT22-Sensor zur Messung der Temperatur und der Luftfeuchtigkeit sowie einen BME280-Sensor zur Messung des Luftdrucks.
Der ESP8266-Mikrocontroller wird in MicroPython, einem Python-Dialekt für Embedded-Systems, angesteuert.
Der [Quellcode für die Wetterstation findet sich natürlich in unserem Repository](weatherStationMicropython/weatherStation.py).

![ESP8266-basierte Wetterstation](doc/weatherStation.jpg)

Einer der großen Vorteile von MicroPython ist die für Mikrocontroller umfangreiche Bibliothek, welche neben einem Treiber für den DHT22-Sensor auch eine minimale MQTT-Implementierung mitbringt.
Über diese werden die Messages an den jeweiligen Eclipse Mosquitto-Broker gesendet.
Die MQTT-Implementierung unterstützt die QoS-Stufen 0 und 1, wobei standardmäßig Stufe 0 aktiviert ist.
Dies ist für die Zwecke einer simplen Wetterstation völlig ausreichend und da sich daraus die geringsten Anforderungen an die Hardware ergeben, verwenden wir auch diese Variante.

# Collector

Die Aufgabe des Collector-Services ist es, sich als Subscriber für Wetterdaten, die über MQTT übertragen werden, zu registrieren, dies zu konvertieren und sie anschließend an eine AMQP-Adresse weiterzuleiten.
Der Collector-Service ist mit [Quarkus](https://quarkus.io/) und mit dessen Implementierung von [MicroProfile Reactive Messaging 1.0](https://download.eclipse.org/microprofile/microprofile-reactive-messaging-1.0/microprofile-reactive-messaging-spec.pdf), welche auf der [SmallRye-Implementierung](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/) beruht, realisiert.

## Design

MicroProfile Reactive Messaging abstrahiert Nachrichten von mehreren Message-Queueing-Technologien (momentan MQTT, AMQP 1.0 und Apache Kafka) in Streams und ermöglicht es, CDI-Beans, welche die Nachrichten eines Stream verarbeiten, zu definieren.
Messages werden innerhalb einer Applikation über Channels übertragen.
Ein Channel ist ein virtuelles Ziel, welches mit einem Namen identifiziert wird.
Die MicroProfile Reactive Messaging-Implementierung stellt anschließend eine Verbindung zwischen Channels und CDI-Beans, welche die Nachrichten eines eingehenden Channels verarbeiten und/oder neue Nachrichten auf einen ausgehenden Channel senden, her.
Eine gute Illustration zeigt das folgende Bild aus der Dokumentation von SmallRye Reactive Messaging.

![Relation von CDI-Beans und Channels](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/_images/channels.png)

Ein Connector stellt anschließend die Verbindung zwischen einem Chanell und einem Message-Broker her.
Dabei werden von einem Broker gesendete Nachrichten auf einen eingehenden Kanal gemappt und von einem Kanal ausgehende Nachrichten gesammelt an einen Broker geschickt.
Der Broker kann es sich dabei entweder eine MQTT-, eine AMQP- oder eine Kafka-Instanz handeln.
Dies wird wiederum in der SmallRye-Dokumentation sehr anschaulich illustriert:

![Connectors stellen Verbindungen zwischen Channels und Message-Brokern her](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/_images/connectors.png)

Eine Nachricht setzt sich in der MicroProfile Reactive Messaging-Spezifikation aus Metadaten und Payload zusammen und werden grundsätzlich durch die Klasse `Message<T>` repräsentiert.
Die Spezifikation legt dabei fest, dass auch Broker-abhängige Metadaten, wie z.B. das Topic in MQTT, über spezielle Klassen, welche das Interface `Message<T>` erweitern und von der Implementierung beisteuert werden, zugegriffen werden kann.
Ein Beispiel ist die Klasse `MqttMessage<T>` [der SmallRye-Implementierung](https://github.com/smallrye/smallrye-reactive-messaging/blob/master/smallrye-reactive-messaging-mqtt/src/main/java/io/smallrye/reactive/messaging/mqtt/MqttMessage.java), welche eine MQTT-Nachricht repräsentiert und über die auf das Topic zugegriffen werden kann.

## Implementierung

Die wichtigste Komponente ist das CDI-Bean `MqttCollector`, dessen Quellcode im unteren Snippet gezeigt wird.
Die Spezifikation MicroProfile Reactive Messaging legt fest, dass Beans, welche einen Stream von Nachrichten verarbeiten, entweder mit `@ApplicationScoped`.
Um die ID der Wetterstation aus dem Topic der MQTT-Nachricht extrahieren zu können, wird der Topic-Präfix als Konfigurationsparameter injiziert.
Die Implementierung nimmt dabei Mqtt-Nachrichten aus dem Channel `mqtt-sensor-data` entgegen, was durch die Annotation `@Incoming("mqtt-sensor-data")` festgelegt wird, verarbeitet diese und leitet sie an den Channel `amqp-measurement-records` weiter, was die Annotation `@Outgoing("amqp-measurement-records")` bewirkt.
Die Annotation `@Acknowledgment(Acknowledgment.Strategy.MANUAL)` stellt den Zeitpunkt des Sendens der Bestätigung für die empfangene Nachricht vom standardmäßigen Bestätigen vor Ausführung der annotierten Methode auf Bestätigung durch den Implementierer um.
Dadurch kann erreicht werden, dass Nachrichten erst nach der wirklichen Verarbeitung besätigt werden.
Hierdurch wird auch für die ausgehenden Nachrichten das AMQP-QoS-Level 1 automatisch durch die Implementierung festgelegt.
Abschließend legt die Annotation `@Broadcast` noch fest, dass ausgehende Nachrichten an alle Subscriber dispatcht werden.

Das Besätigen der eingehenden Nachrichten erfolgt über ein Idiom, welches in der Spezifikation zu [MicroProfile Reactive Messaging 1.0](https://download.eclipse.org/microprofile/microprofile-reactive-messaging-1.0/microprofile-reactive-messaging-spec.pdf) beschrieben wird (Seite 27): Die eingehende Nachricht wird mittels eines Callbacks erst dann bestätigt, nachdem die ausgehende Nachricht versendet wurde.

```java
@ApplicationScoped
public class MqttCollector {
	private final String topicPrefix;

	@Inject
	public MqttCollector(@ConfigProperty(name = "mqtt.topic-prefix") String topicPrefix) {
		this.topicPrefix = topicPrefix;
	}

	@Incoming("mqtt-sensor-data")
	@Outgoing("amqp-measurement-records")
	@Acknowledgment(Acknowledgment.Strategy.MANUAL)
	@Broadcast
	public Message<String> processMeasurement(MqttMessage<byte[]> message) {
		long weatherStationId = MqttMessageParser.parseWeatherStationId(message.getTopic(), this.topicPrefix);
		String payload = new String(message.getPayload());
		MeasurementDto measurement = MqttMessageParser.parseMeasurementCsv(payload);
		LocalDateTime timestamp = LocalDateTime.now()
				.truncatedTo(ChronoUnit.SECONDS);
		RecordDto record = new RecordDto(weatherStationId, timestamp, measurement);
		return Message.of(
				RecordJsonConverter.toJson(record).toString(),
				message::ack
		);
	}
}
```

## Konfiguration

Die Connectoren, welche die Channels mit den Brokern verbinden, werden in der Konfiguration der Anwendung festgelegt.
Die möglichen Eigenschaften sind dabei wieder implementierungsabhängig (siehe Dokumentation zu [AMQP](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/amqp/amqp.html) und [MQTT](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/mqtt/mqtt.html) in SmallRye Reactive Messaging).
Im ersten Block wird der eingehende Channel `mqtt-sensor-data` mit allen MQTT-Topics, die mit der Bezeichnung `sensor-data` beginnen und anschließend eine beliebige Stations-ID als zweiten Bestandteil haben verbunden.
Darüber hinaus wird das QoS-Level noch entsprechend gesetzt und eine Client-ID zur besseren Identifizierung am Broker festgelegt.

Der zweite Block legt die grundlegenden Eigenschaften zur Verbindung mit dem AMQP-Broker fest, während der dritte Block die Verbindung zwischen dem ausgehenden Channel `amqp-measurement-records` und der AMQP-Adresse `measurement-records` festlegt.
Die Festlegung der Container-ID dient wieder zur besseren Identifizierbarkeit am AMQP-Broker, da ansonsten eine UUID verwendet wird.
Wichtig ist noch, dass festgelegt wird, dass die Queues durable sind, also auch bei Verbindungsabrissen bestehen bleiben.

```properties
mp.messaging.incoming.mqtt-sensor-data.client-id = eu.collector.mqtt
mp.messaging.incoming.mqtt-sensor-data.auto-generated-client-id = false
mp.messaging.incoming.mqtt-sensor-data.type = smallrye-mqtt
mp.messaging.incoming.mqtt-sensor-data.topic = sensor-data/+
mp.messaging.incoming.mqtt-sensor-data.host = 127.0.0.1
mp.messaging.incoming.mqtt-sensor-data.port = 1883
mp.messaging.incoming.mqtt-sensor-data.qos = 2

# set the AMQP broker credentials
amqp-host = 127.0.0.1
amqp-port = 5672
amqp-username = weatherdata
amqp-password = thunderstorm

# configure the AMQP connector to write to the `persisted-sensor-values` address
mp.messaging.outgoing.amqp-measurement-records.connector = smallrye-amqp
mp.messaging.outgoing.amqp-measurement-records.address = measurement-records
mp.messaging.outgoing.amqp-measurement-records.containerId = collector
mp.messaging.outgoing.amqp-measurement-records.durable = true

mqtt.topic-prefix = sensor-data/
```

# Frontend

Alexander

## Design

## Implementierung

Annotationen + Konfiguration (`application.properties`) erklären

## React SPA

Alexander

# Persistence

Der Zweck des Persistence-Services ist es, die eingehenden Wetternachrichten in einer PostgreSQL-Datenbank abzuspeichern.
Um auch andere Implementierungen als das in Quarkus verwendete SmallRye Reactive Messaging zu testen, ist der Persistence-Service mit dem Toolkits [Vert.x](https://www.vertx.io/) implementiert.

## Design

Vert.x ist ausschließlich auf asynchrone Kommunikation innerhalb der Anwendung ausgelegt und bietet asynchrone Libraries für [AMQP](https://vertx.io/docs/vertx-pg-client/java/) und [PostgreSQL](https://vertx.io/docs/vertx-amqp-client/java/).
Um die Funktionalitäten des Empfangens von AMQP-Nachrichten und des Persistierens der Wetterdaten zu trennen, wurden beide Funktionalitäten als eigenständige Komponenten (**Verticles** in Vert.x) realisiert, die mittels eines Request-Response-Patterns über den von Vert.x intern verwendeten Event-Bus miteinander kommunizieren.
Das Verticle zum Empfang von AMQP-Nachrichten extrahiert bei einer eingehenden Nachricht das JSON-Objekt aus der Nachricht, sendet einen Request an das Datenbank-Persistence-Verticle und bestätigt je nach Resultat die erfolgreiche oder fehlgeschlagene Verarbeitung der Nachricht.

### Behandlung von Duplikaten

Um Duplikate zu vermeiden, sind für ein Wettermessdatum sowohl die ID der Wetterstation als auch der Zeitstempel as Primärschlüssel definiert.
Auf Seite des Persistence-Services wird eine Transaktion zum Einfügen verwendet.
Tritt eine Verletzung des Primärschlüssels auf, wird diese erkannt, die Transaktion zurückgerollt und dieser Fall mittels eines speziellen Responses an das AMQP-Verticle signalisiert.
Das AMQP-Verticle bestätigt im Falle eines Duplikates die Verarbeitung der Nachricht dennoch an den AMQP-Broker, da davon ausgegangen ist, dass es sich um eine versehentlich doppelt zugestellte Nachricht handelt, die nun korrekt behandelt wurde.

Tritt eine andere Ausnahme beim Einfügen eines Wetterdatums ein, wird eine Rejection an den AMQP-Broker gesendet.

## Implementierung und Konfiguration

In der Methode `start` des AMQP-Consumer-Verticles wird eine Verbindung zum AMQP-Broker aufgebaut, wobei die Werte aus der Konfigurationsdatei übernommen werden.
Die Container-ID dient dabei wieder dazu, den Consumer besser am AMQP-Broker identifizieren zu können.
Für den AMQP-Receiver (Subscriber) wird festgelegt, dass die Nachrichten nicht automatisch von der Vert.x-Implementierung des AMQP-Clients bestätigt werden sollen.
Darüber hinaus wird festgelegt, dass die Durable-Eigenschaft gesetzt sein soll, Nachrichten also nach einer Offline-Periode des Subscribers erneut zugestellt werden sollen.
Ebenfalls wird das QoS-Level auf 1 gesetzt und die Anzahl der Nachrichten, die der Subscriber maximal puffern kann, begrenzt, wobei nur der Vert.x-AMQP-Client eine so feingranulare Konfiguration erlaubt.

Die Methode `handleWeatherRecordMessage` extrahiert das JSON-Objekt aus der Nachricht, schickt einen Request an das PostgreSQL-Persitence-Verticle und bestätigt oder rejected die AMQP-Nachricht abhängig vom Response des PostgreSQL-Persitence-Verticles.

```java
public class AmqpConsumerVerticle extends AbstractVerticle {
	// ...

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		this.eventBus = this.vertx.eventBus();
		var amqpConfiguration = new AmqpClientOptions()
			.setHost(this.config().getString("hostname"))
			.setPort(this.config().getInteger("port"))
			.setUsername(this.config().getString("username"))
			.setPassword(this.config().getString("password"))
			.setContainerId(this.config().getString("containerId"));
		this.amqpClient = AmqpClient.create(this.vertx, amqpConfiguration);
		this.amqpClient.connect(connectionResult -> {
			if (connectionResult.failed()) {
				startFuture.fail(connectionResult.cause());
			} else {
				AmqpConnection amqpConnection = connectionResult.result();
				var amqpReceiverOptions = new AmqpReceiverOptions()
					.setAutoAcknowledgement(false)
					.setDurable(true)
					.setQos("AT_LEAST_ONCE")
					.setMaxBufferedMessages(this.config().getInteger("maxBufferedMessages"));
				amqpConnection.createReceiver(
					this.config().getString("queueName"),
					amqpReceiverOptions,
					receiverCreationResult -> {
						if (receiverCreationResult.failed()) {
							startFuture.fail(receiverCreationResult.cause());
						} else {
							AmqpReceiver receiver = receiverCreationResult.result();
							receiver
								.exceptionHandler(this::handleWeatherRecordMessageException)
								.handler(this::handleWeatherRecordMessage);
							startFuture.complete();
							System.out.println("AMQP receiver listening to queue " + this.config().getString("queueName"));
						}
					}
				);
			}
		});
	}

	private void handleWeatherRecordMessage(AmqpMessage amqpMessage) {
		var body = new JsonObject(amqpMessage.bodyAsString());
		this.eventBus.send(
			EventBusAddresses.PERSISTENCE_POSTGRESQL,
			body,
			response -> {
				if (response.failed()) {
					ReplyException exception = ((ReplyException) response.cause());
					if (exception.failureCode() != PostgresqlPersistenceVerticle.FAILURE_CODE_DUPLICATED_RECORD) {
						amqpMessage.rejected();
						response.cause().printStackTrace(System.err);
					} else {
						amqpMessage.accepted();
						System.out.println("Duplicated record not inserted for " + body);
					}
				} else {
					amqpMessage.accepted();
					System.out.println("Inserted record for " + body);
				}
			}
		);
	}

	// ...
}
```

# Dashboard

Lukas

## Design

## Implementierung und Konfiguration

AMQP-Receiver-Konfiguration erklären

## Testfälle
