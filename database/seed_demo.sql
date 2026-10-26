-- =============================================================================
-- OBRIXEN — Dataset Oficial de Demostración y Evaluación
-- Especificación:
--   1 Usuario Administrador (ADMIN)
--   1 Usuario Cliente (CLIENTE)
--   3 Usuarios Propietarios (PROPIETARIO) con 8 herramientas cada uno (24 total)
-- Compatibilidad: Flyway V6 (Sin columna NIT en propietario)
-- Idempotencia: ON CONFLICT DO NOTHING
-- =============================================================================

-- 1. Limpieza de usuarios técnicos temporales sin dependencias (si existen)
DELETE FROM propietario WHERE usuario_id IN (SELECT id FROM usuario WHERE email = 'test.propietario.nuevo@empresa.com');
DELETE FROM usuario WHERE email = 'test.propietario.nuevo@empresa.com';

-- 2. USUARIOS (5 cuentas exactas para demostración)
-- Contraseñas hasheadas con BCrypt $2a$ (10 rondas):
--   admin@obrixen.com.co        -> AdminDemo2026*!
--   cliente@obrixen.com.co      -> ClienteDemo2026*!
--   propietario1@obrixen.com.co -> Propietario1Demo2026*!
--   propietario2@obrixen.com.co -> Propietario2Demo2026*!
--   propietario3@obrixen.com.co -> Propietario3Demo2026*!

INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo, creado_en, actualizado_en)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'admin@obrixen.com.co', '$2a$10$TJnLw/MW6sEd8ZkO7kGli.uFXV9zZRKDYluHUmJyw5sUPu4DgXpue', 'ADMIN', 'Administrador', 'General', '+573001110000', true, now(), now()),
  ('22222222-2222-2222-2222-222222222222', 'cliente@obrixen.com.co', '$2a$10$jYGUp0TZwUAAqHWwUPf8L.7Rr4GuSkj/2AX10g7EzcCnA7fxY5DQy', 'CLIENTE', 'Andrés', 'Restrepo', '+573102223344', true, now(), now()),
  ('33333333-3333-3333-3333-333333333331', 'propietario1@obrixen.com.co', '$2a$10$6I9NlA98rDSkAf90NydiM.UvvpsGW7s6HkrrB9aBhlGe4XREBQCoC', 'PROPIETARIO', 'Juan Camilo', 'Pérez', '+573114445566', true, now(), now()),
  ('33333333-3333-3333-3333-333333333332', 'propietario2@obrixen.com.co', '$2a$10$7oztLrkGnF.I../omb8aO.dWxUA9oUBItO89SsHmaF3XZynshwE5W', 'PROPIETARIO', 'María Fernanda', 'Gómez', '+573125556677', true, now(), now()),
  ('33333333-3333-3333-3333-333333333333', 'propietario3@obrixen.com.co', '$2a$10$12XR4l0BbWyNOrJyc.9TCO2NxZORqcxLvsGecQ91Irw7wRq39V2s6', 'PROPIETARIO', 'Santiago', 'Morales', '+573136667788', true, now(), now())
ON CONFLICT (id) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  activo = true,
  nombre = EXCLUDED.nombre,
  apellido = EXCLUDED.apellido;

-- 3. CLIENTE (1 cliente exacto)
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion)
VALUES ('22222222-2222-2222-2222-222222222222', 'Calle 100 # 15-20, Edificio Altavista Obra 4, Bogotá', '{"razon_social":"Constructora Restrepo SAS","regimen":"COMUN"}'::jsonb)
ON CONFLICT (usuario_id) DO NOTHING;

-- 4. PROPIETARIOS (3 propietarios exactos)
INSERT INTO propietario (usuario_id, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en)
VALUES
  ('33333333-3333-3333-3333-333333333331', 'Maquinaria y Equipos Andina S.A.S.', 'Cra 15 # 93-45 Oficina 502', '+573114445566', 'COMUN', 'Bogotá', 'Cundinamarca', now()),
  ('33333333-3333-3333-3333-333333333332', 'Soluciones de Energía y Andamios del Valle S.A.S.', 'Calle 26N # 6N-40', '+573125556677', 'COMUN', 'Cali', 'Valle del Cauca', now()),
  ('33333333-3333-3333-3333-333333333333', 'Equipos Eléctricos y Menores de Antioquia Ltda.', 'Cra 43A # 1Sur-50', '+573136667788', 'COMUN', 'Medellín', 'Antioquia', now())
ON CONFLICT (usuario_id) DO NOTHING;

-- 5. HERRAMIENTAS (24 herramientas exactas: 8 por cada propietario)
-- Categorías estándar de V2:
--   Maquinaria Pesada:      b76d4751-8cf2-4db5-8d4c-cabab4e270af
--   Equipos de Concreto:    248a54ef-36c9-4b40-8ea8-69862c9fb4c7
--   Andamios y Estructuras: 7752e73a-af20-4428-aad9-07d177ef4312
--   Generación y Bombeo:    16a20a8b-86b9-4ee1-8dbc-10ab807da353
--   Herramienta Eléctrica:  31de8d04-1dfd-4a4d-af65-1c68ae92cdcb
--   Herramienta Menor:      fb149955-d12a-4529-a62b-15ca365baa8f

