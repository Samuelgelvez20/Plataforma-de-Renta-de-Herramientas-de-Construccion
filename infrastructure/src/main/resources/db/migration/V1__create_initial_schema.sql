-- V1__create_initial_schema.sql
-- Plataforma de Renta de Herramientas y Equipos de Construcción
-- Jurisdicción: Colombia, Moneda: COP, IVA configurable
-- PostgreSQL 16 compatible

-- 1. EXTENSIONES
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. ENUMS
CREATE TYPE USER_ROL AS ENUM ('ADMIN', 'PROPIETARIO', 'CLIENTE');
CREATE TYPE RESERVA_ESTADO AS ENUM ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA', 'EXPIRADA');
CREATE TYPE PAGO_ESTADO AS ENUM ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO');
CREATE TYPE HERRAMIENTA_PUB_ESTADO AS ENUM ('PUBLICADA', 'PAUSADA', 'RETIRADA');
CREATE TYPE FACTURA_TIPO AS ENUM ('FACTURA', 'NOTA_CREDITO');
CREATE TYPE DANO_SEVERIDAD AS ENUM ('LEVE', 'MODERADO', 'GRAVE', 'TOTAL');

-- 3. CONFIGURACIÓN FISCAL
CREATE TABLE configuracion_fiscal (
    clave VARCHAR(50) PRIMARY KEY,
    valor VARCHAR(100) NOT NULL,
    descripcion TEXT,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO configuracion_fiscal (clave, valor, descripcion) VALUES
    ('iva_porcentaje', '19.00', 'IVA general Colombia'),
    ('moneda', 'COP', 'Moneda base'),
    ('reserva_pendiente_expiracion_minutos', '30', 'Expiración automática PENDIENTE'),
    ('factura_serie_prefijo', 'F', 'Prefijo numeración interna'),
    ('factura_correlativo_inicio', '1', 'Correlativo inicial');

-- 4. USUARIO Y PERFILES
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol USER_ROL NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_usuario_rol ON usuario(rol);

CREATE TABLE propietario (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    razon_social VARCHAR(200) NOT NULL,
    nit VARCHAR(20) NOT NULL UNIQUE,
    direccion_fiscal TEXT NOT NULL,
    telefono_contacto VARCHAR(20),
    regimen VARCHAR(30) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    verificado_en TIMESTAMPTZ
);

CREATE TABLE cliente (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    direccion_entrega_default TEXT,
    datos_facturacion JSONB
);

-- 5. CATÁLOGO
CREATE TABLE categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE herramienta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_id UUID NOT NULL REFERENCES categoria(id) ON DELETE RESTRICT,
    propietario_id UUID NOT NULL REFERENCES propietario(usuario_id) ON DELETE RESTRICT,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    marca VARCHAR(50),
    modelo VARCHAR(50),
    precio_dia NUMERIC(14,2) NOT NULL CHECK (precio_dia >= 0),
    cantidad_total INTEGER NOT NULL CHECK (cantidad_total > 0),
    estado_publicacion HERRAMIENTA_PUB_ESTADO NOT NULL DEFAULT 'PUBLICADA',
    iva_porcentaje NUMERIC(5,2) NOT NULL DEFAULT 19.00 CHECK (iva_porcentaje >= 0),
    iva_aplica BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_herramienta_propietario ON herramienta(propietario_id);
CREATE INDEX idx_herramienta_categoria ON herramienta(categoria_id);
CREATE INDEX idx_herramienta_estado_pub ON herramienta(estado_publicacion);

-- 6. RESERVA
CREATE TABLE reserva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente(usuario_id) ON DELETE RESTRICT,
    estado RESERVA_ESTADO NOT NULL DEFAULT 'PENDIENTE',
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_confirmacion TIMESTAMPTZ,
    fecha_expiracion_pendiente TIMESTAMPTZ,
    fecha_cancelacion TIMESTAMPTZ,
    motivo_cancelacion TEXT,
    cancelado_por_usuario_id UUID REFERENCES usuario(id),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reserva_fechas CHECK (fecha_fin >= fecha_inicio)
);
CREATE INDEX idx_reserva_cliente ON reserva(cliente_id);
CREATE INDEX idx_reserva_estado ON reserva(estado);
CREATE INDEX idx_reserva_expiracion ON reserva(fecha_expiracion_pendiente) WHERE estado = 'PENDIENTE';

