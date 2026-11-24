CREATE TABLE trabalhadores (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL
);

ALTER TABLE maquinas
    ADD COLUMN limite_superproducao NUMERIC(12, 2),
    ADD COLUMN manutencao_programada_em DATE,
    ADD COLUMN antecedencia_manutencao_dias INTEGER NOT NULL DEFAULT 7,
    ADD COLUMN responsavel_edicao_id BIGINT,
    ADD CONSTRAINT fk_maquinas_responsavel_edicao
        FOREIGN KEY (responsavel_edicao_id) REFERENCES trabalhadores(id);

ALTER TABLE leituras
    ADD COLUMN responsavel_edicao_id BIGINT,
    ADD CONSTRAINT fk_leituras_responsavel_edicao
        FOREIGN KEY (responsavel_edicao_id) REFERENCES trabalhadores(id);

ALTER TABLE alertas
    ADD COLUMN responsavel_id BIGINT,
    ADD COLUMN observacoes TEXT,
    ADD CONSTRAINT fk_alertas_responsavel
        FOREIGN KEY (responsavel_id) REFERENCES trabalhadores(id);
