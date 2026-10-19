-- ============================================================================
-- PLATAFORMA DE RENTA DE HERRAMIENTAS Y EQUIPOS DE CONSTRUCCION
-- DATOS DE DEMOSTRACION Y POBLACION INICIAL (MINIMO 20 REGISTROS POR ENTIDAD)
-- Entidades requeridas: usuarios, proveedores, clientes, herramientas, reservas, pagos
-- ============================================================================

-- 1. CONFIGURACION FISCAL
INSERT INTO configuracion_fiscal (clave, valor, descripcion) VALUES
('iva_porcentaje', '19.00', 'Tarifa general de IVA en Colombia 19%'),
('moneda', 'COP', 'Moneda de operación de la plataforma'),
('reserva_pendiente_expiracion_minutos', '30', 'Tiempo para pagar antes de expirar'),
('factura_serie_prefijo', 'F', 'Prefijo de serie para facturas electrónicas'),
('factura_correlativo_inicio', '1', 'Número inicial de consecutivo'),
('IVA_PORCENTAJE', '19.00', 'Tarifa general de IVA en Colombia para arrendamiento de bienes muebles'),
('MONEDA_PRINCIPAL', 'COP', 'Peso Colombiano'),
('TIEMPO_EXPIRACION_RESERVA_HORAS', '24', 'Horas para confirmación y pago antes de expirar reserva'),
('DIAS_ANTICIPACION_MAXIMA_RESERVA', '90', 'Rango máximo en días hacia el futuro para reservar')
ON CONFLICT (clave) DO NOTHING;

-- 2. CATEGORIAS
INSERT INTO categoria (id, nombre, descripcion) VALUES
('039732dd-87fe-5a74-b00b-03ac76648499', 'Maquinaria Pesada', 'Excavadoras, retroexcavadoras, cargadores y compactadoras'),
('d74295fc-8eb9-52f2-845b-7c14c533e46f', 'Herramienta Eléctrica', 'Taladros, rotomartillos, sierras y pulidoras'),
('08d81acd-76d1-50f1-affa-d4f9783b1d9f', 'Andamios y Estructuras', 'Andamios certificados, torres de trabajo y escaleras'),
('de906bd2-ace6-59b2-9054-dfa034fc8646', 'Equipos de Concreto', 'Mezcladoras, vibradores de concreto y allanadoras'),
('dc17e3d0-e1be-5acc-9cf8-b7513cadee05', 'Generación y Bombeo', 'Generadores eléctricos, motobombas y torres de iluminación'),
('2ab65d8b-689d-57e8-aff8-b172904dc8ba', 'Herramienta Menor', 'Herramientas manuales, niveles láser y compactadores tipo rana')
ON CONFLICT (nombre) DO UPDATE SET id = EXCLUDED.id, descripcion = EXCLUDED.descripcion;

