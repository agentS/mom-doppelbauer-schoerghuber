# Architektur

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

### Collector

Lukas

### Frontend

Lukas

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
