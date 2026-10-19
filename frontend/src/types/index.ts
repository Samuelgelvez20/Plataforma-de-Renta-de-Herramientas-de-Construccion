export interface User {
  id: string | number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'PROPIETARIO' | 'CLIENTE';
  telefono: string;
  direccion: string;
  documentoIdentidad: string;
  activo: boolean;
  fechaRegistro: string;
}

export interface Category {
  id: string | number;
  nombre: string;
  descripcion: string;
  icono: string;
  activo: boolean;
}

export interface Tool {
  id: string | number;
  nombre: string;
  descripcion: string;
  categoriaId: string | number;
  propietarioId: string | number;
  precioPorDia: number;
  depositoGarantia: number;
  cantidadTotal?: number;
  cantidadDisponible?: number;
  estadoPublicacion: 'PUBLICADA' | 'BORRADOR' | 'ARCHIVADA';
  estadoOperativo: 'DISPONIBLE' | 'ALQUILADA' | 'EN_MANTENIMIENTO' | 'DANADA';
  imagenUrl: string;
  ubicacion: string;
  marca: string;
  modelo: string;
  especificacionesTecnicas: string;
  fechaCreacion: string;
  categoria?: Category;
  propietarioNombre?: string;
}

export interface Reservation {
  id: string | number;
  clienteId: string | number;
  herramientaId: string | number;
  fechaInicio: string;
  fechaFin: string;
  dias: number;
  precioDiaSnapshot: number;
  subtotal: number;
  iva: number;
  total: number;
  estado: 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'EXPIRADA' | 'RECHAZADA' | 'ACEPTADA' | 'PAGADA' | 'ENTREGADA' | 'DEVUELTA';
  notasCliente?: string;
  motivoRechazo?: string;
  fechaCreacion: string;
  herramientaNombre?: string;
  herramientaImagen?: string;
  clienteNombre?: string;
  pagada?: boolean;
  solicitudDevolucion?: boolean;
  estadoDevolucion?: string;
  notaDevolucionCliente?: string;
  costoReparacion?: number;
  descripcionDano?: string;
  observacionPropietario?: string;
  fechaDevolucion?: string;
}

export interface Payment {
  id: string | number;
  reservaId: string | number;
  monto: number;
  metodo: 'TARJETA_CREDITO' | 'PSE' | 'TRANSFERENCIA_BANCARIA';
  estado: 'PENDIENTE' | 'COMPLETADO' | 'FALLIDO' | 'REEMBOLSADO';
  referenciaTransaccion: string;
  claveIdempotencia: string;
  respuestaPasarela: string;
  fechaPago: string;
}

export interface InvoiceDetail {
  id: number;
  concepto: string;
  cantidadDias: number;
  precioUnitario: number;
  subtotalLinea: number;
}

export interface Invoice {
  id: string | number;
  numeroFactura: string;
  reservaId: string | number;
  pagoId?: string | number;
  clienteId?: string | number;
  propietarioId?: string | number;
  clienteNombre: string;
  clienteDocumento: string;
  clienteDireccion: string;
  propietarioNombre: string;
  subtotal: number;
  iva: number;
  total: number;
  fechaEmision: string;
  lineas: InvoiceDetail[];
}

export interface RefundRequest {
  id: string | number;
  pagoId: string | number;
  reservaId?: string | number;
  clienteId?: string | number;
  monto: number;
  motivo: string;
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
  resolucion?: string;
  resueltoPorUsuarioId?: string | number;
  fechaSolicitud: string;
  fechaResolucion?: string;
}

export interface DamageReport {
  id: string | number;
  reservaId: string | number;
  herramientaId: string | number;
  reportadoPorUsuarioId: string | number;
  descripcion: string;
  costoEstimadoReparacion: number;
  estado: 'REGISTRADO' | 'EN_REVISION' | 'RESUELTO';
  fechaReporte: string;
}

export interface DashboardMetrics {
  resumen: {
    totalUsuarios: number;
    totalClientes: number;
    totalPropietarios: number;
    totalHerramientas: number;
    totalReservas: number;
    reservasActivas: number;
    totalIngresos: number;
    totalFacturas: number;
    reembolsosPendientes: number;
  };
  disponibilidadInventario: {
    disponibles: number;
    alquiladas: number;
    enMantenimiento: number;
  };
  masAlquiladas: Array<{
    id: string | number;
    nombre: string;
    vecesAlquilada: number;
    precioPorDia: number;
  }>;
  clientesFrecuentes: Array<{
    nombre: string;
    reservas: number;
    gasto: number;
  }>;
  ingresosMensuales: Array<{
    mes: string;
    ingresos: number;
  }>;
}
