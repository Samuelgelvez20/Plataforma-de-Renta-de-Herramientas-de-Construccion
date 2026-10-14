# Plan de Tareas — Plataforma de Renta de Herramientas y Equipos de Construcción

Proyecto individual · 5 días · 5–7 horas por día

## 0. Decisiones del proyecto

| Tema | Decisión |
|---|---|
| Backend | Spring Boot (última versión estable al iniciar) + Java 21 + Maven |
| Seguridad | Spring Security + JWT, roles `ADMIN`, `PROVEEDOR`, `CLIENTE` |
| Base de datos | **PostgreSQL** (no MySQL), Spring Data JPA, scripts SQL propios |
| Frontend | **React + Vite** + React Router + Axios, estilos con Bootstrap 5 y tema propio |
| Pagos | **Simulados** (pasarela interna falsa, diseñada para cambiarla por una real) |
| Facturas | **PDF generado en el backend** |
| Imágenes | Carpeta local de imágenes preparadas; cada herramienta guarda el nombre/ruta de su imagen |
| Documentación | Swagger (springdoc-openapi), README, diagrama relacional, colección Postman |
| Despliegue | Tomcat embebido de Spring Boot |
| Equipo de desarrollo | **opencode** (escribe y ejecuta el código) |
| Asesor | **ChatGPT** (revisa, analiza resultados y errores) |

> **Pendiente por confirmar con el trainer:** el enunciado dice "Bootstrap o Tailwind (según lo autorice el trainer)" y que React/Vue se permiten si él lo autoriza. Pedir autorización de React + Bootstrap **hoy mismo**. Si la negara, el plan B es HTML/CSS/JS puro + Bootstrap; las historias del Día 4 y 5 se mantienen, solo cambia la implementación.

## 1. Forma de trabajar con opencode y ChatGPT

**Ciclo de cada historia de usuario:**
1. Tú copias a opencode la historia (criterios de aceptación incluidos) y el contexto mínimo (estructura de paquetes y entidades ya hechas).
2. opencode implementa y ejecuta (compilar, levantar, probar).
3. Tú verificas con Swagger o Postman que se cumplan los criterios.
4. Si algo falla o dudas del diseño, pasas a ChatGPT el error o el resultado y le pides análisis.
5. Commit con mensaje claro (`feat: ...`, `fix: ...`). **Mínimo 1 commit por historia**, el enunciado pide commits frecuentes.

**Reglas fijas para opencode (pégalas al inicio de cada sesión):**
- Arquitectura por capas: `controller`, `service`, `repository`, `dto`, `entity`, `exception`, `config`, `security`.
- Nunca exponer entidades en los controllers, siempre DTOs.
- Validaciones con Bean Validation (`@Valid`).
- Comentarios explicativos donde la lógica no sea obvia.
- Cada endpoint documentado en Swagger con ejemplos.
- No cambiar el esquema de la BD sin avisarlo: el esquema vive en `schema.sql`.

**Rol de ChatGPT:** revisar el diseño del esquema antes de implementarlo (Día 1), analizar errores que opencode no resuelva, revisar el README y la lista final de requisitos (Día 5). No escribe código del proyecto.

**Prioridades:** `P0` = obligatorio del enunciado, sin esto no se entrega · `P1` = importante, suma calidad · `P2` = pulido/opcional, se recorta si hay retraso.

## 2. Modelo de datos de referencia (a validar con ChatGPT en el Día 1)

Entidades: `usuario` (base de login con rol), `proveedor`, `cliente`, `categoria`, `herramienta`, `reserva`, `pago`, `factura`, `reporte_dano`.

Relaciones clave:
- `usuario` 1–1 `proveedor` o `cliente` según el rol.
- `proveedor` 1–N `herramienta`; `categoria` 1–N `herramienta`.
- `cliente` 1–N `reserva`; `herramienta` 1–N `reserva`.
- `reserva` 1–1 `pago`; `pago` 1–1 `factura`.
- `reserva` 1–N `reporte_dano`.

Estados sugeridos: reserva `PENDIENTE → ACEPTADA/RECHAZADA → PAGADA → ENTREGADA → DEVUELTA` (o `CANCELADA`); herramienta `DISPONIBLE / ALQUILADA / EN_MANTENIMIENTO / DANADA`.

