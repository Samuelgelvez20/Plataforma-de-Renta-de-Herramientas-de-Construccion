import {
  User,
  Category,
  Tool,
  Reservation,
  Payment,
  Invoice,
  RefundRequest,
  DashboardMetrics
} from '../types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('jwt_token');
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

async function handleResponse<T>(res: Response, defaultErrorMsg: string): Promise<T> {
  if (!res.ok) {
    let errorDetail = '';
    try {
      const errJson = await res.json();
      if (errJson.errores && typeof errJson.errores === 'object') {
        errorDetail = Object.entries(errJson.errores)
          .map(([k, v]) => `${k}: ${v}`)
          .join(', ');
      } else if (errJson.mensaje) {
        errorDetail = errJson.mensaje;
      } else if (errJson.error) {
        errorDetail = errJson.error;
      }
    } catch (_) {
      // Si la respuesta no es JSON
      errorDetail = await res.text().catch(() => '');
    }

    if (res.status === 401) {
      throw new Error(errorDetail || 'Sesión no autorizada o expirada');
    }
    if (res.status === 403) {
      throw new Error(errorDetail || 'Acceso denegado: permisos insuficientes para esta operación');
    }
    throw new Error(errorDetail || defaultErrorMsg);
  }

  // Si no hay contenido (ej: 204 No Content)
  if (res.status === 204) {
    return {} as T;
  }

  return res.json();
}

function normalizeTool(t: any): Tool {
  return {
    id: t.id,
    nombre: t.nombre || '',
    descripcion: t.descripcion || '',
    categoriaId: t.categoriaId,
    propietarioId: t.propietarioId,
    precioPorDia: t.precioPorDia ?? t.precioDia ?? 0,
    depositoGarantia: t.depositoGarantia ?? 0,
    cantidadTotal: t.cantidadTotal ?? 1,
    cantidadDisponible: t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal ?? 1),
    estadoPublicacion: t.estadoPublicacion || 'PUBLICADA',
    estadoOperativo: t.estadoOperativo || 'DISPONIBLE',
    imagenUrl: t.imagenUrl || '',
    ubicacion: t.ubicacion || 'Bogotá, D.C.',
    marca: t.marca || '',
    modelo: t.modelo || '',
    especificacionesTecnicas: t.especificacionesTecnicas || '',
    fechaCreacion: t.fechaCreacion || t.creadoEn || new Date().toISOString(),
    categoria: t.categoria,
    propietarioNombre: t.propietarioNombre
  };
}

function normalizeReservation(r: any): Reservation {
  const primerDetalle = r.detalles && r.detalles.length > 0 ? r.detalles[0] : null;
  const dias = primerDetalle?.dias || 1;
  const subtotal = typeof r.subtotal === 'number' ? r.subtotal : parseFloat(r.subtotal || '0');
  const iva = typeof r.iva === 'number' ? r.iva : parseFloat(r.iva || '0');
  const total = typeof r.total === 'number' ? r.total : parseFloat(r.total || '0');

  return {
    id: r.id,
    clienteId: r.clienteId,
    herramientaId: primerDetalle?.herramientaId || r.herramientaId,
    fechaInicio: r.fechaInicio,
    fechaFin: r.fechaFin,
    dias,
    precioDiaSnapshot: primerDetalle?.precioDia || 0,
    subtotal,
    iva,
    total,
    estado: r.estado || 'PENDIENTE',
    notasCliente: r.notasCliente,
    motivoRechazo: r.motivoRechazo,
    fechaCreacion: r.creadoEn || r.fechaCreacion || new Date().toISOString(),
    herramientaNombre: primerDetalle?.herramientaNombre || 'Equipo de Construcción',
    clienteNombre: r.clienteNombre,
    pagada: Boolean(r.pagada),
    solicitudDevolucion: Boolean(r.solicitudDevolucion),
    estadoDevolucion: r.estadoDevolucion,
    notaDevolucionCliente: r.notaDevolucionCliente,
    costoReparacion: typeof r.costoReparacion === 'number' ? r.costoReparacion : (r.costoReparacion ? parseFloat(r.costoReparacion) : 0),
    descripcionDano: r.descripcionDano,
    observacionPropietario: r.observacionPropietario
  };
}

