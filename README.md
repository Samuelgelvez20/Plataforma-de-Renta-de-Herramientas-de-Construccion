# Plataforma de Renta de Herramientas y Equipos de Construcción

> **Proyecto Académico de Ingeniería de Software**
> **Backend:** Java 21 + Spring Boot (Maven Multimódulo, Arquitectura Hexagonal / DDD)
> **Frontend:** React 19 + TypeScript + Vite + Tailwind CSS v4
> **Base de Datos:** PostgreSQL 16 con migraciones automatizadas Flyway
> **Documentación de API:** OpenAPI 3 / Swagger UI y Colección Postman v2.1

---

## 1. Descripción del Proyecto

La **Plataforma de Renta de Herramientas y Equipos de Construcción** es una solución web empresarial integral diseñada para la gestión, reserva, despacho, facturación y control de maquinaria y herramientas en el sector constructor.

El sistema resuelve la problemática de inventarios desarticulados, sobreventa (overbooking) y falta de trazabilidad en las operaciones de alquiler de maquinaria pesada, liviana y eléctrica, conectando a:
1. **Clientes (Constructoras / Contratistas):** Quienes exploran el catálogo unificado, verifican la disponibilidad de inventario en rangos temporales específicos, gestionan sus reservas y descargan sus facturas en PDF.
2. **Propietarios (Empresas de Maquinaria):** Quienes publican su parque de herramientas, aceptan o rechazan solicitudes, despachan los equipos y realizan la recepción técnica tras inspección de daños.
3. **Administradores:** Quienes supervisan la plataforma global, monitorean métricas de negocio e ingresos, administran cuentas de usuario y aprueban reembolsos.

---

## 2. Arquitectura de Software

La solución implementa una **Arquitectura Hexagonal (Puertos y Adaptadores)** combinada con principios de **Domain-Driven Design (DDD)**, estructurada en un proyecto Maven multimódulo.

### Diagrama de Arquitectura Hexagonal

```mermaid
graph TD
    subgraph Capa API y Presentacion
        REST[Controladores REST Spring Web]
        OpenAPI[Swagger / OpenAPI 3.0]
        ReactUI[Frontend React 19 + Tailwind v4]
        ReactUI -->|HTTP / JSON + JWT| REST
    end

    subgraph Modulo api
        REST
        DTOs[DTOs Request / Response / Validaciones]
        MappersApi[Mappers API - DTO]
        REST --> DTOs
    end

    subgraph Modulo aplicacion
        UseCases[Servicios de Aplicacion / Casos de Uso]
        AuthService[AuthService]
        HerramientaService[HerramientaService]
        ReservaService[ReservaService]
        PagoService[PagoService]
        FacturaService[FacturaService]
    end

    subgraph Modulo dominio
        Entities[Entidades y Agregados: Reserva, Pago, Factura, Herramienta]
        ValueObjects[Value Objects: Dinero, Email, Nit, Rangos]
        PortsIn[Puertos de Entrada]
        PortsOut[Puertos de Salida - Repositorios / PDF]
    end

    subgraph Modulo infraestructura
        JPARepos[Adaptadores de Persistencia Spring Data JPA]
        OpenPDFAdapter[Adaptador de Generacion PDF OpenPDF]
        FlywayMigrations[Migraciones Flyway SQL]
        PostgreSQL[(PostgreSQL 16 DB)]
        JPARepos --> PostgreSQL
        FlywayMigrations --> PostgreSQL
    end

    subgraph Modulo platform
        BootApp[PlataformaRentaApplication]
        SecConfig[Spring Security 6 + Filtro JWT]
        GlobalEx[GlobalExceptionHandler RFC 7807]
    end

    REST --> UseCases
    UseCases --> PortsOut
    UseCases --> Entities
    JPARepos -.->|Implementa| PortsOut
    OpenPDFAdapter -.->|Implementa| PortsOut
    SecConfig --> AuthService
```

### Módulos Maven y Responsabilidades