---

## DÍA 1 — Cimientos, base de datos y autenticación

**Objetivo:** dejar el proyecto corriendo con PostgreSQL, el esquema completo, login con JWT y roles funcionando. Todo lo demás depende de este día.

**Habilidades:** Spring Initializr, configuración de Maven, Spring Data JPA, PostgreSQL, Spring Security 6, JWT, manejo global de excepciones, Swagger, Git.

### HU-1.1 · Inicializar proyecto y repositorio `P0`
*Como desarrollador quiero un proyecto Spring Boot configurado y versionado para empezar a construir.*
- Proyecto creado con Java 21, Maven y dependencias: Web, Security, Data JPA, Validation, PostgreSQL Driver, Lombok, springdoc-openapi, librería JWT.
- Repositorio privado en GitHub con nombre `Proyecto_RentaHerramientas_ApellidoNombre`, compartido con el instructor.
- Estructura de paquetes por capas creada.
- `application.properties` con la conexión a PostgreSQL y el puerto.
- `.gitignore` correcto (sin credenciales ni `target/`).

### HU-1.2 · Esquema de base de datos y entidades `P0`
*Como sistema quiero un esquema relacional normalizado para guardar usuarios, herramientas, reservas y pagos.*
- ChatGPT revisa el modelo de la sección 2 antes de implementarlo.
- Script `schema.sql` con tablas, llaves foráneas, restricciones e índices (por ejemplo sobre estado de herramienta y fechas de reserva).
- Entidades JPA con relaciones coherentes con el script.
- La aplicación arranca contra PostgreSQL sin errores.
- Diagrama relacional exportado como imagen (se puede refinar el Día 5).

### HU-1.3 · Registro e inicio de sesión con JWT `P0`
*Como cliente quiero registrarme e iniciar sesión para obtener un token y usar la plataforma.*
- `POST /api/auth/register` (cliente y proveedor) y `POST /api/auth/login`.
- Contraseñas con BCrypt.
- El token incluye el rol y tiene expiración.
- Filtro JWT que valida el token en cada petición.

### HU-1.4 · Control de acceso por roles `P0`
*Como administrador quiero que cada rol solo acceda a lo que le corresponde.*
- Rutas protegidas por rol (`/api/admin/**`, `/api/proveedor/**`, `/api/cliente/**`) y rutas públicas para explorar herramientas.
- Respuestas 401 y 403 con cuerpo JSON claro.
- Usuario administrador inicial creado por script SQL.

### HU-1.5 · Manejo de errores y CORS `P0`
*Como frontend quiero errores consistentes y poder comunicarme con la API.*
- `@RestControllerAdvice` con excepciones personalizadas (`RecursoNoEncontrado`, `ReglaDeNegocio`, validación, etc.) y formato de error único.
- CORS configurado para el origen del frontend de desarrollo (Vite, normalmente `localhost:5173`).

### HU-1.6 · Swagger con soporte JWT `P1`
- Swagger UI accesible, con botón "Authorize" para pegar el token.
- Endpoints de autenticación documentados con ejemplos.

**Definición de terminado del día:** registro y login funcionan desde Swagger, un endpoint de cada rol responde 200 con el token correcto y 403 con el incorrecto, todo commiteado.

---

## DÍA 2 — Usuarios, herramientas y disponibilidad

**Objetivo:** tener los CRUD principales funcionando y el inventario con imágenes y control de disponibilidad.

**Habilidades:** CRUD REST con DTOs, mapeo entidad–DTO, Bean Validation, paginación y filtros con Spring Data, servir recursos estáticos, consultas JPQL / Specifications, scripts de datos de prueba.

### HU-2.1 · Gestión de proveedores y clientes por el administrador `P0`
*Como administrador quiero registrar, editar, listar y eliminar proveedores y clientes.*
- CRUD completo en `/api/admin/proveedores` y `/api/admin/clientes`.
- Eliminación con regla de negocio: no se elimina quien tenga reservas activas (baja lógica recomendada).
- Listados paginados.

### HU-2.2 · CRUD de categorías y herramientas `P0`
*Como proveedor quiero agregar, editar y eliminar mis herramientas con costo y disponibilidad.*
- Campos: nombre, descripción, categoría, precio por día, estado, imagen, proveedor.
- El proveedor solo ve y modifica **sus** herramientas; el administrador puede gestionar todas.
- Validaciones (precio mayor que 0, nombre obligatorio).

