ALTER TABLE leituras ADD COLUMN chave_evento VARCHAR(64);

UPDATE leituras
SET chave_evento = 'LEGACY-' || id
WHERE chave_evento IS NULL;

ALTER TABLE leituras ALTER COLUMN chave_evento SET NOT NULL;

CREATE UNIQUE INDEX uq_leituras_chave_evento ON leituras (chave_evento);
