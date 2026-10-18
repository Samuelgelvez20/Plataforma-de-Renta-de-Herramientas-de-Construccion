import React, { useState } from 'react';
import { X, Layers, Code, CheckCircle, Database, Shield, FileText } from 'lucide-react';

interface SwaggerViewerModalProps {
  onClose: () => void;
}

export const SwaggerViewerModal: React.FC<SwaggerViewerModalProps> = ({ onClose }) => {
  const [activeTab, setActiveTab] = useState<'swagger' | 'architecture' | 'database'>('swagger');

  const endpoints = [
    { method: 'POST', path: '/api/v1/auth/register', auth: 'Público', desc: 'Registro de clientes y propietarios (ADMIN prohibido públicamente)' },
    { method: 'POST', path: '/api/v1/auth/login', auth: 'Público', desc: 'Autenticación con email/contraseña; devuelve token JWT con rol' },
    { method: 'GET', path: '/api/v1/auth/me', auth: 'Bearer JWT', desc: 'Consulta el perfil y rol del usuario autenticado' },
    { method: 'GET', path: '/api/v1/herramientas', auth: 'Público', desc: 'Catálogo de maquinaria con filtros por categoría, búsqueda, precio y disponibilidad' },
    { method: 'GET', path: '/api/v1/herramientas/{id}', auth: 'Público', desc: 'Detalle de equipo, especificaciones técnicas y tarifas' },
    { method: 'GET', path: '/api/v1/herramientas/{id}/disponibilidad', auth: 'Público', desc: 'Verifica solapamiento en intervalo semiabierto [inicio, fin)' },
    { method: 'POST', path: '/api/v1/herramientas', auth: 'PROPIETARIO/ADMIN', desc: 'Crea nuevo equipo en el inventario propio' },
    { method: 'PUT', path: '/api/v1/herramientas/{id}', auth: 'PROPIETARIO/ADMIN', desc: 'Actualiza datos, tarifas y estado operativo del equipo' },
    { method: 'DELETE', path: '/api/v1/herramientas/{id}', auth: 'PROPIETARIO/ADMIN', desc: 'Baja física o lógica si presenta reservas históricas' },
    { method: 'GET', path: '/api/v1/reservas', auth: 'Bearer JWT', desc: 'Lista reservas filtradas según el rol del usuario en sesión' },
    { method: 'POST', path: '/api/v1/reservas', auth: 'CLIENTE', desc: 'Crea reserva calculando subtotal, 19% IVA y bloqueando solapamientos' },
    { method: 'PUT', path: '/api/v1/reservas/{id}/aceptar', auth: 'PROPIETARIO', desc: 'Dueño acepta la solicitud de renta' },
    { method: 'PUT', path: '/api/v1/reservas/{id}/rechazar', auth: 'PROPIETARIO', desc: 'Dueño rechaza la solicitud indicando motivo' },
    { method: 'PUT', path: '/api/v1/reservas/{id}/entregar', auth: 'PROPIETARIO', desc: 'Despacho de maquinaria: reserva ENTREGADA, equipo ALQUILADA' },
    { method: 'PUT', path: '/api/v1/reservas/{id}/devolver', auth: 'PROPIETARIO', desc: 'Retorno e inspección técnica de daños; equipo DISPONIBLE o EN_MANTENIMIENTO' },
    { method: 'POST', path: '/api/v1/pagos/procesar', auth: 'CLIENTE', desc: 'Pasarela simulada con idempotencia; emite Factura inmutable' },
    { method: 'GET', path: '/api/v1/facturas', auth: 'Bearer JWT', desc: 'Listado de facturas vinculadas a transacciones' },
    { method: 'GET', path: '/api/v1/facturas/{id}/pdf', auth: 'Bearer JWT', desc: 'Descarga de documento fiscal formal en formato PDF' },
    { method: 'POST', path: '/api/v1/reembolsos', auth: 'CLIENTE', desc: 'Cliente radica solicitud de reembolso de un pago completado' },
    { method: 'PUT', path: '/api/v1/reembolsos/{id}/resolver', auth: 'PROPIETARIO/ADMIN', desc: 'Aprobación o rechazo con validación de rol' },
    { method: 'GET', path: '/api/v1/reportes/dashboard', auth: 'Bearer JWT', desc: 'Analítica agregada: ingresos mensuales, equipos más rentados y disponibilidad' }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-4xl w-full overflow-hidden shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-200">
          <div className="flex items-center gap-2">
            <Code className="w-5 h-5 text-amber-600" />
            <div>
              <h2 className="text-base font-bold text-neutral-900 leading-tight">
                Documentación Técnica & Especificación OpenAPI / Hexagonal
              </h2>
              <span className="text-xs text-neutral-500 font-mono">
                Spring Boot 3.2.4 · Java 21 · OpenAPI 3.0 · PostgreSQL 16
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-neutral-200 px-6 bg-neutral-50 gap-2 pt-2">
          <button
            onClick={() => setActiveTab('swagger')}
            className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
              activeTab === 'swagger'
                ? 'border-amber-500 text-neutral-900 bg-white rounded-t-lg'
                : 'border-transparent text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Contratos REST API (/api/v1/*)
          </button>
          <button
            onClick={() => setActiveTab('architecture')}
            className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
              activeTab === 'architecture'
                ? 'border-amber-500 text-neutral-900 bg-white rounded-t-lg'
                : 'border-transparent text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Arquitectura Hexagonal DDD
          </button>
          <button
            onClick={() => setActiveTab('database')}
            className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
              activeTab === 'database'
                ? 'border-amber-500 text-neutral-900 bg-white rounded-t-lg'
                : 'border-transparent text-neutral-600 hover:text-neutral-900'
            }`}
          >
            Modelo Relacional PostgreSQL
          </button>
        </div>

        {/* Content Area */}
        <div className="p-6 max-h-[70vh] overflow-y-auto">
          {activeTab === 'swagger' && (
            <div className="space-y-3">
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs text-neutral-600 mb-4 flex items-center justify-between">
                <span>Todos los endpoints responden con formato RFC 7807 uniforme ante excepciones y soportan autenticación Bearer JWT.</span>
                <span className="font-mono font-bold text-amber-700">{endpoints.length} Endpoints Registrados</span>
              </div>

              <div className="space-y-2">
                {endpoints.map((ep, i) => (
                  <div key={i} className="p-3 rounded-lg border border-neutral-200 bg-white hover:bg-neutral-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-3">
                      <span className={`px-2 py-0.5 font-mono font-bold text-[10px] rounded ${
                        ep.method === 'GET' ? 'bg-blue-100 text-blue-800' :
                        ep.method === 'POST' ? 'bg-emerald-100 text-emerald-800' :
                        ep.method === 'PUT' ? 'bg-amber-100 text-amber-800' :
                        'bg-rose-100 text-rose-800'
                      }`}>
                        {ep.method}
                      </span>
                      <span className="font-mono font-bold text-neutral-900">{ep.path}</span>
                    </div>

                    <div className="flex items-center gap-4 text-neutral-500">
                      <span className="text-[11px] italic">{ep.desc}</span>
                      <span className="px-2 py-0.5 rounded bg-neutral-100 text-neutral-700 font-mono text-[10px] shrink-0">
                        {ep.auth}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'architecture' && (
            <div className="space-y-6 text-xs text-neutral-700">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50">
                  <h4 className="font-bold text-sm text-neutral-900 mb-2 flex items-center gap-1.5">
                    <Layers className="w-4 h-4 text-amber-600" />
                    <span>Módulo 1: Domain (Puro)</span>
                  </h4>
                  <p className="mb-2">Agnóstico a Spring Boot y JPA. Contiene:</p>
                  <ul className="list-disc list-inside space-y-1 font-mono text-[11px]">
                    <li>Value Objects: Dinero (COP, IVA 19%), IntervaloFechas [inicio, fin)</li>
                    <li>Entidades: Usuario, Herramienta, Reserva, Pago, Factura, ReporteDano</li>
                    <li>Puertos de Salida: ReservaRepositoryPort, PasarelaPagoPort, GeneradorFacturaPdfPort</li>
                    <li>Excepciones de dominio y reglas de invariantes</li>
                  </ul>
                </div>

                <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50">
                  <h4 className="font-bold text-sm text-neutral-900 mb-2 flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-emerald-600" />
                    <span>Módulo 2: Application (Casos de Uso)</span>
                  </h4>
                  <p className="mb-2">Orquestación de operaciones y reglas de negocio:</p>
                  <ul className="list-disc list-inside space-y-1 font-mono text-[11px]">
                    <li>CrearReservaUseCase (comprobación estricta de solapamiento)</li>
                    <li>ProcesarPagoUseCase (control de idempotencia, gateway y facturación)</li>
                    <li>RegistrarEntregaDevolucionUseCase (inspección técnica de daños)</li>
                    <li>GestionReembolsoUseCase (permisos cliente vs propietario/admin)</li>
                  </ul>
                </div>

                <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50">
                  <h4 className="font-bold text-sm text-neutral-900 mb-2 flex items-center gap-1.5">
                    <Database className="w-4 h-4 text-blue-600" />
                    <span>Módulo 3: Infrastructure</span>
                  </h4>
                  <p className="mb-2">Adaptadores técnicos y persistencia:</p>
                  <ul className="list-disc list-inside space-y-1 font-mono text-[11px]">
                    <li>Spring Data JPA Repositories con bloqueos pesimistas</li>
                    <li>Migraciones Flyway: V1__schema.sql y V2__seed.sql</li>
                    <li>Generador de Facturas PDF (OpenPDF / jsPDF)</li>
                    <li>Pruebas ArchUnit para verificar aislamiento de capas</li>
                  </ul>
                </div>

                <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50">
                  <h4 className="font-bold text-sm text-neutral-900 mb-2 flex items-center gap-1.5">
                    <Shield className="w-4 h-4 text-purple-600" />
                    <span>Módulo 4: API & Platform</span>
                  </h4>
                  <p className="mb-2">Capa de presentación HTTP y arranque:</p>
                  <ul className="list-disc list-inside space-y-1 font-mono text-[11px]">
                    <li>Controladores REST delgados bajo prefijo /api/v1</li>
                    <li>Filtro JwtAuthorizationFilter con BCrypt</li>
                    <li>Manejador global @RestControllerAdvice (RFC 7807)</li>
                    <li>PlataformaRentaApplication y perfiles application.properties</li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'database' && (
            <div className="space-y-4 text-xs font-mono">
              <div className="p-3 bg-neutral-900 text-neutral-200 rounded-xl overflow-x-auto text-[11px]">
                <pre>{`-- Esquema Relacional de Tablas Principales (PostgreSQL 16)
usuarios (id PK, email UK, password_hash, rol CHECK, doc_identidad UK, activo)
categorias (id PK, nombre UK, activo)
herramientas (id PK, nombre, categoria_id FK, propietario_id FK, precio_por_dia > 0, estado_op, estado_pub)
reservas (id PK, cliente_id FK, herramienta_id FK, fecha_inicio, fecha_fin, subtotal, iva 19%, total, estado)
pagos (id PK, reserva_id FK, monto, metodo, estado, ref_transaccion UK, clave_idempotencia UK)
facturas (id PK, numero_factura UK, reserva_id FK, pago_id FK, cliente_id, subtotal, iva, total)
detalles_factura (id PK, factura_id FK, concepto, cantidad_dias, precio_unitario, subtotal_linea)
solicitudes_reembolso (id PK, pago_id FK, reserva_id FK, cliente_id, monto, estado, resolucion)
reportes_dano (id PK, reserva_id FK, herramienta_id FK, descripcion, costo_estimado_reparacion, estado)`}</pre>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 bg-neutral-50 border-t border-neutral-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-neutral-700 bg-white border border-neutral-300 rounded-lg hover:bg-neutral-100"
          >
            Cerrar Especificación
          </button>
        </div>
      </div>
    </div>
  );
};
