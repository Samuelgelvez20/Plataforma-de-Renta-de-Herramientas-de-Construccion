-- V5__add_activo_to_usuario.sql
-- Añade columna activo para habilitación/deshabilitación y borrado lógico de usuarios

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