### HU-2.3 · Imágenes de herramientas `P0`
*Como proveedor quiero que mis herramientas tengan imagen para que el cliente las identifique.*
- Carpeta de imágenes del proyecto (preparada por ti: se crea o se buscan imágenes) servida como recurso estático.
- La herramienta guarda el nombre o ruta de su imagen y la API devuelve la URL completa.
- Imagen por defecto cuando no hay una asignada.
- `P2`: endpoint opcional de subida (multipart) hacia esa misma carpeta.

### HU-2.4 · Exploración pública con búsqueda y filtros `P0`
*Como cliente quiero buscar herramientas por categoría, texto y disponibilidad.*
- `GET /api/herramientas` público, paginado, con filtros por categoría, texto, rango de precio y rango de fechas disponibles.
- `GET /api/herramientas/{id}` con detalle y precio.

### HU-2.5 · Control de disponibilidad por fechas `P0`
*Como sistema quiero saber si una herramienta está libre en un rango de fechas.*
- Consulta que detecta solapamiento con reservas activas (aceptadas, pagadas o entregadas).
- Servicio reutilizable `estaDisponible(herramienta, desde, hasta)` que usará el Día 3.
- Pruebas unitarias del servicio con casos límite (mismo día, fechas contiguas, solapamiento parcial).

### HU-2.6 · Datos de prueba `P0`
- Script `data.sql` con **al menos 20 registros por entidad** (usuarios, proveedores, clientes, herramientas, reservas, pagos), con datos realistas y coherentes entre sí.
- Se pueden cargar de nuevo desde cero sin errores.

**Definición de terminado del día:** desde Swagger se crean, editan y filtran herramientas con cada rol, la disponibilidad responde bien en los casos límite y la BD arranca poblada con el script.

---

## DÍA 3 — Reservas, devoluciones, pagos simulados y facturas

**Objetivo:** completar el flujo de negocio central de principio a fin: reservar → aceptar → pagar → factura → entrega → devolución.

**Habilidades:** transacciones (`@Transactional`), máquinas de estado, reglas de negocio, patrón Strategy/interfaz para la pasarela de pago, generación de PDF (OpenPDF o iText), descarga de archivos desde la API.

### HU-3.1 · Crear reserva con validación `P0`
*Como cliente quiero seleccionar fechas y reservar una herramienta.*
- `POST /api/cliente/reservas` con herramienta, fecha inicio y fecha fin.
- Rechazo claro si las fechas son inválidas o la herramienta no está disponible.
- Cálculo automático del total (días × precio por día).
- Estado inicial `PENDIENTE`.

### HU-3.2 · Aceptar o rechazar solicitudes `P0`
*Como proveedor quiero decidir sobre las solicitudes de alquiler de mis herramientas.*
- Lista de solicitudes de mis herramientas con filtro por estado.
- Aceptar o rechazar (con motivo opcional); solo el dueño de la herramienta puede hacerlo.
- Revalidar disponibilidad al aceptar para evitar doble reserva concurrente.

### HU-3.3 · Pago simulado `P0`
*Como cliente quiero pagar en línea mi reserva aceptada.*
- Interfaz `PasarelaPago` con una implementación `PasarelaSimulada` (aprueba o rechaza según reglas simples, por ejemplo tarjetas de prueba o un parámetro) para poder cambiarla por una real después.
- `POST /api/cliente/reservas/{id}/pagar` registra el pago con método, monto, referencia y estado.
- Validación: monto igual al total, la reserva debe estar aceptada, no se paga dos veces.
- Al aprobarse el pago, la reserva pasa a `PAGADA`.
- Historial de pagos del cliente.

### HU-3.4 · Facturación automática en PDF `P0`
*Como cliente y proveedor quiero una factura al completarse el pago.*
- Factura generada automáticamente al aprobar el pago, con número consecutivo, datos de cliente y proveedor, detalle (herramienta, días, tarifa), total y fecha.
- `GET /api/facturas/{id}/pdf` descarga el PDF; solo lo ven las partes involucradas y el administrador.
- El proveedor puede ver las facturas de sus alquileres.

