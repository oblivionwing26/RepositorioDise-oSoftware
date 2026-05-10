USE esientradas;

START TRANSACTION;

SET @escenario_nombre = 'Demo Cola';
SET @artista_demo = 'Demo Cola - 1 entrada';
SET @artista_demo_antiguo = 'Demo Cola - 3 entradas';

INSERT INTO escenario (nombre, descripcion)
SELECT @escenario_nombre, 'Escenario de defensa para probar cola virtual'
WHERE NOT EXISTS (
    SELECT 1 FROM escenario WHERE nombre = @escenario_nombre
);

SELECT @escenario_id := id
FROM escenario
WHERE nombre = @escenario_nombre
ORDER BY id
LIMIT 1;

DELETE tc
FROM turno_cola tc
JOIN espectaculo es ON es.id = tc.id_espectaculo
WHERE es.artista IN (@artista_demo, @artista_demo_antiguo);

DELETE dz
FROM de_zona dz
JOIN entrada en ON en.id = dz.id
JOIN espectaculo es ON es.id = en.espectaculo_id
WHERE es.artista IN (@artista_demo, @artista_demo_antiguo);

DELETE en
FROM entrada en
JOIN espectaculo es ON es.id = en.espectaculo_id
WHERE es.artista IN (@artista_demo, @artista_demo_antiguo);

DELETE FROM espectaculo
WHERE artista IN (@artista_demo, @artista_demo_antiguo);

INSERT INTO espectaculo (artista, fecha, escenario_id)
VALUES (@artista_demo, DATE_ADD(NOW(), INTERVAL 7 DAY), @escenario_id);

SET @espectaculo_id = LAST_INSERT_ID();

INSERT INTO entrada (precio, estado, espectaculo_id) VALUES (2500, 'DISPONIBLE', @espectaculo_id);
SET @entrada_1 = LAST_INSERT_ID();
INSERT INTO de_zona (id, zona) VALUES (@entrada_1, 1);

COMMIT;

SELECT @espectaculo_id AS id_espectaculo_demo;
