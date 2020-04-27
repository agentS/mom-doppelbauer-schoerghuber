DROP TABLE measurement;
DROP TABLE station;

CREATE TABLE station(
    id SERIAL PRIMARY KEY NOT NULL,
    name varchar(255) NOT NULL
);

CREATE TABLE measurement(
    station_id INTEGER NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    humidity DOUBLE PRECISION NOT NULL,
    air_pressure DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_measurement_station FOREIGN KEY (station_id) REFERENCES station(id),
    CONSTRAINT pk_measurement PRIMARY KEY(station_id, created_at)
);

INSERT INTO station (name) VALUES ('station1');
INSERT INTO station (name) VALUES ('station2');
INSERT INTO station (name) VALUES ('station3');
INSERT INTO station (name) VALUES ('station4');
INSERT INTO station (name) VALUES ('station5');
INSERT INTO station (name) VALUES ('station6');
INSERT INTO station (name) VALUES ('station7');
INSERT INTO station (name) VALUES ('station8');
INSERT INTO station (name) VALUES ('station9');
INSERT INTO station (name) VALUES ('ESP8266-powered weather station');

COMMIT;
