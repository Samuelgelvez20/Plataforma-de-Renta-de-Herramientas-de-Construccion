-- V6__remove_nit_from_propietario.sql
-- Eliminación integral de NIT como requisito y columna persistente en la tabla propietario

ALTER TABLE propietario DROP COLUMN IF EXISTS nit;
