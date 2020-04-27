# Architektur

Die folgende Abbildung zeigt die Architektur des Systems.
Die Wetterstationen generieren Wetteraufzeichnungen und senden diese an den MQTT-Broker.
Der Collector-Service hört auf die Nachrichten, welcher der MQTT-Broker veröffentlicht, konvertiert diese und leitet sie an den AMQP-Broker weiter.
Die Web-Frontend-Services schicken die aktuellsten Wetterdaten der jeweils ausgewählten Station über eine WebSocket-Verbindung an eine beliebige Anzahl an Browser, wobei jede Browser-Verbindung getrennt über eine Session behandelt wird.
Die Persistierung der Wetterdaten in eine PostgreSQL-Datenbank übernehmen die Persistence-Services mittels eines asynchronen JDBC-Treibers, was eine hohe Performance ermöglichen soll.

![Architekturdiagramm](doc/architecture.svg)

## MQTT mit Eclipse Mosqitto

Alexander

### Collector (aus Architektursicht)

- MQTT-QoS Level 2 --> Funktioniert das überhaupt so, dass der Publisher mit QoS 0 sendet und der Consumer mit QoS 2 Nachrichten empfängt?

Alexander

## AMQP mit Apache Artemis

Lukas

- Adressen
- Queues
- Divert

### Bestätigungen

Lukas

### Serialisierungsformat

Lukas

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

### Frontend

Lukas

### Dashboard

### Persistence

Lukas

### Demonstration

- 3 Collector-Services
- 2 Persistence-Services
- 2 Frontend-Services
	- 1 Web-Frontend-Service
	- 1 Dashboard-Service

**Status in Apache Artemis erklären**

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