-- === PROPIETARIO 1: Maquinaria y Equipos Andina S.A.S. (8 herramientas) ===
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, marca, modelo, precio_dia, cantidad_total, cantidad_disponible, estado_publicacion, iva_porcentaje, iva_aplica)
VALUES
  ('44444444-1111-0000-0000-000000000001', 'b76d4751-8cf2-4db5-8d4c-cabab4e270af', '33333333-3333-3333-3333-333333333331',
   'Retroexcavadora sobre Orugas 20 Ton', 'Excavadora hidráulica de alto rendimiento para movimiento de tierras masivo y excavaciones profundas', 'Caterpillar', '320D', 450000.00, 3, 3, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000002', 'b76d4751-8cf2-4db5-8d4c-cabab4e270af', '33333333-3333-3333-3333-333333333331',
   'Miniexcavadora Compacta 3.5 Ton', 'Excavadora compacta con giro de voladizo cero ideal para zanjas urbanas y espacios reducidos', 'Bobcat', 'E35', 280000.00, 4, 4, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000003', 'b76d4751-8cf2-4db5-8d4c-cabab4e270af', '33333333-3333-3333-3333-333333333331',
   'Cargador Frontal Articulado 2.5 m³', 'Cargador de ruedas para cargue ágil de volquetas y acopio de materiales de cantera', 'Komatsu', 'WA200', 520000.00, 2, 2, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000004', 'b76d4751-8cf2-4db5-8d4c-cabab4e270af', '33333333-3333-3333-3333-333333333331',
   'Minicargador sobre Llantas', 'Equipo versátil para nivelación, cargue de escombros y apoyo logístico en obras civiles', 'Case', 'SR175', 210000.00, 5, 5, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000005', '248a54ef-36c9-4b40-8ea8-69862c9fb4c7', '33333333-3333-3333-3333-333333333331',
   'Mezcladora de Concreto 2 Bultos Diésel', 'Trompo mezclador con motor diésel para fundición continua de zapatas, vigas y losas', 'Husqvarna', 'C200', 85000.00, 6, 6, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000006', '248a54ef-36c9-4b40-8ea8-69862c9fb4c7', '33333333-3333-3333-3333-333333333331',
   'Allanadora de Pavimento Helicóptero 36"', 'Allanadora mecánica de 4 aspas para acabado superficial liso en pisos industriales de concreto', 'Wacker Neuson', 'CT36', 110000.00, 4, 4, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000007', '248a54ef-36c9-4b40-8ea8-69862c9fb4c7', '33333333-3333-3333-3333-333333333331',
   'Vibrador de Concreto Eléctrico 2 HP', 'Vibrador de inmersión con manguera de 4 metros para compactación homogénea de columnas', 'Bosch', 'GVC 22 EX', 45000.00, 8, 8, 'PUBLICADA', 19.00, true),
  ('44444444-1111-0000-0000-000000000008', '248a54ef-36c9-4b40-8ea8-69862c9fb4c7', '33333333-3333-3333-3333-333333333331',
   'Regla Vibratoria para Losas 3 m', 'Perfil oscilante de aluminio para compactado y nivelación milimétrica de pavimentos', 'Enar', 'QZ-30', 75000.00, 5, 5, 'PUBLICADA', 19.00, true)
ON CONFLICT (id) DO NOTHING;

