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

Lukas

Annotationen + Konfiguration (`application.properties`) erklären

# Frontend

Alexander

Annotationen + Konfiguration (`application.properties`) erklären

## React SPA

Alexander

# Persistence

Lukas

AMQP-Receiver-Konfiguration erklären

# Dashboard

Lukas

AMQP-Receiver-Konfiguration erklären