| Módulo | Tipo | Responsabilidad Arquitectónica | Dependencias Permitidas |
| :--- | :--- | :--- | :--- |
| **`domain`** | Java Puro | Núcleo del negocio: Entidades (`Reserva`, `Pago`, `Factura`, `Herramienta`), Enums (`ReservaEstado`, `PagoEstado`), Value Objects (`Dinero`, `Nit`, `Email`) y Puertos (`*Repository`, `FacturaPdfPort`). | Ninguna dependencia externa de frameworks. |
| **`application`** | Java / Spring Core | Casos de uso y orquestación del dominio (`ReservaService`, `PagoService`, `FacturaService`, etc.). | Depende únicamente de `domain`. |
| **`infrastructure`** | Spring Data JPA / OpenPDF | Adaptadores que implementan los puertos de salida: mapeo relacional a PostgreSQL, consultas SQL optimizadas y renderizado de facturas PDF. | Depende de `domain` y `application`. |
| **`api`** | Spring Web | Controladores REST, DTOs de entrada/salida y validación `@Valid` según estándares OpenAPI 3. | Depende de `application` y `domain`. |
| **`platform`** | Spring Boot Packaging | Punto de entrada `@SpringBootApplication`, configuración de Spring Security, filtro JWT, variables de entorno y pruebas de arquitectura ArchUnit. | Integra todos los módulos anteriores. |

---

## 3. Modelo de Datos PostgreSQL

El esquema relacional fue diseñado específicamente para PostgreSQL 16 con tipos enumerados nativos, restricciones de integridad referencial, disparadores de inmutabilidad financiera y cálculos de disponibilidad no bloqueantes.

### Diagrama Entidad-Relación (ER)

```mermaid
erDiagram
    USUARIO ||--o| CLIENTE : "perfil cliente"
    USUARIO ||--o| PROPIETARIO : "perfil propietario"
    CATEGORIA ||--o{ HERRAMIENTA : "clasifica"
    PROPIETARIO ||--o{ HERRAMIENTA : "posee"
    CLIENTE ||--o{ RESERVA : "solicita"
    RESERVA ||--|{ DETALLE_RESERVA : "contiene"
    HERRAMIENTA ||--o{ DETALLE_RESERVA : "reservada en"
    RESERVA ||--o{ PAGO : "asociada a"
    RESERVA ||--o{ FACTURA : "genera"
    PAGO ||--o| FACTURA : "soporta nota credito"
    DETALLE_RESERVA ||--o{ REPORTE_DANO : "registra inspeccion"

    USUARIO {
        uuid id PK
        varchar email UK
        varchar password_hash
        user_rol rol
        varchar nombre
        varchar apellido
        varchar telefono
        timestamptz creado_en
    }

    CLIENTE {
        uuid usuario_id PK,FK
        varchar direccion_entrega_default
    }

    PROPIETARIO {
        uuid usuario_id PK,FK
        varchar nit UK
        varchar razon_social
        varchar direccion_fiscal
        varchar regimen_tributario
    }

    CATEGORIA {
        uuid id PK
        varchar nombre UK
        text descripcion
        boolean activo
    }

    HERRAMIENTA {
        uuid id PK
        uuid categoria_id FK
        uuid propietario_id FK
        varchar nombre
        numeric precio_dia
        integer cantidad_total
        herramienta_pub_estado estado_publicacion
    }

    RESERVA {
        uuid id PK
        uuid cliente_id FK
        reserva_estado estado
        date fecha_inicio
        date fecha_fin
        timestamptz fecha_confirmacion
        timestamptz fecha_creacion
    }

    PAGO {
        uuid id PK
        uuid reserva_id FK
        uuid pago_original_id FK
        pago_estado estado
        varchar proveedor_pago
        varchar transaccion_id
        numeric monto
        char moneda
    }

    FACTURA {
        uuid id PK
        uuid reserva_id FK
        varchar numero_factura UK
        factura_tipo tipo
        uuid factura_original_id FK
        uuid pago_reembolso_id FK
        numeric subtotal
        numeric iva
        numeric total
        jsonb datos_emisor
        jsonb datos_receptor
    }
```

### Reglas de Integridad y Triggers Destacados

1. **Inmutabilidad de Pagos Completados (`trg_pago_completado_inmutable`):**
   Impide que registros en tabla `pago` con estado `COMPLETADO` sean alterados en su monto, moneda o reserva asociada.
