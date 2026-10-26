-- =============================================================================
-- V7: Agregar nuevo valor 'REPARACION' al tipo enumerado factura_tipo
-- =============================================================================
ALTER TYPE factura_tipo ADD VALUE IF NOT EXISTS 'REPARACION';