-- === PROPIETARIO 2: Soluciones de Energía y Andamios del Valle S.A.S. (8 herramientas) ===
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, marca, modelo, precio_dia, cantidad_total, cantidad_disponible, estado_publicacion, iva_porcentaje, iva_aplica)
VALUES
  ('44444444-2222-0000-0000-000000000001', '7752e73a-af20-4428-aad9-07d177ef4312', '33333333-3333-3333-3333-333333333332',
   'Andamio Multidireccional Certificado (Cuerpo 2x2m)', 'Módulo certificado bajo norma OSHA/ANSI con plataformas antideslizantes y rodapiés', 'Layher', 'Allround', 35000.00, 20, 20, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000002', '7752e73a-af20-4428-aad9-07d177ef4312', '33333333-3333-3333-3333-333333333332',
   'Torre Móvil de Acceso en Aluminio 6 m', 'Torre liviana con ruedas frenadas y estabilizadores telescópicos para trabajo en altura', 'Altrex', 'MiTOWER-6', 65000.00, 6, 6, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000003', '7752e73a-af20-4428-aad9-07d177ef4312', '33333333-3333-3333-3333-333333333332',
   'Plataforma Colgante Motorizada 4 m', 'Andamio colgante eléctrico certificado para mantenimiento de fachadas y ventanería', 'Tractel', 'Tirak-400', 190000.00, 3, 3, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000004', '7752e73a-af20-4428-aad9-07d177ef4312', '33333333-3333-3333-3333-333333333332',
   'Escalera Telescópica de Fibra de Vidrio 28 Pasos', 'Escalera extensible dieléctrica certificada para trabajos eléctricos y estructurales', 'Werner', 'D6228-2', 30000.00, 10, 10, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000005', '16a20a8b-86b9-4ee1-8dbc-10ab807da353', '33333333-3333-3333-3333-333333333332',
   'Generador Diésel Insonorizado 60 kVA', 'Planta eléctrica cabinada ultrasilenciosa con transferencia automática para faenas exigentes', 'Cummins', 'C60D5', 320000.00, 3, 3, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000006', '16a20a8b-86b9-4ee1-8dbc-10ab807da353', '33333333-3333-3333-3333-333333333332',
   'Generador Portátil a Gasolina 8.5 kW', 'Generador inverter robusto para alimentación de herramientas y soldadores en campo', 'Honda', 'EU70is', 120000.00, 6, 6, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000007', '16a20a8b-86b9-4ee1-8dbc-10ab807da353', '33333333-3333-3333-3333-333333333332',
   'Motobomba Autocebante para Lodos 3"', 'Bomba centrífuga con motor a gasolina para desagüe de excavaciones con sólidos en suspensión', 'Wacker Neuson', 'PT3A', 70000.00, 5, 5, 'PUBLICADA', 19.00, true),
  ('44444444-2222-0000-0000-000000000008', '16a20a8b-86b9-4ee1-8dbc-10ab807da353', '33333333-3333-3333-3333-333333333332',
   'Torre de Iluminación 4 Lámparas LED 350W', 'Torre con mástil vertical de 8.5 metros y generador propio para operaciones nocturnas', 'Generac', 'VT-Solar', 140000.00, 4, 4, 'PUBLICADA', 19.00, true)
ON CONFLICT (id) DO NOTHING;

-- === PROPIETARIO 3: Equipos Eléctricos y Menores de Antioquia Ltda. (8 herramientas) ===
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, marca, modelo, precio_dia, cantidad_total, cantidad_disponible, estado_publicacion, iva_porcentaje, iva_aplica)
VALUES
  ('44444444-3333-0000-0000-000000000001', '31de8d04-1dfd-4a4d-af65-1c68ae92cdcb', '33333333-3333-3333-3333-333333333333',
   'Rotomartillo Combinado SDS-Max 11 kg', 'Martillo perforador y demoledor con control de vibración para mampostería pesada', 'Bosch', 'GBH 12-52 DV', 65000.00, 7, 7, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000002', 'fb149955-d12a-4529-a62b-15ca365baa8f', '33333333-3333-3333-3333-333333333333',
   'Cortadora de Concreto y Asfalto 14"', 'Tronzadora a gasolina con toma de agua para cortes limpios en losas y bordillos', 'Stihl', 'TS 420', 90000.00, 5, 5, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000003', '31de8d04-1dfd-4a4d-af65-1c68ae92cdcb', '33333333-3333-3333-3333-333333333333',
   'Martillo Demoledor Hexagonal Eléctrico 30 kg', 'Rompedor de pavimento eléctrico de alto impacto con energía de golpe de 68 Joules', 'DeWalt', 'D25980', 85000.00, 6, 6, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000004', 'fb149955-d12a-4529-a62b-15ca365baa8f', '33333333-3333-3333-3333-333333333333',
   'Compactador Tipo Canguro 4 Tiempos', 'Apisonador vertical a gasolina para zanjas de tubería y bases granulares estrechas', 'Mikasa', 'MTX-70', 75000.00, 8, 8, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000005', 'fb149955-d12a-4529-a62b-15ca365baa8f', '33333333-3333-3333-3333-333333333333',
   'Placa Compactadora Reversible 160 kg', 'Vibrocompactador reversible de avance rápido para adoquinados y subbases de pavimento', 'Weber', 'CR3', 95000.00, 4, 4, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000006', '31de8d04-1dfd-4a4d-af65-1c68ae92cdcb', '33333333-3333-3333-3333-333333333333',
   'Sierra Circular Eléctrica de Banco 10"', 'Mesa de corte de madera de precisión con guía milimétrica para formaletería y carpintería', 'Makita', 'MLT100', 50000.00, 5, 5, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000007', '31de8d04-1dfd-4a4d-af65-1c68ae92cdcb', '33333333-3333-3333-3333-333333333333',
   'Pulidora Angular 9" para Desbaste Pesado', 'Amoladora angular industrial de 2400W para corte de perfiles metálicos y esmerilado', 'Milwaukee', '6088-30', 35000.00, 10, 10, 'PUBLICADA', 19.00, true),
  ('44444444-3333-0000-0000-000000000008', 'fb149955-d12a-4529-a62b-15ca365baa8f', '33333333-3333-3333-3333-333333333333',
   'Cortadora de Varilla Eléctrica Hidráulica', 'Cizalla electrohidráulica portátil para corte de acero de refuerzo hasta 1 pulgada', 'Ogura', 'HBC-19', 80000.00, 4, 4, 'PUBLICADA', 19.00, true)
ON CONFLICT (id) DO NOTHING;