### HU-3.5 · Entrega y devolución `P0`
*Como proveedor quiero registrar la entrega y la devolución de mis herramientas.*
- Registrar entrega: reserva `ENTREGADA`, herramienta `ALQUILADA`.
- Confirmar devolución: reserva `DEVUELTA`, herramienta de nuevo `DISPONIBLE` o `DANADA`/`EN_MANTENIMIENTO` según el estado reportado.
- Detección de reservas vencidas (fecha fin pasada sin devolución) marcadas como retrasadas.

### HU-3.6 · Reporte de daños `P1`
*Como proveedor quiero reportar daños al devolverse una herramienta.*
- Reporte con descripción y estado, asociado a la reserva y a la herramienta.
- El administrador puede listarlos y resolverlos o cerrarlos.

### HU-3.7 · Historial de alquileres `P0`
- Cliente: sus alquileres con estado. Proveedor: alquileres de sus herramientas. Administrador: todos.

**Definición de terminado del día:** flujo completo ejecutado una vez desde Swagger (reservar → aceptar → pagar → descargar PDF → entregar → devolver) y probado con Postman en la secuencia correcta, más al menos un flujo negativo (fechas solapadas).

---

## DÍA 4 — Reportes y frontend (cliente y proveedor)

**Objetivo:** cerrar el backend con los reportes y construir la interfaz de usuario base con los flujos de cliente y proveedor funcionando de verdad contra la API.

**Habilidades:** consultas agregadas (JPQL / `GROUP BY`), React con hooks, React Router con rutas protegidas, Context API para sesión JWT, Axios con interceptores, Bootstrap 5, diseño responsivo, UX de formularios.

### HU-4.1 · Endpoints de reportes `P0` (primeras 2 horas)
*Como administrador quiero estadísticas del negocio.*
- Ingresos por alquileres (total y por mes).
- Herramientas más alquiladas y clientes frecuentes.
- Disponibilidad y estado actual de las herramientas.
- Reporte de rentabilidad por herramienta.
- Documentados en Swagger.

### HU-4.2 · Base del frontend `P0`
*Como usuario quiero una aplicación bonita, responsiva y con navegación clara.*
- Proyecto Vite + React, estructura por carpetas (`pages`, `components`, `services`, `context`, `routes`).
- Layout con barra de navegación que cambia según rol, footer y estilos propios sobre Bootstrap (paleta, tipografía, tarjetas) que le den identidad de marca.
- Diseño adaptable a móvil, tableta y escritorio.
- Cliente Axios con el token en cada petición y redirección al login si el token expira (401).

### HU-4.3 · Login, registro y rutas protegidas `P0`
- Pantallas de login y registro con validación visual y mensajes de error del backend.
- `AuthContext` que guarda sesión y rol; rutas protegidas por rol; redirección al panel correcto tras iniciar sesión.

### HU-4.4 · Experiencia del cliente `P0`
*Como cliente quiero explorar, reservar, pagar y revisar mi historial.*
- Catálogo con tarjetas, imágenes, búsqueda, filtro por categoría y por disponibilidad.
- Detalle de herramienta con selector de fechas, cálculo del total en vivo y botón de reservar.
- Mis reservas con estado y botón de pago.
- Pantalla de pago simulado con confirmación y descarga de la factura en PDF.
- Historial de alquileres.

### HU-4.5 · Experiencia del proveedor `P0`
*Como proveedor quiero gestionar mi inventario y mis reservas.*
- Lista de mis herramientas con estado y disponibilidad; formulario para crear y editar con imagen y descripción.
- Bandeja de solicitudes con aceptar o rechazar.
- Acciones de entrega y confirmación de devolución, con reporte de daños.
- Listado de facturas generadas.

### HU-4.6 · Estados de interfaz `P1`
- Indicadores de carga, mensajes vacíos ("aún no tienes reservas"), notificaciones (toasts) de éxito o error, confirmación antes de borrar.

**Definición de terminado del día:** un cliente nuevo puede registrarse, reservar y pagar desde el navegador, y un proveedor puede aceptar y registrar la devolución, todo sin tocar Swagger.

---

## DÍA 5 — Panel de administración, pulido, pruebas y entrega

