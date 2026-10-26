# Guía de Inicio, Conexiones y Ensayo de Demostración — OBRIXEN

**Proyecto:** OBRIXEN — Plataforma de Renta de Herramientas y Equipos de Construcción
**Stack:** Java 21 + Spring Boot 3.3 (Hexagonal / DDD), PostgreSQL 16 Alpine (Docker Compose), React 19 + TypeScript + Vite

---

## 1. Mapa de Puertos y Servicios

| Componente | Dirección / Puerto | Tecnología | Descripción |
| :--- | :--- | :--- | :--- |
| **Base de Datos** | `localhost:5439` | PostgreSQL 16 Alpine | Servicio `postgres` en Docker Compose con volumen persistente |
| **Backend API** | `http://localhost:8080` | Spring Boot 3.3 | API REST, Casos de uso DDD, Spring Security y JWT |
| **Swagger UI** | `http://localhost:8080/swagger-ui/index.html` | Springdoc OpenAPI 3 | Documentación interactiva con botón **Authorize (Bearer JWT)** |
| **OpenAPI Spec** | `http://localhost:8080/v3/api-docs` | JSON Spec | Contrato formal OpenAPI 3.0 con esquema `bearerAuth` |
| **Frontend Web** | `http://localhost:5173` | React 19 + Vite | Interfaz de usuario SPA moderna |

---

## 2. Flujo de Arranque Paso a Paso

### Paso 1: Preparar Variables de Entorno
Copia la plantilla `.env.example` al archivo `.env` en la raíz del proyecto:
```bash
cp .env.example .env
```
Verifica que las credenciales coincidan entre PostgreSQL y Spring Boot:
- `POSTGRES_DB=plataforma_renta`
- `POSTGRES_USER=postgres`
- `POSTGRES_PASSWORD=postgres`
- `POSTGRES_PORT=5439`
- `DB_URL=jdbc:postgresql://localhost:5439/plataforma_renta`

---

### Paso 2: Iniciar PostgreSQL con Docker Compose
Inicia el contenedor en segundo plano:
```bash
docker compose up -d
```

---

### Paso 3: Comprobar el Healthcheck
Verifica que el servicio esté saludable (`healthy`):
```bash
docker compose ps
```
Debe mostrar `obrixen_postgres` con estado `Up` y `(healthy)`.
También puedes verificar directamente:
```bash
docker compose exec postgres pg_isready -U postgres -d plataforma_renta
```

---

### Paso 4: Arrancar el Backend y Ejecutar Migraciones Flyway
Compila y ejecuta el backend:
```bash
# Compilar todos los módulos Maven
mvn clean package -DskipTests

# Arrancar Spring Boot (Flyway migrará automáticamente de V1 a V6)
export $(cat .env | grep -v '^#' | xargs)
java -jar platform/target/platform-0.1.0-SNAPSHOT.jar
```
Al iniciar, en los logs verás:
`Flyway Community Edition ... Current version of schema "public": 6`

---

### Paso 5: Arrancar el Frontend Web
En otra terminal:
```bash
cd frontend
npm install
npm run dev
```
La aplicación abrirá en `http://localhost:5173`.

---

### Paso 6: Cargar Datos de Demostración Oficiales
Para poblar la base de datos con el conjunto de demostración verificado (1 Admin, 1 Cliente, 3 Propietarios y 24 Herramientas):
```bash
docker compose exec -i postgres psql -U postgres -d plataforma_renta < database/seed_demo.sql
```

---

### Paso 7: Cuentas de Demostración para Evaluación y Sustentación

