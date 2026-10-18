INSERT INTO categoria (nombre, descripcion) VALUES
('Maquinaria Pesada', 'Excavadoras, retroexcavadoras, cargadores y compactadoras'),
('Herramienta Eléctrica', 'Taladros, rotomartillos, sierras y pulidoras'),
('Andamios y Estructuras', 'Andamios certificados, torres de trabajo y escaleras'),
('Equipos de Concreto', 'Mezcladoras, vibradores de concreto y allanadoras'),
('Generación y Bombeo', 'Generadores eléctricos, motobombas y torres de iluminación'),
('Herramienta Menor', 'Herramientas manuales y compactadores tipo rana')
ON CONFLICT (nombre) DO NOTHING;