**Objetivo:** completar el rol de administrador, dejar todo bonito y probado, y preparar los entregables.

**Habilidades:** gráficos en React (Recharts o Chart.js), pruebas con JUnit y Mockito, `@SpringBootTest` / MockMvc, Postman (colecciones y variables), redacción técnica (README), diagramas ER, empaquetado Maven.

### HU-5.1 · Panel del administrador `P0`
*Como administrador quiero supervisar toda la plataforma.*
- Lista de usuarios (proveedores y clientes) con crear, editar y eliminar.
- Historial global de alquileres, supervisión de pagos y facturación.
- Gestión de incidencias y reportes de daños.
- Verificación del estado de equipos alquilados.

### HU-5.2 · Dashboard de reportes `P0`
- Tarjetas de resumen (ingresos, alquileres activos, herramientas disponibles) y gráficos de ingresos por mes, herramientas más alquiladas y clientes frecuentes.

### HU-5.3 · Pulido visual y de experiencia `P1`
- Revisión completa en móvil, tableta y escritorio.
- Coherencia de colores, espaciados, iconos, estados vacíos y errores.
- Imagen por defecto y manejo de imágenes rotas.
- Datos de demostración que se vean bien en el catálogo.

### HU-5.4 · Pruebas `P0`
- Pruebas unitarias de servicios críticos (disponibilidad, cálculo de total, pago, estados de reserva).
- Al menos algunas pruebas de integración de endpoints con seguridad por rol.
- Colección Postman exportada con: CRUD (POST, GET, PUT, DELETE), autenticación JWT, protección de rutas, y el flujo completo de alquiler y pago.

### HU-5.5 · Documentación y entregables `P0`
- `README.md` con: nombre y descripción, tecnologías (Java 21, Spring Boot, PostgreSQL, JWT, React), instrucciones para clonar, instalar y ejecutar (backend y frontend), ejemplo de `application.properties`, diagrama relacional en imagen, lista de endpoints con ejemplos JSON, explicación de autenticación y roles, y guía para ejecutar pruebas.
- Swagger revisado: ejemplos de petición y respuesta, y explicación del uso del JWT.
- Scripts SQL finales (`schema.sql` y `data.sql`) verificados desde una base vacía.
- Diagrama relacional final exportado como imagen.
- ChatGPT revisa el README y la lista de requisitos del enunciado punto por punto.

### HU-5.6 · Ejecución y despliegue `P0`
- Backend empaquetado con Maven y ejecutándose en el Tomcat embebido.
- Prueba final de punta a punta con la base de datos reiniciada desde los scripts.
- Frontend configurado para apuntar a la URL de la API mediante variable de entorno.

### HU-5.7 · Extras `P2` (solo si sobra tiempo)
- Subida real de imágenes, paginación en todas las tablas del frontend, modo oscuro, exportar reportes a CSV.

**Definición de terminado del día:** repositorio limpio con commits frecuentes, proyecto levantado desde cero siguiendo solo el README, checklist del enunciado completo.

---

## 3. Si vamos retrasados: línea de corte

Se recortan en este orden: HU-5.7 → HU-5.3 → HU-4.6 → HU-3.6 → subida de imágenes (HU-2.3 `P2`). Todo lo `P0` del enunciado se protege.

## 4. Checklist final contra el enunciado

- [ ] Repo privado compartido con el instructor, con el nombre `Proyecto_RentaHerramientas_ApellidoNombre`
- [ ] Capas separadas, código comentado, commits frecuentes
- [ ] JWT con 3 roles, restricciones de acceso, errores personalizados, CORS
- [ ] CRUD de clientes, proveedores, herramientas, reservas y pagos
- [ ] Disponibilidad validada antes de confirmar reserva
- [ ] Pagos y facturación automática (PDF)
- [ ] Entregas, devoluciones y reportes de daños
- [ ] Reportes de ingresos, más alquiladas, clientes frecuentes y disponibilidad
- [ ] PostgreSQL: scripts de creación y ≥ 20 registros por entidad, índices
- [ ] Diagrama relacional en imagen
- [ ] README completo y Swagger con ejemplos
- [ ] Frontend responsivo con panel por rol
- [ ] Pruebas Postman y pruebas unitarias/integración
- [ ] Ejecución en Tomcat embebido
