import React, { useState, useEffect } from 'react';
import { DashboardMetrics, User, AdminReservation, AdminInvoice, AdminPayment } from '../types';
import { api } from '../services/api';
import {
  DollarSign,
  Users,
  Wrench,
  Calendar,
  TrendingUp,
  ShieldCheck,
  UserCheck,
  UserX,
  UserPlus,
  Edit2,
  Trash2,
  Award,
  FileText,
  CreditCard,
  AlertTriangle,
  Download,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  ExternalLink
} from 'lucide-react';
import { useToast } from '../context/ToastContext';

export const AdminDashboard: React.FC = () => {
  const { toast } = useToast();
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [reservas, setReservas] = useState<AdminReservation[]>([]);
  const [facturas, setFacturas] = useState<AdminInvoice[]>([]);
  const [pagos, setPagos] = useState<AdminPayment[]>([]);
  const [loading, setLoading] = useState(true);

  // Tab state
  const [activeTab, setActiveTab] = useState<'usuarios' | 'reservas' | 'facturas' | 'pagos'>('usuarios');
  const [searchTerm, setSearchTerm] = useState('');
  const [reservaFilter, setReservaFilter] = useState<'TODAS' | 'DANOS' | 'EN_CURSO' | 'FINALIZADA' | 'PENDIENTE'>('TODAS');
  const [facturaFilter, setFacturaFilter] = useState<'TODAS' | 'FACTURA' | 'REPARACION' | 'NOTA_CREDITO'>('TODAS');
  const [pagoFilter, setPagoFilter] = useState<'TODOS' | 'ALQUILER' | 'REPARACION' | 'REEMBOLSO'>('TODOS');

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [deletingUser, setDeletingUser] = useState<User | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Form state for creation
  const [createForm, setCreateForm] = useState({
    email: '',
    password: '',
    nombre: '',
    apellido: '',
    rol: 'CLIENTE',
    telefono: '',
    razonSocial: '',
    direccionFiscal: '',
    direccionEntregaDefault: ''
  });

  // Form state for editing
  const [editForm, setEditForm] = useState({
    nombre: '',
    apellido: '',
    telefono: '',
    direccion: '',
    razonSocial: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const [m, u, r, f, p] = await Promise.all([
        api.getDashboardMetrics(),
        api.getAdminUsers(),
        api.getAdminReservas().catch(err => { console.error(err); return [] as AdminReservation[]; }),
        api.getAdminFacturas().catch(err => { console.error(err); return [] as AdminInvoice[]; }),
        api.getAdminPagos().catch(err => { console.error(err); return [] as AdminPayment[]; })
      ]);
      setMetrics(m);
      setUsers(u);
      setReservas(r);
      setFacturas(f);
      setPagos(p);
    } catch (e) {
      console.error('Error cargando métricas admin:', e);
    } finally {
      setLoading(false);
    }
  }

  const handleDownloadPdf = async (facturaId: string, numFactura: string) => {
    try {
      await api.downloadInvoicePdf(facturaId, numFactura);
      toast.success(`Factura ${numFactura} descargada exitosamente`, 'Descarga Completada');
    } catch (e: any) {
      toast.error(e.message || 'Error al descargar la factura', 'Error de Descarga');
    }
  };

  const handleToggleUser = async (userId: string | number) => {
    try {
      await api.toggleUserActive(userId);
      toast.success('Estado del usuario actualizado exitosamente', 'Usuario Modificado');
      loadData();
    } catch (e: any) {
      toast.error(e.message || 'Error al modificar estado del usuario', 'Error de Usuario');
    }
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      await api.createAdminUser(createForm);
      toast.success('Usuario registrado exitosamente', 'Usuario Creado');
      setShowCreateModal(false);
      setCreateForm({
        email: '',
        password: '',
        nombre: '',
        apellido: '',
        rol: 'CLIENTE',
        telefono: '',
        razonSocial: '',
        direccionFiscal: '',
        direccionEntregaDefault: ''
      });
      await loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al registrar usuario', 'Fallo al Crear');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenEdit = (u: User) => {
    setEditingUser(u);
    const parts = u.nombre.split(' ');
    setEditForm({
      nombre: parts[0] || u.nombre,
      apellido: parts.slice(1).join(' ') || '',
      telefono: u.telefono || '',
      direccion: u.direccion || '',
      razonSocial: u.nombre
    });
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    try {
      setSubmitting(true);
      await api.updateAdminUser(editingUser.id, editForm);
      toast.success('Datos de usuario actualizados correctamente', 'Perfil Actualizado');
      setEditingUser(null);
      await loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al actualizar usuario', 'Fallo de Actualización');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSubmit = async () => {
    if (!deletingUser) return;
    try {
      setSubmitting(true);
      await api.deleteAdminUser(deletingUser.id);
      toast.success('Usuario eliminado permanentemente del sistema', 'Usuario Eliminado');
      setDeletingUser(null);
      await loadData();
    } catch (err: any) {
      toast.error(err.message || 'No se pudo eliminar el usuario', 'Restricción de Integridad');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || !metrics) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-600"></div>
      </div>
    );
  }

  const { resumen, disponibilidadInventario, masAlquiladas, clientesFrecuentes, ingresosMensuales } = metrics;
  const maxIngreso = Math.max(...ingresosMensuales.map((i) => i.ingresos), 1);

  // Filters for Reservas
  const filteredReservas = reservas.filter(r => {
    if (reservaFilter === 'DANOS' && !r.presentaDanos) return false;
    if (reservaFilter === 'EN_CURSO' && r.estado !== 'EN_CURSO') return false;
    if (reservaFilter === 'FINALIZADA' && r.estado !== 'FINALIZADA') return false;
    if (reservaFilter === 'PENDIENTE' && r.estado !== 'PENDIENTE') return false;
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      r.codigoReserva?.toLowerCase().includes(term) ||
      r.id.toLowerCase().includes(term) ||
      r.herramientaNombre?.toLowerCase().includes(term) ||
      r.clienteNombre?.toLowerCase().includes(term) ||
      r.clienteEmail?.toLowerCase().includes(term) ||
      r.propietarioNombre?.toLowerCase().includes(term) ||
      r.propietarioRazonSocial?.toLowerCase().includes(term)
    );
  });

  // Filters for Facturas
  const filteredFacturas = facturas.filter(f => {
    if (facturaFilter !== 'TODAS' && f.tipo !== facturaFilter) return false;
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      f.numeroFactura?.toLowerCase().includes(term) ||
      f.codigoReserva?.toLowerCase().includes(term) ||
      f.herramientaNombre?.toLowerCase().includes(term) ||
      f.clienteNombre?.toLowerCase().includes(term) ||
      f.descripcionDano?.toLowerCase().includes(term)
    );
  });

  // Filters for Pagos
  const filteredPagos = pagos.filter(p => {
    if (pagoFilter !== 'TODOS' && p.tipo !== pagoFilter) return false;
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      p.transaccionId?.toLowerCase().includes(term) ||
      p.codigoReserva?.toLowerCase().includes(term) ||
      p.numeroFactura?.toLowerCase().includes(term) ||
      p.herramientaNombre?.toLowerCase().includes(term) ||
      p.clienteNombre?.toLowerCase().includes(term) ||
      p.proveedorPago?.toLowerCase().includes(term)
    );
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-[#17191C] text-[#F5A623] text-xs font-bold uppercase tracking-wider mb-2.5 border border-[#292D32]">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Supervisión y Control Administrativo</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Métricas de Plataforma y Gestión de Operaciones
          </h1>
          <p className="text-xs sm:text-sm text-neutral-600 mt-1">
            Supervisión integral de reservas, cobros por daños, facturación fiscal DIAN, pagos y cuentas.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2.5 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer inline-flex items-center gap-2 shadow-xs"
          >
            <UserPlus className="w-4 h-4" />
            <span>Registrar Usuario</span>
          </button>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-xs font-semibold text-neutral-500 mb-2">
            <span>Ingresos Totales (COP)</span>
            <DollarSign className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            ${resumen.totalIngresos.toLocaleString('es-CO')}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {resumen.totalFacturas} facturas emitidas
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-xs font-semibold text-neutral-500 mb-2">
            <span>Reservas Activas</span>
            <Calendar className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            {resumen.reservasActivas}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {resumen.totalReservas} contratos registrados
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-xs font-semibold text-neutral-500 mb-2">
            <span>Inventario de Equipos</span>
            <Wrench className="w-4 h-4 text-amber-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            {resumen.totalHerramientas}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {disponibilidadInventario.disponibles} disponibles · {disponibilidadInventario.alquiladas} en obra
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-xs font-semibold text-neutral-500 mb-2">
            <span>Comunidad de Usuarios</span>
            <Users className="w-4 h-4 text-purple-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            {resumen.totalUsuarios}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {resumen.totalClientes} clientes · {resumen.totalPropietarios} proveedores
          </span>
        </div>
      </div>

      {/* Analytics Charts & Rankings */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-10">
        {/* Monthly Revenue Bar Chart */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-amber-600" />
                <span>Evolución Mensual (COP)</span>
              </h3>
              <span className="text-xs text-neutral-500 font-mono">Año 2026</span>
            </div>

            {ingresosMensuales.length === 0 ? (
              <div className="py-8 text-center text-xs text-neutral-400">
                No hay transacciones registradas aún
              </div>
            ) : (
              <div className="space-y-3">
                {ingresosMensuales.map((item, idx) => (
                  <div key={idx} className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="font-semibold text-neutral-700">{item.mes}</span>
                      <span className="font-mono text-neutral-900 font-bold">
                        ${item.ingresos.toLocaleString('es-CO')}
                      </span>
                    </div>
                    <div className="w-full bg-neutral-100 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-amber-600 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${(item.ingresos / maxIngreso) * 100}%` }}
                      ></div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Top Rented Tools */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
              <Award className="w-4 h-4 text-amber-600" />
              <span>Equipos Más Alquilados</span>
            </h3>
            <span className="text-xs text-neutral-500">Ranking Top</span>
          </div>

          {masAlquiladas.length === 0 ? (
            <div className="py-8 text-center text-xs text-neutral-400">
              Sin datos de alquiler registrados
            </div>
          ) : (
            <div className="space-y-3">
              {masAlquiladas.map((item, idx) => (
                <div key={idx} className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm font-extrabold text-amber-600 w-5">
                      #{idx + 1}
                    </span>
                    <div>
                      <h4 className="font-bold text-neutral-900 line-clamp-1">{item.nombre}</h4>
                      <span className="text-[11px] text-neutral-500 font-mono">
                        ${item.precioPorDia.toLocaleString('es-CO')} / día
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="px-2 py-0.5 rounded-md bg-amber-100 text-amber-900 font-bold font-mono text-xs">
                      {item.vecesAlquilada} contratos
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Frequent Clients */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
              <Users className="w-4 h-4 text-purple-600" />
              <span>Clientes Frecuentes</span>
            </h3>
            <span className="text-xs text-neutral-500">Top Contratistas</span>
          </div>

          {clientesFrecuentes.length === 0 ? (
            <div className="py-8 text-center text-xs text-neutral-400">
              Sin historial de clientes registrado
            </div>
          ) : (
            <div className="space-y-3">
              {clientesFrecuentes.map((item, idx) => (
                <div key={idx} className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm font-extrabold text-purple-600 w-5">
                      #{idx + 1}
                    </span>
                    <div>
                      <h4 className="font-bold text-neutral-900">{item.nombre}</h4>
                      <span className="text-[11px] text-neutral-500 font-mono">
                        ${item.gasto.toLocaleString('es-CO')} COP invertidos
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-900 font-bold font-mono text-xs">
                      {item.reservas} reservas
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Module Navigation Tabs */}
      <div className="flex border-b border-neutral-200 mb-6 gap-2 sm:gap-4 overflow-x-auto pb-1" id="admin-tabs">
        <button
          id="tab-btn-usuarios"
          onClick={() => { setActiveTab('usuarios'); setSearchTerm(''); }}
          className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'usuarios'
              ? 'border-amber-600 text-amber-600 bg-amber-50/50 rounded-t-lg'
              : 'border-transparent text-neutral-500 hover:text-neutral-900 hover:border-neutral-300'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Gestión de Cuentas</span>
          <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-neutral-100 text-neutral-600 font-mono">
            {users.length}
          </span>
        </button>

        <button
          id="tab-btn-reservas"
          onClick={() => { setActiveTab('reservas'); setSearchTerm(''); }}
          className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'reservas'
              ? 'border-amber-600 text-amber-600 bg-amber-50/50 rounded-t-lg'
              : 'border-transparent text-neutral-500 hover:text-neutral-900 hover:border-neutral-300'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>Historial de Reservas y Devoluciones</span>
          <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-neutral-100 text-neutral-600 font-mono">
            {reservas.length}
          </span>
          {reservas.filter(r => r.presentaDanos).length > 0 && (
            <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-rose-100 text-rose-700 font-bold font-mono">
              {reservas.filter(r => r.presentaDanos).length} con daños
            </span>
          )}
        </button>

        <button
          id="tab-btn-facturas"
          onClick={() => { setActiveTab('facturas'); setSearchTerm(''); }}
          className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'facturas'
              ? 'border-amber-600 text-amber-600 bg-amber-50/50 rounded-t-lg'
              : 'border-transparent text-neutral-500 hover:text-neutral-900 hover:border-neutral-300'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Facturas y Cobros</span>
          <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-neutral-100 text-neutral-600 font-mono">
            {facturas.length}
          </span>
        </button>

        <button
          id="tab-btn-pagos"
          onClick={() => { setActiveTab('pagos'); setSearchTerm(''); }}
          className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'pagos'
              ? 'border-amber-600 text-amber-600 bg-amber-50/50 rounded-t-lg'
              : 'border-transparent text-neutral-500 hover:text-neutral-900 hover:border-neutral-300'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Transacciones y Pagos</span>
          <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-neutral-100 text-neutral-600 font-mono">
            {pagos.length}
          </span>
        </button>
      </div>

      {/* TAB 1: GESTIÓN DE CUENTAS */}
      {activeTab === 'usuarios' && (
        <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs animate-in fade-in duration-200">
          <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h3 className="text-sm font-bold text-neutral-900">
                Gestión Integral de Usuarios (Clientes, Proveedores y Administradores)
              </h3>
              <p className="text-[11px] text-neutral-500">Supervisión, activación, suspensión y edición de perfiles en la plataforma.</p>
            </div>
            <span className="text-xs text-neutral-500 font-mono">Total: {users.length} cuentas</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                  <th className="py-3 px-4">Usuario</th>
                  <th className="py-3 px-4">Documento / ID</th>
                  <th className="py-3 px-4">Rol</th>
                  <th className="py-3 px-4">Contacto / Dirección</th>
                  <th className="py-3 px-4">Estado</th>
                  <th className="py-3 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-neutral-50">
                    <td className="py-3 px-4">
                      <span className="font-bold text-neutral-900 block">{u.nombre}</span>
                      <span className="text-[11px] text-neutral-500 font-mono">{u.email}</span>
                    </td>
                    <td className="py-3 px-4 font-mono text-neutral-700">{u.documentoIdentidad}</td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                        u.rol === 'ADMIN' ? 'bg-purple-100 text-purple-800' :
                        u.rol === 'PROPIETARIO' ? 'bg-blue-100 text-blue-800' :
                        'bg-emerald-100 text-emerald-800'
                      }`}>
                        {u.rol}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-neutral-600">
                      <div>{u.telefono || 'Sin teléfono'}</div>
                      <div className="text-[10px] text-neutral-400 truncate max-w-[150px]">{u.direccion || 'Sin dirección'}</div>
                    </td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                        u.activo ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                      }`}>
                        {u.activo ? 'Activo' : 'Suspendido'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right space-x-1 whitespace-nowrap">
                      <button
                        onClick={() => handleOpenEdit(u)}
                        className="px-2 py-1 text-xs font-semibold text-neutral-700 bg-white hover:bg-neutral-100 border border-neutral-300 rounded-md transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                        title="Editar datos de contacto y perfil"
                      >
                        <Edit2 className="w-3 h-3 text-neutral-600" />
                        <span>Editar</span>
                      </button>

                      {u.rol !== 'ADMIN' && (
                        <>
                          <button
                            onClick={() => handleToggleUser(u.id)}
                            className={`px-2 py-1 text-xs font-semibold rounded-md transition-colors cursor-pointer inline-flex items-center gap-1 ${
                              u.activo
                                ? 'text-amber-800 bg-amber-50 hover:bg-amber-100 border border-amber-200'
                                : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200'
                            }`}
                            title={u.activo ? 'Suspender acceso a la plataforma' : 'Reactivar acceso a la plataforma'}
                          >
                            {u.activo ? (
                              <>
                                <UserX className="w-3 h-3" />
                                <span>Suspender</span>
                              </>
                            ) : (
                              <>
                                <UserCheck className="w-3 h-3" />
                                <span>Activar</span>
                              </>
                            )}
                          </button>

                          <button
                            onClick={() => setDeletingUser(u)}
                            className="px-2 py-1 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-md transition-colors cursor-pointer inline-flex items-center gap-1"
                            title="Eliminar usuario del sistema"
                          >
                            <Trash2 className="w-3 h-3" />
                            <span>Eliminar</span>
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: HISTORIAL DE RESERVAS, DEVOLUCIONES Y DAÑOS */}
      {activeTab === 'reservas' && (
        <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs animate-in fade-in duration-200" id="admin-reservas-section">
          <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-bold text-neutral-900">
                Auditoría Global de Reservas y Gestión de Devoluciones
              </h3>
              <p className="text-[11px] text-neutral-500">
                Seguimiento de ciclo de vida completo: entrega en obra, devolución técnica y cobro por daños.
              </p>
            </div>

            {/* Filter buttons */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => setReservaFilter('TODAS')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  reservaFilter === 'TODAS'
                    ? 'bg-neutral-900 text-white border-neutral-900'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Todas ({reservas.length})
              </button>
              <button
                onClick={() => setReservaFilter('DANOS')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer inline-flex items-center gap-1.5 ${
                  reservaFilter === 'DANOS'
                    ? 'bg-rose-600 text-white border-rose-600'
                    : 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100'
                }`}
              >
                <AlertTriangle className="w-3 h-3" />
                <span>Con Daños ({reservas.filter(r => r.presentaDanos).length})</span>
              </button>
              <button
                onClick={() => setReservaFilter('EN_CURSO')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  reservaFilter === 'EN_CURSO'
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                En Obra ({reservas.filter(r => r.estado === 'EN_CURSO').length})
              </button>
              <button
                onClick={() => setReservaFilter('FINALIZADA')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  reservaFilter === 'FINALIZADA'
                    ? 'bg-emerald-600 text-white border-emerald-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Finalizadas ({reservas.filter(r => r.estado === 'FINALIZADA').length})
              </button>
            </div>
          </div>

          {/* Search box */}
          <div className="p-4 border-b border-neutral-100 bg-white">
            <div className="relative max-w-md">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por código, equipo, cliente o proveedor..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-1.5 text-xs border border-neutral-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs" id="tabla-reservas-admin">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                  <th className="py-3 px-4">Ref. / Código</th>
                  <th className="py-3 px-4">Herramienta / Equipo</th>
                  <th className="py-3 px-4">Cliente Titular</th>
                  <th className="py-3 px-4">Propietario / Empresa</th>
                  <th className="py-3 px-4">Fechas Contrato</th>
                  <th className="py-3 px-4">Estado Reserva</th>
                  <th className="py-3 px-4">Inspección / Daños Reportados</th>
                  <th className="py-3 px-4 text-right">Total Alquiler</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredReservas.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-xs text-neutral-400">
                      No se encontraron reservas con los filtros seleccionados
                    </td>
                  </tr>
                ) : (
                  filteredReservas.map((r) => (
                    <tr key={r.id} className="hover:bg-neutral-50/80 transition-colors">
                      <td className="py-3 px-4">
                        <span className="font-mono font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-200 block w-max">
                          {r.codigoReserva || r.id.slice(0, 8)}
                        </span>
                        <span className="text-[10px] text-neutral-400 font-mono truncate max-w-[120px] block mt-0.5" title={r.id}>
                          {r.id}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-bold text-neutral-900 block">{r.herramientaNombre}</span>
                        <span className="text-[11px] text-neutral-500">ID: {String(r.herramientaId).slice(0, 8)}</span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-neutral-800 block">{r.clienteNombre}</span>
                        <span className="text-[11px] text-neutral-500 font-mono">{r.clienteEmail}</span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-neutral-800 block">{r.propietarioRazonSocial || r.propietarioNombre}</span>
                        <span className="text-[11px] text-neutral-500">{r.propietarioNombre}</span>
                      </td>
                      <td className="py-3 px-4 font-mono text-[11px] text-neutral-600">
                        <div>{r.fechaInicio}</div>
                        <div className="text-neutral-400">hasta {r.fechaFin}</div>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
                          r.estado === 'FINALIZADA' ? 'bg-neutral-200 text-neutral-800' :
                          r.estado === 'EN_CURSO' ? 'bg-blue-100 text-blue-800 border border-blue-200' :
                          r.estado === 'CONFIRMADA' ? 'bg-amber-100 text-amber-800' :
                          r.estado === 'CANCELADA' ? 'bg-rose-100 text-rose-800' :
                          'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}>
                          {r.estado === 'EN_CURSO' ? 'EN OBRA' : r.estado}
                        </span>
                        <div className="text-[10px] text-neutral-500 mt-0.5">
                          {r.pagada ? '✓ Pago confirmado' : '⚠ Pago pendiente'}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        {r.presentaDanos ? (
                          <div className="space-y-1">
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                              <AlertTriangle className="w-3 h-3 text-rose-600" />
                              <span>Devuelto con Daños</span>
                            </span>
                            <div className="text-[11px] text-rose-950 font-medium bg-rose-50/80 p-2 rounded border border-rose-200/60 max-w-[260px]">
                              <span className="font-bold block text-rose-900 font-mono">
                                Costo: ${r.costoReparacion?.toLocaleString('es-CO')} COP
                              </span>
                              <span className="text-neutral-700 block italic text-[10px] line-clamp-2 mt-0.5">
                                "{r.descripcionDano}"
                              </span>
                              {r.facturaReparacionNumero && (
                                <div className="mt-1 pt-1 border-t border-rose-200/60 flex items-center justify-between text-[10px]">
                                  <span className="font-mono font-semibold text-rose-900">
                                    Factura: {r.facturaReparacionNumero}
                                  </span>
                                  <span className={`px-1.5 py-0.2 rounded font-bold ${
                                    r.facturaReparacionEstado === 'PAGADA' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                                  }`}>
                                    {r.facturaReparacionEstado || 'PENDIENTE'}
                                  </span>
                                </div>
                              )}
                            </div>
                          </div>
                        ) : (
                          <span className="text-neutral-400 text-xs italic block">
                            {r.estado === 'FINALIZADA' ? 'Retorno conforme / Sin daños' : 'Sin novedad de daños'}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <span className="font-mono font-bold text-neutral-900 block">
                          ${r.total?.toLocaleString('es-CO')} COP
                        </span>
                        <span className="text-[10px] text-neutral-400 font-mono">
                          IVA: ${r.iva?.toLocaleString('es-CO')}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: FACTURAS Y COBROS */}
      {activeTab === 'facturas' && (
        <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs animate-in fade-in duration-200" id="admin-facturas-section">
          <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-bold text-neutral-900">
                Facturación Electrónica y Documentos Tributarios
              </h3>
              <p className="text-[11px] text-neutral-500">
                Auditoría diferenciada de comprobantes por alquiler de equipos y facturas por cobro de reparaciones.
              </p>
            </div>

            {/* Filter buttons */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => setFacturaFilter('TODAS')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  facturaFilter === 'TODAS'
                    ? 'bg-neutral-900 text-white border-neutral-900'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Todas ({facturas.length})
              </button>
              <button
                onClick={() => setFacturaFilter('FACTURA')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  facturaFilter === 'FACTURA'
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Alquiler ({facturas.filter(f => f.tipo === 'FACTURA').length})
              </button>
              <button
                onClick={() => setFacturaFilter('REPARACION')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer inline-flex items-center gap-1.5 ${
                  facturaFilter === 'REPARACION'
                    ? 'bg-rose-600 text-white border-rose-600'
                    : 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100'
                }`}
              >
                <AlertTriangle className="w-3 h-3" />
                <span>Reparaciones ({facturas.filter(f => f.tipo === 'REPARACION').length})</span>
              </button>
              <button
                onClick={() => setFacturaFilter('NOTA_CREDITO')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  facturaFilter === 'NOTA_CREDITO'
                    ? 'bg-purple-600 text-white border-purple-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Notas Crédito ({facturas.filter(f => f.tipo === 'NOTA_CREDITO').length})
              </button>
            </div>
          </div>

          {/* Search box */}
          <div className="p-4 border-b border-neutral-100 bg-white">
            <div className="relative max-w-md">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por N° factura, reserva, equipo o descripción..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-1.5 text-xs border border-neutral-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs" id="tabla-facturas-admin">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                  <th className="py-3 px-4">N° Factura</th>
                  <th className="py-3 px-4">Tipo de Comprobante</th>
                  <th className="py-3 px-4">Concepto / Equipo</th>
                  <th className="py-3 px-4">Cliente / Reserva</th>
                  <th className="py-3 px-4">Emisión</th>
                  <th className="py-3 px-4">Total (COP)</th>
                  <th className="py-3 px-4">Estado</th>
                  <th className="py-3 px-4 text-right">Documento</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredFacturas.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-xs text-neutral-400">
                      No se encontraron facturas con los filtros seleccionados
                    </td>
                  </tr>
                ) : (
                  filteredFacturas.map((f) => (
                    <tr key={f.id} className="hover:bg-neutral-50/80 transition-colors">
                      <td className="py-3 px-4 font-mono font-bold text-neutral-900">
                        {f.numeroFactura}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                          f.tipo === 'REPARACION'
                            ? 'bg-rose-100 text-rose-800 border border-rose-200'
                            : f.tipo === 'NOTA_CREDITO'
                            ? 'bg-purple-100 text-purple-800 border border-purple-200'
                            : 'bg-blue-100 text-blue-800 border border-blue-200'
                        }`}>
                          {f.tipo === 'REPARACION' ? 'REPARACIÓN POR DAÑOS' : f.tipo === 'NOTA_CREDITO' ? 'NOTA CRÉDITO' : 'ALQUILER'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-neutral-900 block">{f.concepto || f.herramientaNombre}</span>
                        {f.descripcionDano && (
                          <span className="text-[10px] text-rose-700 italic block mt-0.5 max-w-[220px]">
                            Daño: "{f.descripcionDano}"
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-medium text-neutral-800 block">{f.clienteNombre}</span>
                        {f.codigoReserva && (
                          <span className="font-mono text-[10px] text-amber-700 bg-amber-50 px-1.5 py-0.2 rounded border border-amber-200">
                            {f.codigoReserva}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 font-mono text-[11px] text-neutral-600">
                        {f.fechaEmision ? new Date(f.fechaEmision).toLocaleDateString('es-CO') : 'N/A'}
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-neutral-900">
                        ${f.total?.toLocaleString('es-CO')} COP
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
                          f.estado === 'PAGADA'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}>
                          {f.estado || 'PENDIENTE'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => handleDownloadPdf(f.id, f.numeroFactura)}
                          className="px-2.5 py-1 text-xs font-semibold text-neutral-700 bg-white hover:bg-neutral-100 border border-neutral-300 rounded-md transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                          title="Descargar comprobante en PDF"
                        >
                          <Download className="w-3.5 h-3.5 text-neutral-500" />
                          <span>PDF</span>
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 4: TRANSACCIONES Y PAGOS */}
      {activeTab === 'pagos' && (
        <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs animate-in fade-in duration-200" id="admin-pagos-section">
          <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-bold text-neutral-900">
                Historial de Transacciones Financieras y Pasarelas de Pago
              </h3>
              <p className="text-[11px] text-neutral-500">
                Detalle individual de pagos de alquiler, cobros por reparación y desembolsos de reembolsos.
              </p>
            </div>

            {/* Filter buttons */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => setPagoFilter('TODOS')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  pagoFilter === 'TODOS'
                    ? 'bg-neutral-900 text-white border-neutral-900'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Todos ({pagos.length})
              </button>
              <button
                onClick={() => setPagoFilter('ALQUILER')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  pagoFilter === 'ALQUILER'
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Alquiler ({pagos.filter(p => p.tipo === 'ALQUILER').length})
              </button>
              <button
                onClick={() => setPagoFilter('REPARACION')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer inline-flex items-center gap-1.5 ${
                  pagoFilter === 'REPARACION'
                    ? 'bg-rose-600 text-white border-rose-600'
                    : 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100'
                }`}
              >
                <AlertTriangle className="w-3 h-3" />
                <span>Reparaciones ({pagos.filter(p => p.tipo === 'REPARACION').length})</span>
              </button>
              <button
                onClick={() => setPagoFilter('REEMBOLSO')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md border cursor-pointer ${
                  pagoFilter === 'REEMBOLSO'
                    ? 'bg-purple-600 text-white border-purple-600'
                    : 'bg-white text-neutral-600 border-neutral-200 hover:bg-neutral-100'
                }`}
              >
                Reembolsos ({pagos.filter(p => p.tipo === 'REEMBOLSO').length})
              </button>
            </div>
          </div>

          {/* Search box */}
          <div className="p-4 border-b border-neutral-100 bg-white">
            <div className="relative max-w-md">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por ID transacción, reserva, cliente o pasarela..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-1.5 text-xs border border-neutral-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs" id="tabla-pagos-admin">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                  <th className="py-3 px-4">Ref. Transacción / ID</th>
                  <th className="py-3 px-4">Tipo de Transacción</th>
                  <th className="py-3 px-4">Equipo / Concepto</th>
                  <th className="py-3 px-4">Cliente / Reserva</th>
                  <th className="py-3 px-4">Factura Asociada</th>
                  <th className="py-3 px-4">Método / Pasarela</th>
                  <th className="py-3 px-4">Monto (COP)</th>
                  <th className="py-3 px-4 text-right">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredPagos.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-xs text-neutral-400">
                      No se encontraron pagos con los filtros seleccionados
                    </td>
                  </tr>
                ) : (
                  filteredPagos.map((p) => (
                    <tr key={p.id} className="hover:bg-neutral-50/80 transition-colors">
                      <td className="py-3 px-4">
                        <span className="font-mono font-bold text-neutral-800 block truncate max-w-[140px]" title={p.transaccionId}>
                          {p.transaccionId || p.id.slice(0, 8)}
                        </span>
                        <span className="text-[10px] text-neutral-400 font-mono block">
                          {p.fechaPago ? new Date(p.fechaPago).toLocaleString('es-CO') : 'Sin fecha'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                          p.tipo === 'REPARACION'
                            ? 'bg-rose-100 text-rose-800 border border-rose-200'
                            : p.tipo === 'REEMBOLSO'
                            ? 'bg-purple-100 text-purple-800 border border-purple-200'
                            : 'bg-blue-100 text-blue-800 border border-blue-200'
                        }`}>
                          {p.tipo === 'REPARACION' ? 'PAGO REPARACIÓN' : p.tipo === 'REEMBOLSO' ? 'REEMBOLSO' : 'PAGO ALQUILER'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-neutral-900 block">{p.herramientaNombre}</span>
                        {p.motivo && <span className="text-[10px] text-neutral-500 block truncate max-w-[160px]">{p.motivo}</span>}
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-medium text-neutral-800 block">{p.clienteNombre}</span>
                        {p.codigoReserva && (
                          <span className="font-mono text-[10px] text-amber-700 bg-amber-50 px-1.5 py-0.2 rounded border border-amber-200">
                            {p.codigoReserva}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 font-mono font-semibold text-neutral-700">
                        {p.numeroFactura || 'N/A'}
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded bg-neutral-100 text-neutral-700 font-mono text-[11px] font-semibold">
                          {p.proveedorPago}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-neutral-900">
                        ${p.monto?.toLocaleString('es-CO')} COP
                      </td>
                      <td className="py-3 px-4 text-right">
                        <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
                          p.estado === 'COMPLETADO'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : p.estado === 'REEMBOLSADO'
                            ? 'bg-purple-50 text-purple-700 border border-purple-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}>
                          {p.estado}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Crear Usuario */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-neutral-200 text-left">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-100 mb-4">
              <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-amber-600" />
                <span>Registrar Nuevo Usuario en Plataforma</span>
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="text-neutral-400 hover:text-neutral-600 font-bold">✕</button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Nombre</label>
                  <input
                    type="text"
                    required
                    value={createForm.nombre}
                    onChange={(e) => setCreateForm({ ...createForm, nombre: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Apellido</label>
                  <input
                    type="text"
                    required
                    value={createForm.apellido}
                    onChange={(e) => setCreateForm({ ...createForm, apellido: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="font-semibold text-neutral-700 block mb-1">Correo Electrónico</label>
                <input
                  type="email"
                  required
                  value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  className="w-full p-2 border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="font-semibold text-neutral-700 block mb-1">Contraseña Inicial</label>
                <input
                  type="password"
                  required
                  value={createForm.password}
                  onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  className="w-full p-2 border border-neutral-300 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Rol</label>
                  <select
                    value={createForm.rol}
                    onChange={(e) => setCreateForm({ ...createForm, rol: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg bg-white"
                  >
                    <option value="CLIENTE">CLIENTE</option>
                    <option value="PROPIETARIO">PROPIETARIO</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Teléfono</label>
                  <input
                    type="text"
                    value={createForm.telefono}
                    onChange={(e) => setCreateForm({ ...createForm, telefono: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                    placeholder="+573001234567"
                  />
                </div>
              </div>

              {createForm.rol === 'PROPIETARIO' && (
                <>
                  <div>
                    <label className="font-semibold text-neutral-700 block mb-1">Razón Social</label>
                    <input
                      type="text"
                      value={createForm.razonSocial}
                      onChange={(e) => setCreateForm({ ...createForm, razonSocial: e.target.value })}
                      className="w-full p-2 border border-neutral-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-neutral-700 block mb-1">Dirección Fiscal</label>
                    <input
                      type="text"
                      value={createForm.direccionFiscal}
                      onChange={(e) => setCreateForm({ ...createForm, direccionFiscal: e.target.value })}
                      className="w-full p-2 border border-neutral-300 rounded-lg"
                    />
                  </div>
                </>
              )}

              {createForm.rol === 'CLIENTE' && (
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Dirección de Entrega</label>
                  <input
                    type="text"
                    value={createForm.direccionEntregaDefault}
                    onChange={(e) => setCreateForm({ ...createForm, direccionEntregaDefault: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
              )}

              <div className="flex justify-end gap-2 pt-4 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 font-semibold text-neutral-700 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 font-bold text-neutral-900 bg-amber-500 hover:bg-amber-600 rounded-lg"
                >
                  {submitting ? 'Guardando...' : 'Crear Usuario'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Editar Usuario */}
      {editingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200 text-left">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-100 mb-4">
              <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                <Edit2 className="w-5 h-5 text-amber-600" />
                <span>Editar Datos de Usuario</span>
              </h3>
              <button onClick={() => setEditingUser(null)} className="text-neutral-400 hover:text-neutral-600 font-bold">✕</button>
            </div>

            <form onSubmit={handleEditSubmit} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Nombre</label>
                  <input
                    type="text"
                    required
                    value={editForm.nombre}
                    onChange={(e) => setEditForm({ ...editForm, nombre: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Apellido</label>
                  <input
                    type="text"
                    value={editForm.apellido}
                    onChange={(e) => setEditForm({ ...editForm, apellido: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="font-semibold text-neutral-700 block mb-1">Teléfono</label>
                <input
                  type="text"
                  value={editForm.telefono}
                  onChange={(e) => setEditForm({ ...editForm, telefono: e.target.value })}
                  className="w-full p-2 border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="font-semibold text-neutral-700 block mb-1">Dirección</label>
                <input
                  type="text"
                  value={editForm.direccion}
                  onChange={(e) => setEditForm({ ...editForm, direccion: e.target.value })}
                  className="w-full p-2 border border-neutral-300 rounded-lg"
                />
              </div>

              {editingUser.rol === 'PROPIETARIO' && (
                <div>
                  <label className="font-semibold text-neutral-700 block mb-1">Razón Social</label>
                  <input
                    type="text"
                    value={editForm.razonSocial}
                    onChange={(e) => setEditForm({ ...editForm, razonSocial: e.target.value })}
                    className="w-full p-2 border border-neutral-300 rounded-lg"
                  />
                </div>
              )}

              <div className="flex justify-end gap-2 pt-4 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setEditingUser(null)}
                  className="px-4 py-2 font-semibold text-neutral-700 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 font-bold text-neutral-900 bg-amber-500 hover:bg-amber-600 rounded-lg"
                >
                  {submitting ? 'Guardando...' : 'Actualizar Usuario'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Eliminar Usuario */}
      {deletingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200 text-left">
            <div className="flex items-center gap-3 text-rose-600 mb-3">
              <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <h3 className="text-base font-bold text-neutral-900">Eliminar Cuenta de Usuario</h3>
                <span className="text-xs text-neutral-500">{deletingUser.email}</span>
              </div>
            </div>

            <p className="text-xs text-neutral-600 mb-4 leading-relaxed">
              ¿Estás seguro de que deseas eliminar este usuario? Si la cuenta posee historial de reservas, herramientas o pagos, la operación será rechazada por seguridad y deberás usar la opción de <strong>Suspensión</strong>.
            </p>

            <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
              <button
                type="button"
                onClick={() => setDeletingUser(null)}
                disabled={submitting}
                className="px-4 py-2 text-xs font-semibold text-neutral-700 hover:bg-neutral-100 rounded-lg"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleDeleteSubmit}
                disabled={submitting}
                className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg"
              >
                {submitting ? 'Eliminando...' : 'Confirmar Eliminación'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