export const api = {
  // Autenticación
  async login(email: string, password: string): Promise<{ token: string; usuario: User }> {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    return handleResponse<{ token: string; usuario: User }>(res, 'Error al iniciar sesión');
  },

  async register(payload: any): Promise<{ token: string; usuario: User }> {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return handleResponse<{ token: string; usuario: User }>(res, 'Error al registrar usuario');
  },

  async getMe(): Promise<User> {
    const res = await fetch(`${API_BASE}/auth/me`, {
      headers: { ...getAuthHeader() }
    });
    return handleResponse<User>(res, 'Error al consultar perfil de usuario');
  },

  // Categorías
  async getCategories(): Promise<Category[]> {
    const res = await fetch(`${API_BASE}/categorias`);
    const list = await handleResponse<any[]>(res, 'Error al listar categorías');
    return list.map(c => ({
      id: c.id,
      nombre: c.nombre,
      descripcion: c.descripcion || '',
      icono: c.icono || 'Layers',
      activo: c.activo ?? true
    }));
  },

  // Herramientas
  async getTools(params?: { categoriaId?: string | number; propietarioId?: string | number; search?: string; disponible?: boolean; precioMax?: number }): Promise<Tool[]> {
    const url = new URL(`${API_BASE}/herramientas`);
    if (params?.categoriaId) url.searchParams.append('categoriaId', params.categoriaId.toString());
    if (params?.propietarioId) url.searchParams.append('propietarioId', params.propietarioId.toString());
    if (params?.search) url.searchParams.append('query', params.search);
    if (params?.disponible !== undefined) url.searchParams.append('disponible', params.disponible.toString());
    if (params?.precioMax) url.searchParams.append('maxPrice', params.precioMax.toString());

    const res = await fetch(url.toString());
    const data = await handleResponse<any[]>(res, 'Error al consultar catálogo de herramientas');
    return data.map(normalizeTool);
  },

  async getToolById(id: string | number): Promise<Tool> {
    const res = await fetch(`${API_BASE}/herramientas/${id}`);
    const data = await handleResponse<any>(res, 'Herramienta no encontrada');
    return normalizeTool(data);
  },

  async checkAvailability(toolId: string | number, fechaInicio: string, fechaFin: string): Promise<{ disponible: boolean; estadoOperativo: string; conflictos: any[] }> {
    const url = `${API_BASE}/herramientas/${toolId}/disponibilidad?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
    const res = await fetch(url);
    const data = await handleResponse<any>(res, 'Error al consultar disponibilidad');
    return {
      disponible: data.disponible ?? true,
      estadoOperativo: data.disponible ? 'DISPONIBLE' : 'ALQUILADA',
      conflictos: []
    };
  },

  async createTool(payload: Partial<Tool> & { cantidadTotal?: number }): Promise<Tool> {
    const body = {
      categoriaId: payload.categoriaId,
      nombre: payload.nombre,
      descripcion: payload.descripcion || '',
      marca: payload.marca || 'Genérica',
      modelo: payload.modelo || 'Estándar',
      precioDia: payload.precioPorDia,
      cantidadTotal: payload.cantidadTotal && payload.cantidadTotal > 0 ? payload.cantidadTotal : 1,
      estadoPublicacion: payload.estadoPublicacion || 'PUBLICADA',
      ivaPorcentaje: 19.00,
      ivaAplica: true
    };
    const res = await fetch(`${API_BASE}/herramientas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(body)
    });
    const data = await handleResponse<any>(res, 'Error al crear herramienta');
    return normalizeTool(data);
  },

  async updateTool(id: string | number, payload: Partial<Tool>): Promise<Tool> {
    const body: Record<string, any> = {};
    if (payload.precioPorDia !== undefined) body.precioDia = payload.precioPorDia;
    if (payload.estadoPublicacion !== undefined) body.estadoPublicacion = payload.estadoPublicacion;
    const res = await fetch(`${API_BASE}/herramientas/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(body)
    });
    const data = await handleResponse<any>(res, 'Error al actualizar equipo');
    return normalizeTool(data);
  },

  async deleteTool(id: string | number): Promise<void> {
    const res = await fetch(`${API_BASE}/herramientas/${id}`, {
      method: 'DELETE',
      headers: { ...getAuthHeader() }
    });
    await handleResponse<void>(res, 'Error al eliminar herramienta');
  },

  // Reservas
  async getReservations(): Promise<Reservation[]> {
    const res = await fetch(`${API_BASE}/reservas`, {
      headers: { ...getAuthHeader() }
    });
    const data = await handleResponse<any[]>(res, 'Error al listar reservas');
    return data.map(normalizeReservation);
  },

  async createReservation(payload: { herramientaId: string | number; cantidad?: number; fechaInicio: string; fechaFin: string; notasCliente?: string }): Promise<Reservation> {
    const body = {
      herramientaId: payload.herramientaId,
      cantidad: payload.cantidad && payload.cantidad > 0 ? payload.cantidad : 1,
      fechaInicio: payload.fechaInicio,
      fechaFin: payload.fechaFin,
      notasCliente: payload.notasCliente || ''
    };
    const res = await fetch(`${API_BASE}/reservas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(body)
    });
    const data = await handleResponse<any>(res, 'Error al crear reserva');
    return normalizeReservation(data);
  },

  async acceptReservation(id: string | number): Promise<void> {
    const res = await fetch(`${API_BASE}/reservas/${id}/aceptar`, {
      method: 'PUT',
      headers: { ...getAuthHeader() }
    });
    await handleResponse<any>(res, 'Error al aceptar reserva');
  },

  async rejectReservation(id: string | number, motivo?: string): Promise<void> {
    const res = await fetch(`${API_BASE}/reservas/${id}/rechazar`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ motivo: motivo || 'Rechazada por propietario' })
    });
    await handleResponse<any>(res, 'Error al rechazar reserva');
  },

  async cancelReservation(id: string | number, motivo?: string): Promise<Reservation> {
    const res = await fetch(`${API_BASE}/reservas/${id}/cancelar`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ motivo: motivo || 'Cancelada por el cliente titular' })
    });
    const data = await handleResponse<any>(res, 'Error al cancelar la reserva');
    return normalizeReservation(data);
  },

  async dispatchReservation(id: string | number): Promise<void> {
    const res = await fetch(`${API_BASE}/reservas/${id}/entregar`, {
      method: 'PUT',
      headers: { ...getAuthHeader() }
    });
    await handleResponse<any>(res, 'Error al entregar equipo');
  },

  async requestReturn(id: string | number, payload: { notaCliente?: string }): Promise<void> {
    const res = await fetch(`${API_BASE}/reservas/${id}/solicitar-devolucion`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({
        notaCliente: payload.notaCliente || ''
      })
    });
    await handleResponse<any>(res, 'Error al solicitar devolución');
  },

  async returnReservation(id: string | number, payload: { aceptada?: boolean; presentaDanos: boolean; descripcionDano?: string; costoReparacion?: number; observaciones?: string }): Promise<void> {
    const res = await fetch(`${API_BASE}/reservas/${id}/devolver`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({
        aceptada: payload.aceptada ?? true,
        presentaDanos: payload.presentaDanos,
        descripcionDano: payload.descripcionDano || '',
        costoReparacion: payload.costoReparacion || 0,
        observaciones: payload.observaciones || ''
      })
    });
    await handleResponse<any>(res, 'Error al confirmar devolución');
  },

  // Pagos
  async processPayment(payload: { reservaId: string | number; metodo: string; claveIdempotencia?: string }): Promise<{ pago: Payment; factura: Invoice; mensaje: string }> {
    const claveIdempotencia = payload.claveIdempotencia || `idemp-${payload.reservaId}-${Date.now()}`;
    const res = await fetch(`${API_BASE}/pagos/procesar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({
        reservaId: payload.reservaId,
        metodo: payload.metodo,
        claveIdempotencia
      })
    });
    const data = await handleResponse<any>(res, 'Error al procesar pago');
    return {
      pago: {
        id: data.pago.id,
        reservaId: data.pago.reservaId,
        monto: typeof data.pago.monto === 'number' ? data.pago.monto : parseFloat(data.pago.monto || '0'),
        metodo: payload.metodo as any,
        estado: data.pago.estado || 'COMPLETADO',
        referenciaTransaccion: data.pago.transaccionId || '',
        claveIdempotencia,
        respuestaPasarela: 'APROBADA',
        fechaPago: data.pago.fechaPago || new Date().toISOString()
      },
      factura: {
        id: data.factura.id,
        numeroFactura: data.factura.numeroFactura,
        reservaId: data.factura.reservaId,
        pagoId: data.pago.id,
        clienteId: 0,
        propietarioId: 0,
        clienteNombre: 'Cliente Registrado',
        clienteDocumento: 'CC',
        clienteDireccion: 'Colombia',
        propietarioNombre: 'Propietario de Equipos',
        subtotal: typeof data.factura.subtotal === 'number' ? data.factura.subtotal : parseFloat(data.factura.subtotal || '0'),
        iva: typeof data.factura.iva === 'number' ? data.factura.iva : parseFloat(data.factura.iva || '0'),
        total: typeof data.factura.total === 'number' ? data.factura.total : parseFloat(data.factura.total || '0'),
        fechaEmision: data.factura.fechaEmision || new Date().toISOString(),
        lineas: []
      },
      mensaje: data.mensaje || 'Pago procesado exitosamente'
    };
  },

  // Facturas
  async getInvoices(): Promise<Invoice[]> {
    const res = await fetch(`${API_BASE}/facturas`, {
      headers: { ...getAuthHeader() }
    });
    const list = await handleResponse<any[]>(res, 'Error al obtener facturas');
    return list.map(f => ({
      id: f.id,
      numeroFactura: f.numeroFactura,
      reservaId: f.reservaId,
      pagoId: f.id,
      clienteId: 0,
      propietarioId: 0,
      clienteNombre: 'Cliente',
      clienteDocumento: 'CC',
      clienteDireccion: '',
      propietarioNombre: 'Propietario',
      subtotal: typeof f.subtotal === 'number' ? f.subtotal : parseFloat(f.subtotal || '0'),
      iva: typeof f.iva === 'number' ? f.iva : parseFloat(f.iva || '0'),
      total: typeof f.total === 'number' ? f.total : parseFloat(f.total || '0'),
      fechaEmision: f.fechaEmision || new Date().toISOString(),
      lineas: []
    }));
  },

  async downloadInvoicePdf(id: string | number, numeroFactura: string): Promise<void> {
    const res = await fetch(`${API_BASE}/facturas/${id}/pdf`, {
      headers: { ...getAuthHeader() }
    });
    if (!res.ok) {
      throw new Error('Error al descargar el PDF de la factura desde el backend');
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${numeroFactura}.pdf`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  // Reembolsos
  async getRefunds(): Promise<RefundRequest[]> {
    const res = await fetch(`${API_BASE}/reembolsos`, {
      headers: { ...getAuthHeader() }
    });
    const list = await handleResponse<any[]>(res, 'Error al obtener reembolsos');
    return list.map(r => ({
      id: r.id,
      pagoId: r.pagoOriginalId,
      reservaId: r.reservaId,
      clienteId: 0,
      monto: typeof r.monto === 'number' ? r.monto : parseFloat(r.monto || '0'),
      motivo: r.motivo || '',
      estado: r.estado || 'PENDIENTE',
      fechaSolicitud: r.creadoEn || new Date().toISOString()
    }));
  },

  async requestRefund(pagoId: string | number, motivo: string, reservaId?: string | number, montoConIva?: number): Promise<RefundRequest> {
    const res = await fetch(`${API_BASE}/reembolsos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({
        pagoId,
        motivo,
        reservaId,
        montoConIva
      })
    });
    const r = await handleResponse<any>(res, 'Error al solicitar reembolso');
    return {
      id: r.id,
      pagoId: r.pagoOriginalId,
      reservaId: r.reservaId,
      clienteId: 0,
      monto: typeof r.monto === 'number' ? r.monto : parseFloat(r.monto || '0'),
      motivo: r.motivo || '',
      estado: r.estado || 'PENDIENTE',
      fechaSolicitud: r.creadoEn || new Date().toISOString()
    };
  },

  async resolveRefund(id: string | number, aprobar: boolean, resolucion: string): Promise<void> {
    const res = await fetch(`${API_BASE}/reembolsos/${id}/resolver`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ aprobar, resolucion })
    });
    await handleResponse<any>(res, 'Error al resolver reembolso');
  },

  // Reportes
  async getDashboardMetrics(): Promise<DashboardMetrics> {
    const res = await fetch(`${API_BASE}/reportes/dashboard`, {
      headers: { ...getAuthHeader() }
    });
    const data = await handleResponse<any>(res, 'Error al cargar métricas del panel');

    return {
      resumen: data.resumen ? {
        totalUsuarios: data.resumen.totalUsuarios ?? 0,
        totalClientes: data.resumen.totalClientes ?? 0,
        totalPropietarios: data.resumen.totalPropietarios ?? 0,
        totalHerramientas: data.resumen.totalHerramientas ?? (data.totalHerramientas || 0),
        totalReservas: data.resumen.totalReservas ?? 0,
        reservasActivas: data.resumen.reservasActivas ?? (data.reservasActivas || 0),
        totalIngresos: typeof data.resumen.totalIngresos === 'number' ? data.resumen.totalIngresos : parseFloat(data.resumen.totalIngresos || data.ingresosTotales || '0'),
        totalFacturas: data.resumen.totalFacturas ?? 0,
        reembolsosPendientes: data.resumen.reembolsosPendientes ?? (data.reembolsosPendientes || 0)
      } : {
        totalUsuarios: 0,
        totalClientes: 0,
        totalPropietarios: 0,
        totalHerramientas: data.totalHerramientas || 0,
        totalReservas: data.reservasActivas || 0,
        reservasActivas: data.reservasActivas || 0,
        totalIngresos: typeof data.ingresosTotales === 'number' ? data.ingresosTotales : parseFloat(data.ingresosTotales || '0'),
        totalFacturas: 0,
        reembolsosPendientes: data.reembolsosPendientes || 0
      },
      disponibilidadInventario: data.disponibilidadInventario ? {
        disponibles: data.disponibilidadInventario.disponibles ?? (data.herramientasDisponibles || 0),
        alquiladas: data.disponibilidadInventario.alquiladas ?? Math.max(0, (data.totalHerramientas || 0) - (data.herramientasDisponibles || 0)),
        enMantenimiento: data.disponibilidadInventario.enMantenimiento ?? 0
      } : {
        disponibles: data.herramientasDisponibles || 0,
        alquiladas: Math.max(0, (data.totalHerramientas || 0) - (data.herramientasDisponibles || 0)),
        enMantenimiento: 0
      },
      masAlquiladas: (data.masAlquiladas || []).map((m: any) => ({
        id: m.id,
        nombre: m.nombre,
        vecesAlquilada: m.vecesAlquilada || 0,
        precioPorDia: typeof m.precioPorDia === 'number' ? m.precioPorDia : parseFloat(m.precioPorDia || '0')
      })),
      clientesFrecuentes: (data.clientesFrecuentes || []).map((c: any) => ({
        nombre: c.nombre,
        reservas: c.reservas || 0,
        gasto: typeof c.gasto === 'number' ? c.gasto : parseFloat(c.gasto || '0')
      })),
      ingresosMensuales: (data.ingresosMensuales || []).map((i: any) => ({
        mes: i.mes,
        ingresos: typeof i.ingresos === 'number' ? i.ingresos : parseFloat(i.ingresos || '0')
      }))
    };
  },

  // Admin Usuarios CRUD
  async getAdminUsers(): Promise<User[]> {
    const res = await fetch(`${API_BASE}/admin/usuarios`, {
      headers: { ...getAuthHeader() }
    });
    const data = await handleResponse<any[]>(res, 'Error al listar usuarios');
    return data.map(u => ({
      id: u.id,
      nombre: `${u.nombre || ''} ${u.apellido || ''}`.trim(),
      email: u.email,
      rol: u.rol,
      telefono: u.telefono || '',
      direccion: u.direccion || '',
      documentoIdentidad: u.nit || ('C.C. ' + String(u.id).slice(0, 8)),
      activo: u.activo ?? true,
      fechaRegistro: new Date().toISOString()
    }));
  },

  async createAdminUser(payload: {
    email: string;
    password: string;
    nombre: string;
    apellido: string;
    rol: string;
    telefono?: string;
    razonSocial?: string;
    nit?: string;
    direccionFiscal?: string;
    direccionEntregaDefault?: string;
  }): Promise<User> {
    const res = await fetch(`${API_BASE}/admin/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(payload)
    });
    const u = await handleResponse<any>(res, 'Error al crear usuario administrativo');
    return {
      id: u.id,
      nombre: `${u.nombre || ''} ${u.apellido || ''}`.trim(),
      email: u.email,
      rol: u.rol,
      telefono: u.telefono || '',
      direccion: u.direccion || '',
      documentoIdentidad: u.nit || ('C.C. ' + String(u.id).slice(0, 8)),
      activo: u.activo ?? true,
      fechaRegistro: new Date().toISOString()
    };
  },

  async updateAdminUser(id: string | number, payload: {
    nombre: string;
    apellido: string;
    telefono?: string;
    direccion?: string;
    razonSocial?: string;
    nit?: string;
  }): Promise<User> {
    const res = await fetch(`${API_BASE}/admin/usuarios/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(payload)
    });
    const u = await handleResponse<any>(res, 'Error al actualizar usuario');
    return {
      id: u.id,
      nombre: `${u.nombre || ''} ${u.apellido || ''}`.trim(),
      email: u.email,
      rol: u.rol,
      telefono: u.telefono || '',
      direccion: u.direccion || '',
      documentoIdentidad: u.nit || ('C.C. ' + String(u.id).slice(0, 8)),
      activo: u.activo ?? true,
      fechaRegistro: new Date().toISOString()
    };
  },

  async deleteAdminUser(id: string | number): Promise<void> {
    const res = await fetch(`${API_BASE}/admin/usuarios/${id}`, {
      method: 'DELETE',
      headers: { ...getAuthHeader() }
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.mensaje || err.message || 'Error al eliminar usuario');
    }
  },

  async toggleUserActive(userId: string | number): Promise<void> {
    const res = await fetch(`${API_BASE}/admin/usuarios/${userId}/toggle-activo`, {
      method: 'PUT',
      headers: { ...getAuthHeader() }
    });
    await handleResponse<any>(res, 'Error al cambiar estado de usuario');
  },

  async resetDemoData(): Promise<void> {
    console.info('Los datos residen en la base de datos real PostgreSQL.');
  }
};