| Rol | Correo Electrónico | Contraseña Demostración | Organización / Perfil |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@obrixen.com.co` | `AdminDemo2026*!` | Administrador General de la Plataforma |
| **CLIENTE** | `cliente@obrixen.com.co` | `ClienteDemo2026*!` | Andrés Restrepo (Constructora Restrepo SAS) |
| **PROPIETARIO 1** | `propietario1@obrixen.com.co` | `Propietario1Demo2026*!` | Maquinaria y Equipos Andina S.A.S. (8 herramientas) |
| **PROPIETARIO 2** | `propietario2@obrixen.com.co` | `Propietario2Demo2026*!` | Soluciones de Energía y Andamios del Valle S.A.S. (8 herramientas) |
| **PROPIETARIO 3** | `propietario3@obrixen.com.co` | `Propietario3Demo2026*!` | Equipos Eléctricos y Menores de Antioquia Ltda. (8 herramientas) |

---

### Paso 8: Autenticación en Swagger UI
1. Abre en tu navegador `http://localhost:8080/swagger-ui/index.html`.
2. Verás el botón verde **Authorize** en la parte superior derecha.
3. Despliega el endpoint público `POST /api/v1/auth/login`.
4. Haz clic en **Try it out** e ingresa las credenciales de cualquiera de las cuentas de demostración, por ejemplo:
   ```json
   {
     "email": "admin@obrixen.com.co",
     "password": "AdminDemo2026*!"
   }
   ```
5. Ejecuta la petición y copia el campo `token` de la respuesta JSON (solo el string JWT).
6. Haz clic en el botón superior **Authorize**, pega el token en el campo `Value` y haz clic en **Authorize**. *(Swagger UI agregará automáticamente el prefijo `Bearer `)*.
7. Ahora puedes probar endpoints protegidos como `GET /api/v1/admin/usuarios` o `GET /api/v1/reportes/dashboard`.

---

## 3. Diagnóstico de Errores Comunes de Conexión

### Puerto 5439 en uso
- **Síntoma:** `Bind for 0.0.0.0:5439 failed: port is already allocated`.
- **Causa:** Hay otro contenedor o proceso usando el puerto 5439.
- **Solución:** Ejecuta `docker ps` para ver si existe un contenedor previo (como `plataforma_renta_isolated_pg`). Detén el contenedor con `docker stop <id>` o usa `docker compose stop`.

### Error de Autenticación en PostgreSQL (password authentication failed)
- **Síntoma:** `org.postgresql.util.PSQLException: FATAL: password authentication failed for user "postgres"`.
- **Causa:** La contraseña en `.env` (`DB_PASSWORD`) no coincide con la configurada al inicializar el volumen.
- **Solución:** Revisa `.env` y asegúrate de que `DB_PASSWORD=postgres` coincida con `POSTGRES_PASSWORD=postgres`.

### Fallo en Migraciones Flyway (Checksum mismatch)
- **Síntoma:** `FlywayException: Validate failed: Migrations have failed validation`.
- **Causa:** Se modificó un archivo histórico V1–V5.
- **Regla:** Los archivos V1–V5 **nunca** deben alterarse. La eliminación de columnas se realiza en `V6__remove_nit_from_propietario.sql`.

### Token JWT Expirado o Ausente
- **Síntoma:** Respuesta HTTP `401 Unauthorized`.
- **Causa:** La petición a un endpoint protegido no incluyó el encabezado `Authorization: Bearer <token>` o el token superó su tiempo de expiración (`JWT_EXPIRATION_MS`).
- **Solución:** Realiza login nuevamente para obtener un nuevo token válido.

---

## 4. Detener los Servicios sin Pérdida de Datos

Para detener los contenedores preservando la base de datos y todos sus datos:
```bash
docker compose down
```
> [!WARNING]
> **NUNCA** ejecutes `docker compose down -v` ni borres el volumen `obrixen_postgres_data`, ya que eliminarías la persistencia de datos.

---

## 5. Conexión desde Clientes SQL Externos (DBeaver, DataGrip, pgAdmin)

- **Host:** `localhost` o `127.0.0.1`
- **Puerto:** `5439` *(no 5432)*
- **Base de Datos:** `plataforma_renta`
- **Usuario:** `postgres`
- **Contraseña:** `postgres`
