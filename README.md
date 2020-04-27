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

### AMQP mit Apache Artemis

Lukas

- Adressen
- Queues
- Divert
- Durability

### Bestätigungen

Lukas

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

- 3 Collector-Services
- 2 Persistence-Services
- 2 Frontend-Services
	- 1 Web-Frontend-Service
	- 1 Dashboard-Service

**Status in Apache Artemis erwähnen**

Lukas

# Wetterstationen

## Simulator

Alexander

## ESP8266-basierte Wetterstation

**Foto einfügen**

Lukas

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
