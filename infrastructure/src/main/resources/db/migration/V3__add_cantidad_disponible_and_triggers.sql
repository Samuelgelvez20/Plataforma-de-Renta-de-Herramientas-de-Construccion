-- V3: Añadir columna cantidad_disponible a herramienta y triggers reactivos de stock

ALTER TABLE herramienta ADD COLUMN IF NOT EXISTS cantidad_disponible INTEGER;
UPDATE herramienta SET cantidad_disponible = cantidad_total WHERE cantidad_disponible IS NULL;
ALTER TABLE herramienta ALTER COLUMN cantidad_disponible SET DEFAULT 1;

-- Función de recálculo de stock disponible
CREATE OR REPLACE FUNCTION recalcular_cantidad_disponible_herramienta(p_herramienta_id UUID)
RETURNS VOID AS $$
DECLARE
    v_total INT;
    v_ocupadas INT;
BEGIN
    SELECT cantidad_total INTO v_total FROM herramienta WHERE id = p_herramienta_id;
    IF v_total IS NULL THEN
        RETURN;
    END IF;

    SELECT COALESCE(SUM(dr.cantidad), 0) INTO v_ocupadas
    FROM detalle_reserva dr
    JOIN reserva r ON r.id = dr.reserva_id
    WHERE dr.herramienta_id = p_herramienta_id
      AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
      AND (r.estado != 'PENDIENTE' OR r.fecha_expiracion_pendiente IS NULL OR r.fecha_expiracion_pendiente > clock_timestamp());

    UPDATE herramienta
    SET cantidad_disponible = GREATEST(0, v_total - v_ocupadas)
    WHERE id = p_herramienta_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualización automática ante cambios de estado en reserva
CREATE OR REPLACE FUNCTION trg_actualizar_disponibilidad_desde_reserva()
RETURNS TRIGGER AS $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT DISTINCT herramienta_id FROM detalle_reserva WHERE reserva_id = NEW.id LOOP
        PERFORM recalcular_cantidad_disponible_herramienta(r.herramienta_id);
    END LOOP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para cambios en detalle_reserva
CREATE OR REPLACE FUNCTION trg_actualizar_disponibilidad_desde_detalle()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM recalcular_cantidad_disponible_herramienta(OLD.herramienta_id);
        RETURN OLD;
    ELSE
        PERFORM recalcular_cantidad_disponible_herramienta(NEW.herramienta_id);
        IF TG_OP = 'UPDATE' AND OLD.herramienta_id <> NEW.herramienta_id THEN
            PERFORM recalcular_cantidad_disponible_herramienta(OLD.herramienta_id);
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reserva_estado_disponibilidad ON reserva;
CREATE TRIGGER trg_reserva_estado_disponibilidad
AFTER INSERT OR UPDATE OF estado ON reserva
FOR EACH ROW
EXECUTE FUNCTION trg_actualizar_disponibilidad_desde_reserva();

DROP TRIGGER IF EXISTS trg_detalle_reserva_disponibilidad ON detalle_reserva;
CREATE TRIGGER trg_detalle_reserva_disponibilidad
AFTER INSERT OR UPDATE OR DELETE ON detalle_reserva
FOR EACH ROW
EXECUTE FUNCTION trg_actualizar_disponibilidad_desde_detalle();

-- Recálculo inicial de todas las herramientas
DO $$
DECLARE
    h RECORD;
BEGIN
    FOR h IN SELECT id FROM herramienta LOOP
        PERFORM recalcular_cantidad_disponible_herramienta(h.id);
    END LOOP;
END;
$$;
