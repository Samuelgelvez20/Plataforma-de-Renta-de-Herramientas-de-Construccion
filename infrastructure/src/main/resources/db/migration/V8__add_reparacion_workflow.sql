-- =============================================================================
-- V8: Flujo de facturación y liquidación de costos de reparación por daños
-- =============================================================================

-- 1. Actualizar restricción de nota crédito para soportar tipo 'REPARACION'
ALTER TABLE factura DROP CONSTRAINT IF EXISTS chk_nota_credito_tiene_original;
ALTER TABLE factura ADD CONSTRAINT chk_nota_credito_tiene_original
    CHECK (tipo != 'NOTA_CREDITO' OR factura_original_id IS NOT NULL);

-- 2. Columnas de estado, descripción del daño y pago en tabla factura
ALTER TABLE factura ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'PAGADA';
ALTER TABLE factura ADD COLUMN IF NOT EXISTS descripcion_dano TEXT;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS pago_id UUID REFERENCES pago(id);

-- 3. Índice único para evitar facturas de reparación duplicadas por reserva
CREATE UNIQUE INDEX IF NOT EXISTS uq_factura_reparacion_por_reserva
    ON factura(reserva_id) WHERE tipo = 'REPARACION';

-- 4. Columna factura_id en tabla pago para vincular pagos de reparación
ALTER TABLE pago ADD COLUMN IF NOT EXISTS factura_id UUID REFERENCES factura(id);

-- 5. Actualizar índices de pagos completados: conservar uq_pago_completado_por_reserva para alquileres y crear uq_pago_completado_por_factura para reparación
DROP INDEX IF EXISTS uq_pago_completado_por_reserva;
CREATE UNIQUE INDEX uq_pago_completado_por_reserva
    ON pago(reserva_id) WHERE estado = 'COMPLETADO' AND factura_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_completado_por_factura
    ON pago(factura_id) WHERE estado = 'COMPLETADO' AND factura_id IS NOT NULL;
