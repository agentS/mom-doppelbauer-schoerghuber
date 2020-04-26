CREATE TABLE station(
    id INTEGER PRIMARY KEY NOT NULL,
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

INSERT INTO station (id, name) VALUES (1, 'station1');
INSERT INTO station (id, name) VALUES (2, 'station2');
INSERT INTO station (id, name) VALUES (3, 'station3');

COMMIT;