-- 3. USUARIOS (1 Administrador + 20 Proveedores + 20 Clientes = 41 usuarios)
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('1077294c-067e-5487-adf3-58914bad10c6', 'admin@rentaequipos.com.co', '$2a$10$XOM7671pLHz.10NhP5yddOLF9504euR3naLtUh9wJpvbG0Q58cwEO', 'ADMIN', 'Carlos', 'Administrador', '+573001112233', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('2a472a0b-eade-5134-93d4-e1150e313e28', 'proveedor01@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 01', '+573111110001', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('06511c04-473e-52d7-943e-fc66440358ee', 'proveedor02@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 02', '+573111110002', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('3a8db36a-1dd1-59ca-b19c-c5e5539ff67e', 'proveedor03@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 03', '+573111110003', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('fa0dd009-b1ca-5e50-8ee7-9dfa0200be02', 'proveedor04@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 04', '+573111110004', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('e6165462-4e93-51e0-a663-c6fb6917e3dc', 'proveedor05@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 05', '+573111110005', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('e6cb7ed5-d911-566f-9278-66ee259bb67c', 'proveedor06@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 06', '+573111110006', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('ee8b99ef-e3bb-5f48-99c3-b8bf221db1b9', 'proveedor07@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 07', '+573111110007', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('15d995ff-9870-56ac-8b7e-97aea9229363', 'proveedor08@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 08', '+573111110008', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('873895ce-a38c-5e95-aa59-f5e24e679fdf', 'proveedor09@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 09', '+573111110009', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('46f1a68a-2f86-543c-9d8e-40778d974f64', 'proveedor10@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 10', '+573111110010', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('77ac967d-b8f2-5393-912c-9a6e073d548b', 'proveedor11@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 11', '+573111110011', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('6982d7bf-c48d-5b2d-9ac7-fc31c96b8966', 'proveedor12@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 12', '+573111110012', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('45cde3ed-2efd-5429-90a2-06cf7b7946a4', 'proveedor13@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 13', '+573111110013', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('f6eae0e7-46ef-5f3b-acd6-f0596de03e51', 'proveedor14@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 14', '+573111110014', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('68b08ad7-aeb8-558e-99b8-a16e4e0e6d3f', 'proveedor15@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 15', '+573111110015', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('fc2d3b90-a803-5123-9a75-095163dac818', 'proveedor16@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 16', '+573111110016', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('a075283e-6a7d-5e76-88b8-99c35982b340', 'proveedor17@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 17', '+573111110017', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('d390b029-1f50-5e5d-a90e-2016119a72e0', 'proveedor18@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 18', '+573111110018', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('f234c924-4756-5a59-a35e-1fe8fdb44497', 'proveedor19@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 19', '+573111110019', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('ffe9f9f9-9b4f-5e4f-b6bf-4b098c6b9c22', 'proveedor20@rentaequipos.com.co', '$2a$10$MW26ioZGl7w.hXoQBN0QCuRxrahL79yhjkkKXPtsM9puOC46HebWW', 'PROPIETARIO', 'Representante', 'Empresa 20', '+573111110020', true) ON CONFLICT (email) DO NOTHING;

-- 4. PROPIETARIOS / PROVEEDORES (20 registros)
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('2a472a0b-eade-5134-93d4-e1150e313e28', '900123456-1', 'Constructora Andina SAS', 'Cra 15 # 93-45', '+573111110001', 'COMUN', 'Bogotá', 'Cundinamarca', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('06511c04-473e-52d7-943e-fc66440358ee', '900234567-2', 'Alquileres del Norte Ltda', 'Calle 72 # 54-20', '+573111110002', 'COMUN', 'Barranquilla', 'Atlántico', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('3a8db36a-1dd1-59ca-b19c-c5e5539ff67e', '900345678-3', 'Maquinaria y Equipos Antioquia', 'Cra 43A # 1Sur-50', '+573111110003', 'COMUN', 'Medellín', 'Antioquia', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('fa0dd009-b1ca-5e50-8ee7-9dfa0200be02', '900456789-4', 'Equipos del Valle del Cauca', 'Calle 26N # 6N-40', '+573111110004', 'COMUN', 'Cali', 'Valle del Cauca', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('e6165462-4e93-51e0-a663-c6fb6917e3dc', '900567890-5', 'Andamios & Maquinaria Caribe', 'Cra 30 # 25-10', '+573111110005', 'COMUN', 'Cartagena', 'Bolívar', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('e6cb7ed5-d911-566f-9278-66ee259bb67c', '900678901-6', 'Soluciones Viales del Eje', 'Cra 7 # 19-28', '+573111110006', 'COMUN', 'Pereira', 'Risaralda', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('ee8b99ef-e3bb-5f48-99c3-b8bf221db1b9', '900789012-7', 'Herramientas Tolima y Huila', 'Cra 5 # 37-12', '+573111110007', 'COMUN', 'Ibagué', 'Tolima', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('15d995ff-9870-56ac-8b7e-97aea9229363', '900890123-8', 'Equipos Industriales Boyacá', 'Av Universitaria # 15-40', '+573111110008', 'COMUN', 'Tunja', 'Boyacá', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('873895ce-a38c-5e95-aa59-f5e24e679fdf', '900901234-9', 'Maquinas y Andamios Llanos', 'Calle 38 # 31-20', '+573111110009', 'COMUN', 'Villavicencio', 'Meta', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('46f1a68a-2f86-543c-9d8e-40778d974f64', '901012345-0', 'Construcciones Nariño Ltda', 'Calle 19 # 24-08', '+573111110010', 'COMUN', 'Pasto', 'Nariño', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('77ac967d-b8f2-5393-912c-9a6e073d548b', '901123456-1', 'Rentas de Concreto del Sinú', 'Cra 2 # 35-18', '+573111110011', 'COMUN', 'Montería', 'Córdoba', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('6982d7bf-c48d-5b2d-9ac7-fc31c96b8966', '901234567-2', 'Equipos Santander Express', 'Autopista Floridablanca # 112', '+573111110012', 'COMUN', 'Floridablanca', 'Santander', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('45cde3ed-2efd-5429-90a2-06cf7b7946a4', '901345678-3', 'Suministros Civiles de Caldas', 'Cra 23 # 54-10', '+573111110013', 'COMUN', 'Manizales', 'Caldas', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('f6eae0e7-46ef-5f3b-acd6-f0596de03e51', '901456789-4', 'Maquinaria Pesada Huila SAS', 'Calle 8 # 14-30', '+573111110014', 'COMUN', 'Neiva', 'Huila', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('68b08ad7-aeb8-558e-99b8-a16e4e0e6d3f', '901567890-5', 'Equipos y Generadores Bolívar', 'Bosque Diag 21 # 50-12', '+573111110015', 'COMUN', 'Cartagena', 'Bolívar', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('fc2d3b90-a803-5123-9a75-095163dac818', '901678901-6', 'Andamios Certificados del César', 'Calle 16 # 9-45', '+573111110016', 'COMUN', 'Valledupar', 'Cesar', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('a075283e-6a7d-5e76-88b8-99c35982b340', '901789012-7', 'Alquileres Cúcuta Frontera', 'Av 0 # 10-25', '+573111110017', 'COMUN', 'Cúcuta', 'Norte de Santander', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('d390b029-1f50-5e5d-a90e-2016119a72e0', '901890123-8', 'Soluciones Mecánicas del Cauca', 'Cra 6 # 4-30', '+573111110018', 'COMUN', 'Popayán', 'Cauca', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('f234c924-4756-5a59-a35e-1fe8fdb44497', '901901234-9', 'Equipos del Magdalena Medio', 'Calle 50 # 18-05', '+573111110019', 'COMUN', 'Barrancabermeja', 'Santander', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;
INSERT INTO propietario (usuario_id, nit, razon_social, direccion_fiscal, telefono_contacto, regimen, municipio, departamento, verificado_en) VALUES ('ffe9f9f9-9b4f-5e4f-b6bf-4b098c6b9c22', '902012345-0', 'MegaEquipos de Occidente SAS', 'Cra 100 # 11-60', '+573111110020', 'COMUN', 'Cali', 'Valle del Cauca', CURRENT_TIMESTAMP) ON CONFLICT (nit) DO NOTHING;

-- 5. CLIENTES (20 registros)
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('31d3a088-0fcd-5ac4-90d1-1ef871d4f9ba', 'cliente01@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Mateo', 'Gómez', '+573201110001', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('31d3a088-0fcd-5ac4-90d1-1ef871d4f9ba', 'Calle 127 # 19-45, Bogotá', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('d3ea6ee7-6b9a-54ed-81bf-146dd391fc13', 'cliente02@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Valentina', 'Rodríguez', '+573201110002', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('d3ea6ee7-6b9a-54ed-81bf-146dd391fc13', 'Cra 33 # 48-20, Bucaramanga', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('4a6f9b23-15c9-5f94-a502-0a1bd949aa42', 'cliente03@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Santiago', 'López', '+573201110003', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('4a6f9b23-15c9-5f94-a502-0a1bd949aa42', 'Calle 10 # 42-15, Medellín', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('e9fd0d0e-c30d-5134-a984-e95c09bb3f4d', 'cliente04@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Camila', 'García', '+573201110004', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('e9fd0d0e-c30d-5134-a984-e95c09bb3f4d', 'Av 4N # 18-30, Cali', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('392b2fc5-6ac4-5a5f-9c3c-92d13b0ca9f0', 'cliente05@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Sebastián', 'Martínez', '+573201110005', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('392b2fc5-6ac4-5a5f-9c3c-92d13b0ca9f0', 'Cra 53 # 82-40, Barranquilla', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('b2d64dc4-7646-5722-a898-78870d821241', 'cliente06@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Daniela', 'Pérez', '+573201110006', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('b2d64dc4-7646-5722-a898-78870d821241', 'Calle 21 # 14-50, Pereira', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('ab6e3792-723b-5c9e-96f1-d9442d35c7f8', 'cliente07@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Alejandro', 'Hernández', '+573201110007', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('ab6e3792-723b-5c9e-96f1-d9442d35c7f8', 'Cra 4 # 11-20, Ibagué', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('2e2c1c6f-82fd-596a-b6ef-f17c287518b7', 'cliente08@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Mariana', 'Torres', '+573201110008', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('2e2c1c6f-82fd-596a-b6ef-f17c287518b7', 'Calle 20 # 10-15, Tunja', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('80df59a0-e01c-599f-9bd1-81b3d894b8f7', 'cliente09@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Nicolás', 'Díaz', '+573201110009', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('80df59a0-e01c-599f-9bd1-81b3d894b8f7', 'Cra 40 # 25-10, Villavicencio', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('86cae8b5-c63a-5ab6-bb9a-44db5b320b0e', 'cliente10@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Isabella', 'Vargas', '+573201110010', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('86cae8b5-c63a-5ab6-bb9a-44db5b320b0e', 'Calle 18 # 22-40, Pasto', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('8f30449f-7bec-5a8b-b06e-d18f59fb5515', 'cliente11@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Samuel', 'Castro', '+573201110011', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('8f30449f-7bec-5a8b-b06e-d18f59fb5515', 'Cra 6 # 60-15, Montería', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('8bd39421-1147-5ec9-8d86-6539e7bed0ff', 'cliente12@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Luciana', 'Morales', '+573201110012', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('8bd39421-1147-5ec9-8d86-6539e7bed0ff', 'Calle 48 # 27-30, Bucaramanga', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('c6e3e078-5fda-5dca-b504-c3a4cc1e760a', 'cliente13@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Felipe', 'Ortiz', '+573201110013', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('c6e3e078-5fda-5dca-b504-c3a4cc1e760a', 'Cra 24 # 50-18, Manizales', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('2e917f55-8a8b-56a0-a465-129e108152c9', 'cliente14@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Sofía', 'Silva', '+573201110014', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('2e917f55-8a8b-56a0-a465-129e108152c9', 'Calle 12 # 15-20, Neiva', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('89c9e56c-f958-595d-be2f-ce1540cb7a85', 'cliente15@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Andrés', 'Ríos', '+573201110015', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('89c9e56c-f958-595d-be2f-ce1540cb7a85', 'Cra 3 # 8-45, Cartagena', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('9076a879-69b7-559c-b790-437b232e3e2d', 'cliente16@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Gabriela', 'Mendoza', '+573201110016', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('9076a879-69b7-559c-b790-437b232e3e2d', 'Calle 15 # 12-30, Valledupar', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('2926aa19-8a6f-5c0e-8138-650680919a39', 'cliente17@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'David', 'Guerrero', '+573201110017', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('2926aa19-8a6f-5c0e-8138-650680919a39', 'Av 1 # 12-50, Cúcuta', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('a31f8d9e-d5fc-5c8e-96dd-bc1dec4224b3', 'cliente18@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Paula', 'Navarro', '+573201110018', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('a31f8d9e-d5fc-5c8e-96dd-bc1dec4224b3', 'Cra 7 # 8-20, Popayán', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('273ef134-050a-57ad-8261-2e00082ef874', 'cliente19@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Esteban', 'Reyes', '+573201110019', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('273ef134-050a-57ad-8261-2e00082ef874', 'Calle 49 # 12-34, Barrancabermeja', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES ('27a06f3e-012d-5f45-a0d4-32dba8ce15a1', 'cliente20@rentaequipos.com.co', '$2a$10$FinR90nj9vivmHgVxRWlqeWCguUjf34ombgDjjf9NwZefmOajL/4C', 'CLIENTE', 'Juliana', 'Acosta', '+573201110020', true) ON CONFLICT (email) DO NOTHING;
INSERT INTO cliente (usuario_id, direccion_entrega_default, datos_facturacion) VALUES ('27a06f3e-012d-5f45-a0d4-32dba8ce15a1', 'Cra 66 # 9-18, Cali', '{"regimen": "SIMPLIFICADO", "ciudad": "Colombia"}'::jsonb) ON CONFLICT (usuario_id) DO NOTHING;

-- 6. HERRAMIENTAS (24 registros)
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('158b1e4b-fd44-5776-9edf-58f1b9797cc3', '039732dd-87fe-5a74-b00b-03ac76648499', '2a472a0b-eade-5134-93d4-e1150e313e28', 'Excavadora Orugas 20 Ton', 'Excavadora hidráulica sobre orugas con balde 1.2 m3', 750000.00, 3, 3, 'Caterpillar', '320D', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('5845de19-401a-5373-a092-dad4fb6ed2c8', '039732dd-87fe-5a74-b00b-03ac76648499', '06511c04-473e-52d7-943e-fc66440358ee', 'Retroexcavadora Mixta 4x4', 'Retroexcavadora con tracción 4x4 y brazo extensible', 480000.00, 4, 4, 'JCB', '3CX', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('78bbae1e-35e5-5042-aacf-23a709efbe22', '039732dd-87fe-5a74-b00b-03ac76648499', '3a8db36a-1dd1-59ca-b19c-c5e5539ff67e', 'Minicargador Frontal Bobcat', 'Minicargador sobre ruedas con cabina cerrada y balde', 320000.00, 5, 5, 'Bobcat', 'S570', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('90240f35-7c27-5dbf-8ec9-8b3be6131bdc', '039732dd-87fe-5a74-b00b-03ac76648499', 'fa0dd009-b1ca-5e50-8ee7-9dfa0200be02', 'Rodillo Compactador Vibro 3T', 'Rodillo vibrocompactador tándem para asfalto y subbase', 290000.00, 3, 3, 'Dynapac', 'CC1200', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('90afd962-792b-5d3d-91ba-4191bdbc393c', 'd74295fc-8eb9-52f2-845b-7c14c533e46f', 'e6165462-4e93-51e0-a663-c6fb6917e3dc', 'Rotomartillo SDS Max 1500W', 'Rotomartillo demoledor de alto impacto para hormigón', 65000.00, 8, 8, 'Bosch', 'GBH 8-45 D', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('7fcece97-5ac2-5e67-bd63-0ef2d04e5ecd', 'd74295fc-8eb9-52f2-845b-7c14c533e46f', 'e6cb7ed5-d911-566f-9278-66ee259bb67c', 'Demolvedor Hexagonal 30Kg', 'Martillo demoledor pesado para rotura de pavimentos', 110000.00, 6, 6, 'Makita', 'HM1812', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('f62ee34b-e2d1-5a76-bb4a-f4da2ecc8112', 'd74295fc-8eb9-52f2-845b-7c14c533e46f', 'ee8b99ef-e3bb-5f48-99c3-b8bf221db1b9', 'Sierra Circular de Mesa 10 Pulg', 'Sierra de banco para corte de encofrados y madera', 55000.00, 5, 5, 'DeWalt', 'DWE7491', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('1e75f6ff-40c5-5322-a18f-f54b3d4ded93', 'd74295fc-8eb9-52f2-845b-7c14c533e46f', '15d995ff-9870-56ac-8b7e-97aea9229363', 'Pulidora Angular Industrial 9 Pulg', 'Amoladora angular para corte y desbaste de acero y piedra', 42000.00, 10, 10, 'DeWalt', 'DWE490', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('f38859cf-d038-5d66-8300-91f0a2436a58', '08d81acd-76d1-50f1-affa-d4f9783b1d9f', '873895ce-a38c-5e95-aa59-f5e24e679fdf', 'Cuerpo Andamio Certificado 2x1.5m', 'Módulo de andamio multidireccional normado con crucetas', 25000.00, 30, 30, 'Andamios Col', 'RINGLOCK', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('2a048659-b2f7-5549-b834-bfad32c1dce3', '08d81acd-76d1-50f1-affa-d4f9783b1d9f', '46f1a68a-2f86-543c-9d8e-40778d974f64', 'Torre Móvil de Andamio 6 Metros', 'Torre rodante con ruedas de freno y plataforma de aluminio', 85000.00, 8, 8, 'Layher', 'RollTower', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('e02df6cd-8874-55ac-ae5f-99b9a3ab6468', '08d81acd-76d1-50f1-affa-d4f9783b1d9f', '77ac967d-b8f2-5393-912c-9a6e073d548b', 'Escalera Extensible Dieléctrica 28P', 'Escalera en fibra de vidrio de alta seguridad tipo 1AA', 38000.00, 12, 12, 'Cuprum', 'D-28P', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('7666ef86-ebab-5254-9273-03d779328b8d', '08d81acd-76d1-50f1-affa-d4f9783b1d9f', '6982d7bf-c48d-5b2d-9ac7-fc31c96b8966', 'Puntales Metálicos Telescópicos', 'Juego de puntales de soporte de losa ajustables 2.0 a 3.5m', 18000.00, 50, 50, 'Alsina', 'P-350', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('1986088e-4a53-5017-90c1-8493cd39c239', 'de906bd2-ace6-59b2-9054-dfa034fc8646', '45cde3ed-2efd-5429-90a2-06cf7b7946a4', 'Mezcladora de Concreto 2 Bultos', 'Trompo mezclador con motor a gasolina Kohler 9HP', 75000.00, 10, 10, 'Husqvarna', 'MC-2B', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('cdfb7ebc-f525-5c26-af8a-aed8756e1642', 'de906bd2-ace6-59b2-9054-dfa034fc8646', 'f6eae0e7-46ef-5f3b-acd6-f0596de03e51', 'Vibrador de Concreto Eléctrico 2HP', 'Vibrador pendular con manguera de 4 metros y aguja de 38mm', 45000.00, 12, 12, 'Wacker Neuson', 'M2000', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('3beba5dd-4b06-51f5-a0b9-0435054f47e7', 'de906bd2-ace6-59b2-9054-dfa034fc8646', '68b08ad7-aeb8-558e-99b8-a16e4e0e6d3f', 'Allanadora de Pavimento 36 Pulg', 'Helicóptero alisador de pisos de concreto motor 6.5HP', 125000.00, 4, 4, 'Wacker Neuson', 'CT36', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('efe378f7-2d70-5e0b-9b57-2274785d9512', 'de906bd2-ace6-59b2-9054-dfa034fc8646', 'fc2d3b90-a803-5123-9a75-095163dac818', 'Cortadora de Pavimento y Concreto', 'Cortadora con disco diamantado de 14 pulg y tanque de agua', 95000.00, 6, 6, 'Husqvarna', 'FS400', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('bd8272f8-e937-5ff6-a54d-188170b95f71', 'dc17e3d0-e1be-5acc-9cf8-b7513cadee05', 'a075283e-6a7d-5e76-88b8-99c35982b340', 'Planta Eléctrica Diesel 10 kVA', 'Generador insonorizado trifásico de respaldo continuo', 180000.00, 5, 5, 'Cummins', 'C10D6', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('6c39cdde-b17e-5638-92b4-645edc25b1f6', 'dc17e3d0-e1be-5acc-9cf8-b7513cadee05', 'd390b029-1f50-5e5d-a90e-2016119a72e0', 'Motobomba Autocebante 3 Pulg', 'Bomba de achique para aguas turbias con motor Honda 8HP', 68000.00, 8, 8, 'Barnes', 'HE3H', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('ec118821-47c5-53e0-8009-b058581c11a6', 'dc17e3d0-e1be-5acc-9cf8-b7513cadee05', 'f234c924-4756-5a59-a35e-1fe8fdb44497', 'Torre de Iluminación 4 Focos LED', 'Torre móvil diésel con mástil extensible de 9 metros', 210000.00, 4, 4, 'Atlas Copco', 'HiLight V4', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('b66cf687-7b93-5c59-9bc1-d0b1937828d5', 'dc17e3d0-e1be-5acc-9cf8-b7513cadee05', 'ffe9f9f9-9b4f-5e4f-b6bf-4b098c6b9c22', 'Compresor de Aire Portátil 185 CFM', 'Compresor diésel para dos martillos neumáticos', 240000.00, 3, 3, 'Ingersoll Rand', 'P185', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('38e48097-fd1f-51e1-ae92-744836e8d9be', '2ab65d8b-689d-57e8-aff8-b172904dc8ba', '2a472a0b-eade-5134-93d4-e1150e313e28', 'Compactador Canguro Tipo Rana 4HP', 'Apisonador vertical para zanjas y cimientos estrechos', 70000.00, 7, 7, 'Wacker Neuson', 'BS50-2', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('3414f083-8180-5a10-83a5-a6e708fe2a84', '2ab65d8b-689d-57e8-aff8-b172904dc8ba', '06511c04-473e-52d7-943e-fc66440358ee', 'Placa Compactadora Unidireccional', 'Bailarina compactadora de suelos granulares y adoquín', 72000.00, 6, 6, 'Weber MT', 'CF2', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('dc81fe7b-8e72-5aa5-85ae-f2c28114270d', '2ab65d8b-689d-57e8-aff8-b172904dc8ba', '3a8db36a-1dd1-59ca-b19c-c5e5539ff67e', 'Nivel Láser Autonivelante 360', 'Nivel óptico láser de línea verde con trípode y mira', 35000.00, 10, 10, 'Bosch', 'GLL 3-80 G', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;
INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, precio_dia, cantidad_total, cantidad_disponible, marca, modelo, estado_publicacion, iva_porcentaje, iva_aplica) VALUES ('e4f5b018-b865-53c8-b316-a255772564f4', '2ab65d8b-689d-57e8-aff8-b172904dc8ba', 'fa0dd009-b1ca-5e50-8ee7-9dfa0200be02', 'Cortadora de Ladrillo y Baldosa', 'Mesa de corte húmedo con disco de diamante 10 pulg', 48000.00, 8, 8, 'Rubi', 'DU-200', 'PUBLICADA', 19.00, true) ON CONFLICT (id) DO NOTHING;

-- 7. RESERVAS Y DETALLES DE RESERVA (22 registros)
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('360b40d6-4af5-5bce-928b-5f1d1e812f1e', '31d3a088-0fcd-5ac4-90d1-1ef871d4f9ba', 'FINALIZADA', (CURRENT_DATE + (-149 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-149 * INTERVAL '1 day'))::date + (4 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (160 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('106c5936-773c-5241-982c-1cce33c872af', '360b40d6-4af5-5bce-928b-5f1d1e812f1e', '158b1e4b-fd44-5776-9edf-58f1b9797cc3', 1, 750000.00, 19.00, true, 3750000.00, 712500.00, 4462500.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('89b99528-5f65-57c1-8a17-c1d7e6e532e6', 'd3ea6ee7-6b9a-54ed-81bf-146dd391fc13', 'FINALIZADA', (CURRENT_DATE + (-118 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-118 * INTERVAL '1 day'))::date + (6 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (130 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('9db0489b-ac78-5568-9bc0-1529dbaa7192', '89b99528-5f65-57c1-8a17-c1d7e6e532e6', '5845de19-401a-5373-a092-dad4fb6ed2c8', 1, 480000.00, 19.00, true, 3360000.00, 638400.00, 3998400.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('67d2c8f3-a3c2-5489-93f9-2412574ef1f6', '4a6f9b23-15c9-5f94-a502-0a1bd949aa42', 'FINALIZADA', (CURRENT_DATE + (-117 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-117 * INTERVAL '1 day'))::date + (3 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (130 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('fa227cd4-0799-5b65-891c-76f388b4e017', '67d2c8f3-a3c2-5489-93f9-2412574ef1f6', '90afd962-792b-5d3d-91ba-4191bdbc393c', 2, 65000.00, 19.00, true, 520000.00, 98800.00, 618800.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('09977755-e425-5a3c-929e-d5b012c34e18', 'e9fd0d0e-c30d-5134-a984-e95c09bb3f4d', 'FINALIZADA', (CURRENT_DATE + (-86 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-86 * INTERVAL '1 day'))::date + (9 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (100 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('532e6101-5414-5812-a60a-8e67985794d2', '09977755-e425-5a3c-929e-d5b012c34e18', 'f38859cf-d038-5d66-8300-91f0a2436a58', 4, 25000.00, 19.00, true, 1000000.00, 190000.00, 1190000.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('88786dc0-4ec7-5993-8089-5521be72df77', '392b2fc5-6ac4-5a5f-9c3c-92d13b0ca9f0', 'FINALIZADA', (CURRENT_DATE + (-90 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-90 * INTERVAL '1 day'))::date + (5 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (100 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('1c996cf8-fd8b-54ac-8c18-6fe37b01eb86', '88786dc0-4ec7-5993-8089-5521be72df77', '1986088e-4a53-5017-90c1-8493cd39c239', 1, 75000.00, 19.00, true, 450000.00, 85500.00, 535500.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('55efd41d-04d8-503f-8213-89e3c015c83b', 'b2d64dc4-7646-5722-a898-78870d821241', 'FINALIZADA', (CURRENT_DATE + (-59 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-59 * INTERVAL '1 day'))::date + (11 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (70 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('eb55711a-35f5-558d-80d1-8c0f204c9161', '55efd41d-04d8-503f-8213-89e3c015c83b', 'bd8272f8-e937-5ff6-a54d-188170b95f71', 1, 180000.00, 19.00, true, 2160000.00, 410400.00, 2570400.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('35eaad50-4f1d-5ddd-9095-0f91b16068f1', 'ab6e3792-723b-5c9e-96f1-d9442d35c7f8', 'FINALIZADA', (CURRENT_DATE + (-58 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-58 * INTERVAL '1 day'))::date + (4 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (70 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('d80c00ab-362e-58ec-89df-0680f43e6fd1', '35eaad50-4f1d-5ddd-9095-0f91b16068f1', '38e48097-fd1f-51e1-ae92-744836e8d9be', 1, 70000.00, 19.00, true, 350000.00, 66500.00, 416500.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('ea84ce34-fea5-5395-a818-41a0777d57da', '2e2c1c6f-82fd-596a-b6ef-f17c287518b7', 'FINALIZADA', (CURRENT_DATE + (-57 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-57 * INTERVAL '1 day'))::date + (7 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (70 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('95764625-35a1-5e57-a957-68df78e30733', 'ea84ce34-fea5-5395-a818-41a0777d57da', '78bbae1e-35e5-5042-aacf-23a709efbe22', 1, 320000.00, 19.00, true, 2560000.00, 486400.00, 3046400.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('649dd926-ead5-565b-8197-f5c6a1e124e7', '80df59a0-e01c-599f-9bd1-81b3d894b8f7', 'FINALIZADA', (CURRENT_DATE + (-26 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-26 * INTERVAL '1 day'))::date + (2 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (40 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('942ecd2f-6bb1-57ba-b013-6739a45c6a43', '649dd926-ead5-565b-8197-f5c6a1e124e7', '7fcece97-5ac2-5e67-bd63-0ef2d04e5ecd', 2, 110000.00, 19.00, true, 660000.00, 125400.00, 785400.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('7a2d9c6a-4ec9-5174-bc93-fa2862b05869', '86cae8b5-c63a-5ab6-bb9a-44db5b320b0e', 'FINALIZADA', (CURRENT_DATE + (-30 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-30 * INTERVAL '1 day'))::date + (14 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (40 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('9283d3d8-db51-51ca-90c6-8af485fbb797', '7a2d9c6a-4ec9-5174-bc93-fa2862b05869', '2a048659-b2f7-5549-b834-bfad32c1dce3', 2, 85000.00, 19.00, true, 2550000.00, 484500.00, 3034500.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('c053e8dd-25dd-5192-a264-411f2da7f765', '8f30449f-7bec-5a8b-b06e-d18f59fb5515', 'FINALIZADA', (CURRENT_DATE + (-29 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-29 * INTERVAL '1 day'))::date + (3 * INTERVAL '1 day'))::date, CASE WHEN 'FINALIZADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (40 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('a0213b11-d4ce-5a5e-99c8-7332a74382c2', 'c053e8dd-25dd-5192-a264-411f2da7f765', 'cdfb7ebc-f525-5c26-af8a-aed8756e1642', 2, 45000.00, 19.00, true, 360000.00, 68400.00, 428400.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('f179fae1-05fc-518f-bc9b-b40ee5bf8b7b', '8bd39421-1147-5ec9-8d86-6539e7bed0ff', 'CONFIRMADA', (CURRENT_DATE + (2 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (2 * INTERVAL '1 day'))::date + (6 * INTERVAL '1 day'))::date, CASE WHEN 'CONFIRMADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('f4104bff-dee0-5689-96ff-c4d3c7de2314', 'f179fae1-05fc-518f-bc9b-b40ee5bf8b7b', '6c39cdde-b17e-5638-92b4-645edc25b1f6', 1, 68000.00, 19.00, true, 476000.00, 90440.00, 566440.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('e7e95a67-60d3-551a-a364-df757c61e884', 'c6e3e078-5fda-5dca-b504-c3a4cc1e760a', 'CONFIRMADA', (CURRENT_DATE + (3 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (3 * INTERVAL '1 day'))::date + (5 * INTERVAL '1 day'))::date, CASE WHEN 'CONFIRMADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('f6ee8ed2-cbe8-5c40-96fe-28b14d36e3fb', 'e7e95a67-60d3-551a-a364-df757c61e884', '3414f083-8180-5a10-83a5-a6e708fe2a84', 1, 72000.00, 19.00, true, 432000.00, 82080.00, 514080.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('99fb94e6-081c-50bb-b65a-559258ad40bd', '2e917f55-8a8b-56a0-a465-129e108152c9', 'CONFIRMADA', (CURRENT_DATE + (4 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (4 * INTERVAL '1 day'))::date + (9 * INTERVAL '1 day'))::date, CASE WHEN 'CONFIRMADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('ef1d6528-8e61-5d5d-9c88-8ba1933f18fb', '99fb94e6-081c-50bb-b65a-559258ad40bd', '90240f35-7c27-5dbf-8ec9-8b3be6131bdc', 1, 290000.00, 19.00, true, 2900000.00, 551000.00, 3451000.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('9e00a326-0df2-57b6-950a-c609342a8597', '89c9e56c-f958-595d-be2f-ce1540cb7a85', 'EN_CURSO', (CURRENT_DATE + (0 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (0 * INTERVAL '1 day'))::date + (4 * INTERVAL '1 day'))::date, CASE WHEN 'EN_CURSO' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('6ccbb974-ce25-551a-8693-42c57b43c746', '9e00a326-0df2-57b6-950a-c609342a8597', 'f62ee34b-e2d1-5a76-bb4a-f4da2ecc8112', 2, 55000.00, 19.00, true, 550000.00, 104500.00, 654500.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('501c4a92-036d-5563-9213-e4694270b39d', '9076a879-69b7-559c-b790-437b232e3e2d', 'EN_CURSO', (CURRENT_DATE + (1 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (1 * INTERVAL '1 day'))::date + (13 * INTERVAL '1 day'))::date, CASE WHEN 'EN_CURSO' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('ab3e6397-407a-5868-948f-bbc43ea3ac9b', '501c4a92-036d-5563-9213-e4694270b39d', 'e02df6cd-8874-55ac-ae5f-99b9a3ab6468', 2, 38000.00, 19.00, true, 1064000.00, 202160.00, 1266160.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('c520260b-7ec6-5245-8b1b-16942203f9da', '2926aa19-8a6f-5c0e-8138-650680919a39', 'EN_CURSO', (CURRENT_DATE + (2 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (2 * INTERVAL '1 day'))::date + (2 * INTERVAL '1 day'))::date, CASE WHEN 'EN_CURSO' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('453c7cc3-e83e-5238-9e20-b6f0d92b4b00', 'c520260b-7ec6-5245-8b1b-16942203f9da', '3beba5dd-4b06-51f5-a0b9-0435054f47e7', 1, 125000.00, 19.00, true, 375000.00, 71250.00, 446250.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('1002b082-0567-522f-8b2c-a2fcc61dcc4e', 'a31f8d9e-d5fc-5c8e-96dd-bc1dec4224b3', 'EN_CURSO', (CURRENT_DATE + (3 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (3 * INTERVAL '1 day'))::date + (6 * INTERVAL '1 day'))::date, CASE WHEN 'EN_CURSO' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('f37d9289-c904-5857-b152-1b80e4049f6b', '1002b082-0567-522f-8b2c-a2fcc61dcc4e', 'ec118821-47c5-53e0-8009-b058581c11a6', 1, 210000.00, 19.00, true, 1470000.00, 279300.00, 1749300.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('69d7a3d9-22ba-59a5-80fb-36423f1c21eb', '273ef134-050a-57ad-8261-2e00082ef874', 'PENDIENTE', (CURRENT_DATE + (4 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (4 * INTERVAL '1 day'))::date + (3 * INTERVAL '1 day'))::date, CASE WHEN 'PENDIENTE' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), CURRENT_TIMESTAMP + (1 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('8ff61aa3-0443-52f7-8e99-41b1db446a69', '69d7a3d9-22ba-59a5-80fb-36423f1c21eb', '1e75f6ff-40c5-5322-a18f-f54b3d4ded93', 2, 42000.00, 19.00, true, 336000.00, 63840.00, 399840.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('ae279f70-914b-5eb5-8d6d-843d83feed50', '27a06f3e-012d-5f45-a0d4-32dba8ce15a1', 'PENDIENTE', (CURRENT_DATE + (0 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (0 * INTERVAL '1 day'))::date + (19 * INTERVAL '1 day'))::date, CASE WHEN 'PENDIENTE' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (10 * INTERVAL '1 day'), CURRENT_TIMESTAMP + (1 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('c40e2382-0885-533b-98fb-bdf349610492', 'ae279f70-914b-5eb5-8d6d-843d83feed50', '7666ef86-ebab-5254-9273-03d779328b8d', 10, 18000.00, 19.00, true, 3600000.00, 684000.00, 4284000.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('81611171-c940-54a3-b464-e371fd0fb098', '31d3a088-0fcd-5ac4-90d1-1ef871d4f9ba', 'CANCELADA', (CURRENT_DATE + (-29 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-29 * INTERVAL '1 day'))::date + (2 * INTERVAL '1 day'))::date, CASE WHEN 'CANCELADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (40 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('e123eb7e-d37e-52af-8c1c-5db6fbca5a45', '81611171-c940-54a3-b464-e371fd0fb098', 'efe378f7-2d70-5e0b-9b57-2274785d9512', 1, 95000.00, 19.00, true, 285000.00, 54150.00, 339150.00)
ON CONFLICT (id) DO NOTHING;
INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_confirmacion, fecha_creacion, fecha_expiracion_pendiente)
VALUES ('855b3c26-09be-5899-96f1-f235aea184f3', 'd3ea6ee7-6b9a-54ed-81bf-146dd391fc13', 'CANCELADA', (CURRENT_DATE + (-58 * INTERVAL '1 day'))::date, ((CURRENT_DATE + (-58 * INTERVAL '1 day'))::date + (3 * INTERVAL '1 day'))::date, CASE WHEN 'CANCELADA' != 'PENDIENTE' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP - (70 * INTERVAL '1 day'), NULL)
ON CONFLICT (id) DO NOTHING;
INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total)
VALUES ('cb73d673-3452-5804-bd1f-caa7d39f0b3e', '855b3c26-09be-5899-96f1-f235aea184f3', 'b66cf687-7b93-5c59-9bc1-d0b1937828d5', 1, 240000.00, 19.00, true, 960000.00, 182400.00, 1142400.00)
ON CONFLICT (id) DO NOTHING;

-- 8. PAGOS (20 registros)
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('627a9717-71d5-54a0-855e-3bb980156ab8', '360b40d6-4af5-5bce-928b-5f1d1e812f1e', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0001-360b40d6', 4462500.00, 'COP', CURRENT_TIMESTAMP - (155 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (158 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('e00a9976-c9ad-5608-890c-247484604d2d', '89b99528-5f65-57c1-8a17-c1d7e6e532e6', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0002-89b99528', 3998400.00, 'COP', CURRENT_TIMESTAMP - (125 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (128 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('77659368-645f-5fa6-a04b-aba9252c5d38', '67d2c8f3-a3c2-5489-93f9-2412574ef1f6', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0003-67d2c8f3', 618800.00, 'COP', CURRENT_TIMESTAMP - (125 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (128 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('c9c2d816-3e75-5178-82f3-1a21ae3eaab1', '09977755-e425-5a3c-929e-d5b012c34e18', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0004-09977755', 1190000.00, 'COP', CURRENT_TIMESTAMP - (95 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (98 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('8761bbbe-fb49-5c40-b455-5ccb54b1616b', '88786dc0-4ec7-5993-8089-5521be72df77', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0005-88786dc0', 535500.00, 'COP', CURRENT_TIMESTAMP - (95 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (98 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('06bd8da9-5e33-5a88-b21f-d2bb37f39a43', '55efd41d-04d8-503f-8213-89e3c015c83b', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0006-55efd41d', 2570400.00, 'COP', CURRENT_TIMESTAMP - (65 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (68 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('c35ee7f6-f3d6-5f95-8b2e-b1185c6de2a2', '35eaad50-4f1d-5ddd-9095-0f91b16068f1', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0007-35eaad50', 416500.00, 'COP', CURRENT_TIMESTAMP - (65 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (68 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('3ec783c9-daba-5e16-9505-f81a0093a578', 'ea84ce34-fea5-5395-a818-41a0777d57da', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0008-ea84ce34', 3046400.00, 'COP', CURRENT_TIMESTAMP - (65 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (68 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('53e0347c-c412-59d8-a6ed-a38ffb1f2690', '649dd926-ead5-565b-8197-f5c6a1e124e7', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0009-649dd926', 785400.00, 'COP', CURRENT_TIMESTAMP - (35 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (38 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('608f95b6-1349-5156-8e6b-e615d4008d2c', '7a2d9c6a-4ec9-5174-bc93-fa2862b05869', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0010-7a2d9c6a', 3034500.00, 'COP', CURRENT_TIMESTAMP - (35 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (38 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('52b3201b-69fc-5052-b3dd-3daace1f39f5', 'c053e8dd-25dd-5192-a264-411f2da7f765', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0011-c053e8dd', 428400.00, 'COP', CURRENT_TIMESTAMP - (35 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (38 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('455b15a6-9537-55ff-8125-099bef8c708a', 'f179fae1-05fc-518f-bc9b-b40ee5bf8b7b', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0012-f179fae1', 566440.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('82016924-06c4-573f-99ee-2ad7825b558f', 'e7e95a67-60d3-551a-a364-df757c61e884', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0013-e7e95a67', 514080.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('c7028d45-2672-5b75-b6c2-00125242288c', '99fb94e6-081c-50bb-b65a-559258ad40bd', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0014-99fb94e6', 3451000.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('fa5cce0b-c05c-5e05-902d-0c796a4e02b5', '9e00a326-0df2-57b6-950a-c609342a8597', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0015-9e00a326', 654500.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('688b425b-581f-57ee-a9e3-9dc6d785247c', '501c4a92-036d-5563-9213-e4694270b39d', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0016-501c4a92', 1266160.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('3aaf325b-d378-5035-80b7-de8297575e86', 'c520260b-7ec6-5245-8b1b-16942203f9da', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0017-c520260b', 446250.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('1001caa3-ca62-5a73-ba7e-b4a32fa8a4d8', '1002b082-0567-522f-8b2c-a2fcc61dcc4e', 'COMPLETADO', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0018-1002b082', 1749300.00, 'COP', CURRENT_TIMESTAMP - (5 * INTERVAL '1 day'), CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('0eed6221-9640-5275-9409-f425796ac650', '69d7a3d9-22ba-59a5-80fb-36423f1c21eb', 'PENDIENTE', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0019-69d7a3d9', 399840.00, 'COP', NULL, CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;
INSERT INTO pago (id, reserva_id, estado, proveedor_pago, transaccion_id, monto, moneda, fecha_pago, creado_en)
VALUES ('2f5c90ce-6be1-515f-bb7e-50e7ce032313', 'ae279f70-914b-5eb5-8d6d-843d83feed50', 'PENDIENTE', 'PASARELA_SIMULADA_BANCOLOMBIA', 'SIM-TX-2026-0020-ae279f70', 4284000.00, 'COP', NULL, CURRENT_TIMESTAMP - (8 * INTERVAL '1 day'))
ON CONFLICT (id) DO NOTHING;

-- Fin de datos de demostracion