2. **Consistencia de Notas de Crédito (`validar_nota_credito_original` y `validar_nota_credito_reembolso_factura`):**
   Verifica que una factura de tipo `NOTA_CREDITO` referencie una `FACTURA` previa válida de la misma reserva y un pago en estado `REEMBOLSADO`.
3. **Idempotencia de Pagos (`uq_pago_completado_por_reserva`):**
   Índice único condicional que garantiza a nivel de motor que jamás exista más de un cobro completado para una misma reserva.
4. **Validación de NIT Colombiano:**
   El Value Object `Nit` valida sintaxis y calcula el dígito de verificación mediante el algoritmo oficial de módulo 11 de la DIAN (`41, 37, 29, 23, 19, 17, 13, 7, 3`).

---

## 4. Flujos de Negocio Principales

### A. Autenticación y Autorización
- Registro y autenticación mediante JWT (`HMAC-SHA256`).
- Autorización basada en roles (`ROLE_CLIENTE`, `ROLE_PROPIETARIO`, `ROLE_ADMIN`) protegida a nivel de endpoints con Spring Security y anotaciones `@PreAuthorize`.

### B. Catálogo y Disponibilidad Temporal
- Consulta de herramientas por categoría.
- Verificación de disponibilidad en tiempo real:
  $$\text{Disponibles} = \text{Total} - \sum \text{Cantidad en reservas solapadas activo (PENDIENTE, CONFIRMADA, EN\_CURSO)}$$
- Prevención estricta de sobrecupo (HTTP 409 Conflict si la cantidad solicitada supera el stock para el rango de fechas).

### C. Ciclo de Vida de la Reserva
```text
[PENDIENTE] --(Propietario Acepta)--> [CONFIRMADA] --(Propietario Entrega)--> [EN_CURSO] --(Devolución con Inspección)--> [FINALIZADA]
     |                                      |
(Rechazo/Expiración)                   (Cancelación)
     v                                      v
 [RECHAZADA / EXPIRADA]                [CANCELADA]
```

### D. Pasarela Simulada, Facturación e Idempotencia
- **Simulación de Pago:** Procesa el abono mediante pasarela simulada con control de clave de idempotencia (`claveIdempotencia`). Peticiones repetidas retornan la transacción existente sin generar cobros duplicados.
- **Cálculo Tributario:** Aplica el 19% de IVA según normativa comercial colombiana.
- **Generación de Factura PDF:** Genera en memoria un documento binario PDF estándar con metadatos tributarios (emisor, receptor, desglose de ítems, subtotal, IVA y total) mediante el motor OpenPDF.

### E. Reembolsos y Notas de Crédito
- El cliente titular solicita un reembolso sobre una reserva pagada.
- El propietario o administrador evalúa y aprueba el reembolso.
- Al aprobarse, el pago transiciona a `REEMBOLSADO` y se emite automáticamente una **Nota de Crédito** formal referenciando la factura original.

---

## 5. Retirada Definitiva de Express y Unificación en Spring Boot

El proyecto sustituyó por completo el prototipo basado en Express.js (`server.ts`), consolidando la totalidad de la lógica en Spring Boot:

- **Eliminación:** Se retiró el archivo `server.ts` y las dependencias Node redundantes (`express`, `@types/express`, `tsx`, `jsonwebtoken`, `bcryptjs`, `jspdf`, `@google/genai`).
- **Frontend Independiente:** React 19 + TypeScript + Vite opera como cliente SPA estricto comunicándose exclusivamente mediante `fetch` con los endpoints REST del backend Spring Boot.
- **Comprobación:** Build de producción de Vite exitoso en `< 1s` y comprobación estricta de tipos `tsc --noEmit` con 0 errores.

---

## 6. Variables de Entorno y Configuración

El backend se configura mediante variables de entorno estándar. No se versionan credenciales reales en el repositorio.

| Variable | Descripción | Valor Local por Defecto |
| :--- | :--- | :--- |
| `SERVER_PORT` | Puerto de escucha del servidor HTTP | `8080` |
| `DB_URL` | URL JDBC de conexión a PostgreSQL | `jdbc:postgresql://localhost:5439/plataforma_renta` |
| `DB_USERNAME` | Usuario de base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de base de datos | `postgres` |
| `JWT_SECRET` | Clave secreta para firma HMAC de tokens (mínimo 32 bytes) | `clave-secreta-de-prueba-de-32-bytes-minimo-para-testing` |
| `VITE_API_URL` | URL base consumida por el frontend | `http://localhost:8080/api/v1` |

