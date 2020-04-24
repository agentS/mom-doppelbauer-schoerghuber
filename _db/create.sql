CREATE TABLE station(
    id INTEGER PRIMARY KEY,
    name varchar(255)
);

CREATE TABLE measurement(
    stationId INTEGER PRIMARY KEY,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    airPressure DOUBLE PRECISION,
    createdAt TIMESTAMP,
    FOREIGN KEY (stationId) REFERENCES station(id)
);

INSERT INTO station (id, name) VALUES (1, 'station1');
INSERT INTO station (id, name) VALUES (2, 'station2');
INSERT INTO station (id, name) VALUES (3, 'station3');

COMMIT;