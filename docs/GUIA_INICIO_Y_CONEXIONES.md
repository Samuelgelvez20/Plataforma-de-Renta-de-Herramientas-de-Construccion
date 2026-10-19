# Guía Rápida de Inicio, Conexiones y Ensayo de Demostración

**Proyecto:** Plataforma de Renta de Herramientas y Equipos de Construcción
**Tecnologías:** Java 21 + Spring Boot (Hexagonal / DDD), PostgreSQL 16 (Docker), React 19 + Vite

---

## 1. Mapa de Puertos y Servicios

| Componente | Dirección / Puerto | Tecnología | Descripción |
| :--- | :--- | :--- | :--- |
| **Base de Datos** | `localhost:5439` | PostgreSQL 16 Alpine | Instancia aislada para evitar colisiones con el puerto 5432 del host |
| **Backend API** | `http://localhost:8080` | Spring Boot 3.3 | API REST, Casos de uso DDD y seguridad JWT |
| **Swagger UI** | `http://localhost:8080/swagger-ui/index.html` | OpenAPI 3.0 | Documentación interactiva de la API |
| **OpenAPI Spec** | `http://localhost:8080/v3/api-docs` | JSON Spec | Contrato formal de servicios |
| **Frontend Web** | `http://localhost:5174` | React 19 + Vite | Interfaz de usuario SPA (usa 5174 si 5173 está ocupado) |

---

## 2. Comandos de Arranque (Paso a Paso)

### Paso 1: Base de Datos PostgreSQL 16 (Terminal 1)
```bash
# Iniciar contenedor existente:
docker start plataforma_renta_isolated_pg

# O crearlo desde cero si no existe:
docker run -d --name plataforma_renta_isolated_pg \
  -e POSTGRES_DB=plataforma_renta \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5439:5432 \
  postgres:16-alpine
```

### Paso 2: Backend Spring Boot (Terminal 2)
```bash
cd /home/papisamu/Documentos/JavaSpringBoot/Plataforma-de-Renta-de-Herramientas-y-Equipos-de-Construcci-n

# Compilar proyecto (si no está compilado aún)
mvn clean package -DskipTests

# Ejecutar el servicio backend con las variables requeridas
DB_URL="jdbc:postgresql://localhost:5439/plataforma_renta" \
DB_USERNAME="postgres" \
DB_PASSWORD="postgres" \
JWT_SECRET="clave-secreta-de-prueba-de-32-bytes-minimo-para-testing" \
SERVER_PORT="8080" \
java -jar platform/target/platform-0.1.0-SNAPSHOT.jar
```

### Paso 3: Frontend Web React + Vite (Terminal 3)
```bash
cd /home/papisamu/Documentos/JavaSpringBoot/Plataforma-de-Renta-de-Herramientas-y-Equipos-de-Construcci-n/frontend

# Instalar dependencias (solo primera vez)
npm install

# Iniciar servidor de desarrollo en puerto 5174
npm run dev -- --port 5174
```

---

## 3. Conexión a la Base de Datos desde DBeaver o Clientes SQL

Para conectar DBeaver, DataGrip, TablePlus o pgAdmin:

1. **Tipo de Conexión:** PostgreSQL
2. **Host:** `localhost` o `127.0.0.1`
3. **Port / Puerto:** `5439` *(Importante: no usar 5432)*
4. **Database:** `plataforma_renta`
5. **Username:** `postgres`
6. **Password:** `postgres`

### Esquema y Tablas Disponibles (`public`):
- **`usuario`**, **`cliente`**, **`propietario`**: Gestión de identidad, perfiles de negocio y datos tributarios.
- **`categoria`**, **`herramienta`**: Catálogo, tarifas por día e inventario de maquinaria.
- **`reserva`**, **`detalle_reserva`**: Transacciones de alquiler, cálculo de días, tarifas y disponibilidad en tiempo real.
- **`pago`**, **`factura`**, **`configuracion_fiscal`**: Pasarela simulada con idempotencia, desglose IVA 19% en COP y facturas emitidas.
- **`reporte_dano`**: Bitácora técnica de recepción de equipos tras alquiler.
- **`flyway_schema_history`**: Control de migraciones automáticas.

---

## 4. Cuentas de Demostración para Ensayos y Evaluación

| Rol | Correo Electrónico | Contraseña | Capacidades en la Plataforma |
| :--- | :--- | :--- | :--- |
| **PROPIETARIO** | `contacto@maquinariavalle.com` | `Password123!` | Publicar herramientas, gestionar inventario, aceptar/rechazar reservas y recibir equipos devueltos. |
| **CLIENTE** | `compras@colpatriaconstrucciones.com` | `Password123!` | Consultar catálogo, verificar disponibilidad por fecha, reservar maquinaria, pagar y descargar facturas PDF. |

> **Registro Público:** Puedes registrar cualquier cliente o propietario nuevo directamente en la pantalla de Registro (`/registro`). La asignación pública del rol `ADMIN` se encuentra bloqueada por diseño de seguridad.

---

## 5. Comprobación Rápida de Salud (Healthchecks)

Puedes ejecutar estos comandos en la terminal para verificar que todos los servicios responden correctamente:

```bash
# 1. Comprobar que PostgreSQL responde en el puerto 5439
docker exec plataforma_renta_isolated_pg pg_isready -U postgres

# 2. Comprobar OpenAPI / Swagger del Backend
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs

# 3. Comprobar catálogo de categorías
curl -s http://localhost:8080/api/v1/categorias

# 4. Comprobar frontend web
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5174
```