-- 7. DETALLE_RESERVA
CREATE TABLE detalle_reserva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL REFERENCES reserva(id) ON DELETE CASCADE,
    herramienta_id UUID NOT NULL REFERENCES herramienta(id) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_dia_acordado NUMERIC(14,2) NOT NULL CHECK (precio_dia_acordado >= 0),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    iva NUMERIC(14,2) NOT NULL CHECK (iva >= 0),
    total NUMERIC(14,2) NOT NULL CHECK (total >= 0),
    iva_porcentaje NUMERIC(5,2) NOT NULL CHECK (iva_porcentaje >= 0),
    iva_aplica BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_detalle_reserva_reserva ON detalle_reserva(reserva_id);
CREATE INDEX idx_detalle_reserva_herramienta ON detalle_reserva(herramienta_id);

-- 8. FUNCIONES AUXILIARES
CREATE OR REPLACE FUNCTION dias_inclusivos(p_inicio DATE, p_fin DATE) RETURNS INTEGER AS $$
BEGIN
    RETURN (p_fin - p_inicio) + 1;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION calcular_disponibilidad_herramienta(
    p_herramienta_id UUID,
    p_fecha_inicio DATE,
    p_fecha_fin DATE,
    p_excluir_reserva_id UUID DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    v_total INT;
    v_ocupacion_max INT := 0;
    v_dia DATE;
    v_ocupacion_dia INT;
BEGIN
    SELECT cantidad_total INTO v_total FROM herramienta WHERE id = p_herramienta_id;
    
    v_dia := p_fecha_inicio;
    WHILE v_dia <= p_fecha_fin LOOP
        SELECT COALESCE(SUM(dr.cantidad), 0) INTO v_ocupacion_dia
        FROM detalle_reserva dr
        JOIN reserva r ON r.id = dr.reserva_id
        WHERE dr.herramienta_id = p_herramienta_id
          AND (p_excluir_reserva_id IS NULL OR dr.reserva_id <> p_excluir_reserva_id)
          AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
          AND (r.estado != 'PENDIENTE' 
               OR r.fecha_expiracion_pendiente IS NULL 
               OR r.fecha_expiracion_pendiente > clock_timestamp())
          AND r.fecha_inicio <= v_dia
          AND r.fecha_fin >= v_dia;
        
        IF v_ocupacion_dia > v_ocupacion_max THEN
            v_ocupacion_max := v_ocupacion_dia;
        END IF;
        
        v_dia := v_dia + INTERVAL '1 day';
    END LOOP;
    
    RETURN GREATEST(v_total - v_ocupacion_max, 0);
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

-- 9. TRIGGERS RESERVA
CREATE OR REPLACE FUNCTION validar_expiracion_pendiente()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        IF NEW.estado = 'PENDIENTE' THEN
            IF NEW.fecha_expiracion_pendiente IS NULL THEN
                RAISE EXCEPTION 'Reserva PENDIENTE requiere fecha_expiracion_pendiente';
            END IF;
            IF NEW.fecha_expiracion_pendiente <= clock_timestamp() THEN
                RAISE EXCEPTION 'fecha_expiracion_pendiente debe ser futura';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_validar_expiracion_pendiente
    BEFORE INSERT OR UPDATE ON reserva
    FOR EACH ROW EXECUTE FUNCTION validar_expiracion_pendiente();

CREATE OR REPLACE FUNCTION prevent_reserva_fechas_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.fecha_inicio IS DISTINCT FROM NEW.fecha_inicio 
           OR OLD.fecha_fin IS DISTINCT FROM NEW.fecha_fin THEN
            IF EXISTS (SELECT 1 FROM detalle_reserva WHERE reserva_id = OLD.id) THEN
                RAISE EXCEPTION 
                    'No se pueden cambiar fechas de una reserva con detalles. Cancele y cree nueva.';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_reserva_fechas_inmutables
    BEFORE UPDATE ON reserva
    FOR EACH ROW EXECUTE FUNCTION prevent_reserva_fechas_change();

-- 10. TRIGGERS DETALLE_RESERVA
CREATE OR REPLACE FUNCTION verificar_subtotal_detalle()
RETURNS TRIGGER AS $$
DECLARE
    v_dias INT;
    v_subtotal_esperado NUMERIC(14,2);
    v_iva_esperado NUMERIC(14,2);
    v_total_esperado NUMERIC(14,2);
BEGIN
    SELECT dias_inclusivos(r.fecha_inicio, r.fecha_fin) INTO v_dias
    FROM reserva r WHERE r.id = NEW.reserva_id;

    v_subtotal_esperado := NEW.precio_dia_acordado * NEW.cantidad * v_dias;
    
    IF NEW.iva_aplica THEN
        v_iva_esperado := round(v_subtotal_esperado * NEW.iva_porcentaje / 100, 2);
    ELSE
        v_iva_esperado := 0;
    END IF;
    v_total_esperado := v_subtotal_esperado + v_iva_esperado;

    IF NEW.subtotal != v_subtotal_esperado
       OR NEW.iva != v_iva_esperado
       OR NEW.total != v_total_esperado THEN
        RAISE EXCEPTION 
            'Valores incoherentes: subtotal=%, iva=%, total=% (esperado %, %, %)',
            NEW.subtotal, NEW.iva, NEW.total,
            v_subtotal_esperado, v_iva_esperado, v_total_esperado;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_verificar_subtotal
    BEFORE INSERT ON detalle_reserva
    FOR EACH ROW EXECUTE FUNCTION verificar_subtotal_detalle();

CREATE OR REPLACE FUNCTION prevent_detalle_reserva_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.precio_dia_acordado IS DISTINCT FROM NEW.precio_dia_acordado THEN
            RAISE EXCEPTION 'precio_dia_acordado es inmutable';
        END IF;
        IF OLD.cantidad IS DISTINCT FROM NEW.cantidad THEN
            RAISE EXCEPTION 'cantidad es inmutable; cancele y cree nueva reserva';
        END IF;
        IF OLD.herramienta_id IS DISTINCT FROM NEW.herramienta_id THEN
            RAISE EXCEPTION 'herramienta_id es inmutable';
        END IF;
        IF OLD.reserva_id IS DISTINCT FROM NEW.reserva_id THEN
            RAISE EXCEPTION 'reserva_id es inmutable';
        END IF;
        IF OLD.subtotal IS DISTINCT FROM NEW.subtotal
           OR OLD.iva IS DISTINCT FROM NEW.iva
           OR OLD.total IS DISTINCT FROM NEW.total
           OR OLD.iva_porcentaje IS DISTINCT FROM NEW.iva_porcentaje
           OR OLD.iva_aplica IS DISTINCT FROM NEW.iva_aplica THEN
            RAISE EXCEPTION 'valores calculados y tratamiento fiscal son inmutables';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_detalle_reserva_inmutable
    BEFORE UPDATE ON detalle_reserva
    FOR EACH ROW EXECUTE FUNCTION prevent_detalle_reserva_modification();

-- 11. PAGO
CREATE TABLE pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL REFERENCES reserva(id) ON DELETE RESTRICT,
    pago_original_id UUID REFERENCES pago(id),
    estado PAGO_ESTADO NOT NULL DEFAULT 'PENDIENTE',
    proveedor_pago VARCHAR(30) NOT NULL DEFAULT 'SIMULADO',
    transaccion_id VARCHAR(100),
    monto NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    moneda CHAR(3) NOT NULL DEFAULT 'COP',
    motivo TEXT,
    fecha_pago TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_pago_completado_por_reserva ON pago(reserva_id) WHERE estado = 'COMPLETADO';
CREATE INDEX idx_pago_original ON pago(pago_original_id) WHERE pago_original_id IS NOT NULL;
CREATE INDEX idx_pago_reserva ON pago(reserva_id);

CREATE OR REPLACE FUNCTION prevent_pago_completado_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.estado = 'COMPLETADO' THEN
        IF OLD.estado IS DISTINCT FROM NEW.estado
           OR OLD.monto IS DISTINCT FROM NEW.monto
           OR OLD.moneda IS DISTINCT FROM NEW.moneda
           OR OLD.reserva_id IS DISTINCT FROM NEW.reserva_id
           OR OLD.pago_original_id IS DISTINCT FROM NEW.pago_original_id
           OR OLD.transaccion_id IS DISTINCT FROM NEW.transaccion_id THEN
            RAISE EXCEPTION 'Pago COMPLETADO es inmutable en sus campos financieros e identidad';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_pago_completado_inmutable
    BEFORE UPDATE ON pago
    FOR EACH ROW EXECUTE FUNCTION prevent_pago_completado_change();

CREATE OR REPLACE FUNCTION proteger_reembolso_referenciado()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.estado = 'REEMBOLSADO' THEN
        IF EXISTS (SELECT 1 FROM factura WHERE pago_reembolso_id = OLD.id) THEN
            IF OLD.estado IS DISTINCT FROM NEW.estado
               OR OLD.monto IS DISTINCT FROM NEW.monto
               OR OLD.moneda IS DISTINCT FROM NEW.moneda
               OR OLD.reserva_id IS DISTINCT FROM NEW.reserva_id
               OR OLD.pago_original_id IS DISTINCT FROM NEW.pago_original_id THEN
                RAISE EXCEPTION 
                    'No se puede modificar un reembolso referenciado por una nota de crédito';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_proteger_reembolso_referenciado
    BEFORE UPDATE ON pago
    FOR EACH ROW EXECUTE FUNCTION proteger_reembolso_referenciado();

-- 12. FACTURA
CREATE TABLE factura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL REFERENCES reserva(id) ON DELETE RESTRICT,
    numero_factura VARCHAR(30) NOT NULL UNIQUE,
    tipo FACTURA_TIPO NOT NULL DEFAULT 'FACTURA',
    factura_original_id UUID,
    pago_reembolso_id UUID,
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    iva NUMERIC(14,2) NOT NULL CHECK (iva >= 0),
    total NUMERIC(14,2) NOT NULL CHECK (total >= 0),
    moneda CHAR(3) NOT NULL DEFAULT 'COP',
    iva_porcentaje NUMERIC(5,2) NOT NULL CHECK (iva_porcentaje >= 0),
    iva_aplica BOOLEAN NOT NULL DEFAULT true,
    datos_emisor JSONB NOT NULL,
    datos_receptor JSONB NOT NULL,
    fecha_emision TIMESTAMPTZ NOT NULL DEFAULT now(),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_factura_total CHECK (total = subtotal + iva),
    CONSTRAINT chk_nota_credito_tiene_original CHECK (
        tipo = 'FACTURA' OR (tipo = 'NOTA_CREDITO' AND factura_original_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_factura_reserva_id ON factura(reserva_id, id);
CREATE UNIQUE INDEX uq_factura_original_por_reserva ON factura(reserva_id) WHERE tipo = 'FACTURA';

ALTER TABLE factura ADD CONSTRAINT fk_nota_credito_original
    FOREIGN KEY (reserva_id, factura_original_id) REFERENCES factura(reserva_id, id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE factura ADD CONSTRAINT fk_nota_credito_reembolso
    FOREIGN KEY (pago_reembolso_id) REFERENCES pago(id) DEFERRABLE INITIALLY IMMEDIATE;

CREATE OR REPLACE FUNCTION validar_nota_credito_original()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF NEW.factura_original_id IS NOT NULL THEN
            IF NOT EXISTS (
                SELECT 1 FROM factura 
                WHERE id = NEW.factura_original_id 
                  AND reserva_id = NEW.reserva_id 
                  AND tipo = 'FACTURA'
            ) THEN
                RAISE EXCEPTION 
                    'factura_original_id debe referenciar una FACTURA de la misma reserva';
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.tipo = 'FACTURA' AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF EXISTS (SELECT 1 FROM factura WHERE factura_original_id = OLD.id) THEN
            RAISE EXCEPTION 
                'No se puede convertir a NOTA_CREDITO una factura original referenciada por notas de crédito';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_validar_nota_credito_original
    BEFORE INSERT OR UPDATE ON factura
    FOR EACH ROW EXECUTE FUNCTION validar_nota_credito_original();

CREATE OR REPLACE FUNCTION validar_nota_credito_reembolso_factura()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF NEW.pago_reembolso_id IS NULL THEN
            RAISE EXCEPTION 'NOTA_CREDITO requiere pago_reembolso_id';
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pago 
            WHERE id = NEW.pago_reembolso_id 
              AND estado = 'REEMBOLSADO' 
              AND reserva_id = NEW.reserva_id
        ) THEN
            RAISE EXCEPTION 
                'pago_reembolso_id debe referenciar un REEMBOLSADO de la misma reserva';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_validar_nota_credito_reembolso_factura
    BEFORE INSERT OR UPDATE ON factura
    FOR EACH ROW EXECUTE FUNCTION validar_nota_credito_reembolso_factura();

-- 13. REPORTE_DANO
CREATE TABLE reporte_dano (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    detalle_reserva_id UUID NOT NULL REFERENCES detalle_reserva(id) ON DELETE RESTRICT,
    herramienta_id UUID NOT NULL REFERENCES herramienta(id) ON DELETE RESTRICT,
    reportado_por_usuario_id UUID NOT NULL REFERENCES usuario(id),
    descripcion TEXT NOT NULL,
    severidad DANO_SEVERIDAD NOT NULL,
    costo_estimado_reparacion NUMERIC(14,2) CHECK (costo_estimado_reparacion >= 0),
    fecha_reporte TIMESTAMPTZ NOT NULL DEFAULT now(),
    resuelto_en TIMESTAMPTZ,
    resolucion TEXT
);
CREATE INDEX idx_reporte_dano_detalle ON reporte_dano(detalle_reserva_id);
CREATE INDEX idx_reporte_dano_herramienta ON reporte_dano(herramienta_id);
CREATE INDEX idx_reporte_dano_fecha ON reporte_dano(fecha_reporte);

-- 14. SECUENCIA Y GENERADOR
CREATE SEQUENCE factura_seq START 1;

CREATE OR REPLACE FUNCTION generar_numero_factura()
RETURNS VARCHAR AS $$
DECLARE
    v_prefijo VARCHAR := (SELECT valor FROM configuracion_fiscal WHERE clave = 'factura_serie_prefijo');
    v_num BIGINT;
BEGIN
    v_num := nextval('factura_seq');
    RETURN v_prefijo || '-' || lpad(v_num::text, 6, '0');
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 15. AUDITORÍA updated_at
CREATE OR REPLACE FUNCTION trigger_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE TRIGGER trg_usuario_actualizado BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION trigger_actualizado_en();
CREATE TRIGGER trg_herramienta_actualizado BEFORE UPDATE ON herramienta FOR EACH ROW EXECUTE FUNCTION trigger_actualizado_en();
CREATE TRIGGER trg_reserva_actualizado BEFORE UPDATE ON reserva FOR EACH ROW EXECUTE FUNCTION trigger_actualizado_en();