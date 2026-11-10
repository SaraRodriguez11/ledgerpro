-- V1: Esquema inicial de LedgerPro (contabilidad por partida doble)

CREATE TABLE cuenta (
                        id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        codigo           VARCHAR(10)  NOT NULL,
                        nombre           VARCHAR(120) NOT NULL,
                        tipo             VARCHAR(15)  NOT NULL,
                        cuenta_padre_id  BIGINT       REFERENCES cuenta (id),
                        activa           BOOLEAN      NOT NULL DEFAULT TRUE,
                        creada_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),

                        CONSTRAINT uq_cuenta_codigo UNIQUE (codigo),
                        CONSTRAINT ck_cuenta_tipo CHECK (
                            tipo IN ('ACTIVO', 'PASIVO', 'PATRIMONIO', 'INGRESO', 'GASTO')
                            )
);

CREATE TABLE asiento (
                         id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         fecha        DATE         NOT NULL,
                         descripcion  VARCHAR(255) NOT NULL,
                         estado       VARCHAR(15)  NOT NULL DEFAULT 'CONTABILIZADO',
                         creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),

                         CONSTRAINT ck_asiento_estado CHECK (
                             estado IN ('CONTABILIZADO', 'ANULADO')
                             )
);

CREATE TABLE linea_asiento (
                               id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               asiento_id  BIGINT         NOT NULL REFERENCES asiento (id),
                               cuenta_id   BIGINT         NOT NULL REFERENCES cuenta (id),
                               debito      NUMERIC(19, 2) NOT NULL DEFAULT 0,
                               credito     NUMERIC(19, 2) NOT NULL DEFAULT 0,

                               CONSTRAINT ck_linea_montos_positivos CHECK (debito >= 0 AND credito >= 0),
    -- Cada línea es débito O crédito, nunca ambos ni ninguno
                               CONSTRAINT ck_linea_un_solo_lado CHECK (
                                   (debito > 0 AND credito = 0) OR (credito > 0 AND debito = 0)
                                   )
);

CREATE INDEX idx_linea_asiento ON linea_asiento (asiento_id);
CREATE INDEX idx_linea_cuenta  ON linea_asiento (cuenta_id);