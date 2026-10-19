-- V4: Campos para flujo de devolución, informe de cliente e inspección técnica de daños
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS solicitud_devolucion BOOLEAN DEFAULT FALSE;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS estado_devolucion VARCHAR(50);
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS nota_devolucion_cliente TEXT;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS costo_reparacion NUMERIC(14,2) DEFAULT 0;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS descripcion_dano TEXT;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS observacion_propietario TEXT;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS fecha_devolucion TIMESTAMP WITH TIME ZONE;