---

## 7. Instrucciones de Ejecución

### Prerrequisitos
- **Java:** OpenJDK 21 o superior.
- **Maven:** Apache Maven 3.9+.
- **Node.js:** Versión 20+ y `npm`.
- **PostgreSQL / Docker:** Contenedor PostgreSQL 16 activo.

### 1. Iniciar Base de Datos (Docker)
```bash
docker run -d --name plataforma_renta_pg -p 5439:5432 -e POSTGRES_DB=plataforma_renta -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16-alpine
```

### 2. Compilar y Ejecutar el Backend (Spring Boot)
```bash
# Compilar todo el proyecto Maven multimódulo
mvn clean package -DskipTests

# Ejecutar la aplicación Spring Boot
DB_URL="jdbc:postgresql://localhost:5439/plataforma_renta" DB_USERNAME="postgres" DB_PASSWORD="postgres" JWT_SECRET="clave-secreta-de-prueba-de-32-bytes-minimo-para-testing" SERVER_PORT="8080" java -jar platform/target/platform-0.1.0-SNAPSHOT.jar
```

Al iniciar, Flyway aplicará automáticamente las migraciones `V1` y `V2` inicializando el esquema y las categorías semilla.

### 3. Iniciar el Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
La interfaz estará disponible en `http://localhost:5173`.

---

## 8. Documentación de API y Colección Postman

### Swagger / OpenAPI 3.0
Con el backend en ejecución, acceda a la documentación interactiva y especificación OpenAPI en:
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

### Colección Postman
En la carpeta [`docs/postman_collection.json`](docs/postman_collection.json) se encuentra la colección oficial compatible con Postman v2.1. Incluye scripts automáticos para guardar tokens JWT en variables de colección y cubre:
1. Autenticación (Registro y Login de roles).
2. Catálogo de equipos y comprobación de disponibilidad.
3. Creación y flujo de estados de reservas.
4. Procesamiento de pagos con idempotencia y descarga binaria de facturas PDF.
5. Solicitud y resolución de reembolsos con notas de crédito.
6. Dashboard de métricas y administración de usuarios.

---

## 9. Pruebas Automatizadas

El proyecto cuenta con un conjunto integral de pruebas automatizadas:

```bash
# Ejecutar todas las pruebas del backend (unitarias, integración y arquitectura)
mvn test
```

- **Pruebas Unitarias de Dominio:** Validaciones de NIT, operaciones financieras con `Dinero` y transiciones de estados de `Reserva`.
- **Pruebas de Casos de Uso (Mockito):** Verificación de idempotencia, orquestación de pagos, facturas y notas de crédito.
- **Pruebas de Integración con Testcontainers:** Verificación de concurrencia y transacciones sobre PostgreSQL real en contenedor Docker.
- **Pruebas de Arquitectura (ArchUnit):** Reglas hexagonales que prohíben que el dominio importe infraestructura o frameworks.
- **Frontend Check:**
  ```bash
  cd frontend
  npm run lint   # Verificación estricta de tipos TypeScript
  npm run build  # Compilación de bundles de producción Vite
  ```

---

## 10. Aviso Legal y Alcance Normativo

> **IMPORTANTE — ALCANCE DE LA FACTURACIÓN DEL PROYECTO:**
> Los documentos PDF de facturas y notas de crédito emitidos por esta plataforma constituyen **comprobantes comerciales y tributarios simulados con propósitos académicos y de demostración funcional**.
> Esta implementación **no constituye por sí misma una Factura Electrónica de Venta validada previamente por la DIAN** en los términos del Estatuto Tributario colombiano ni del Decreto 358 de 2020. No incluye firma digital con certificado X.509 de un Proveedor Tecnológico Autorizado, generación de CUFE (Código Único de Factura Electrónica), código QR DIAN ni transmisión sincrónica mediante Web Services SOAP/XML a la autoridad tributaria.
