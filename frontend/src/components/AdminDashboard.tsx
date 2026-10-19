import React, { useState, useEffect } from 'react';
import { DashboardMetrics, User } from '../types';
import { api } from '../services/api';
import { DollarSign, Users, Wrench, Calendar, TrendingUp, CheckCircle, ShieldCheck, UserCheck, UserX, UserPlus, Edit2, Trash2, Award } from 'lucide-react';
import { useToast } from '../context/ToastContext';

export const AdminDashboard: React.FC = () => {
  const { toast } = useToast();
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

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
    nit: '',
    direccionFiscal: '',
    direccionEntregaDefault: ''
  });

  // Form state for editing
  const [editForm, setEditForm] = useState({
    nombre: '',
    apellido: '',
    telefono: '',
    direccion: '',
    razonSocial: '',
    nit: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const [m, u] = await Promise.all([
        api.getDashboardMetrics(),
        api.getAdminUsers()
      ]);
      setMetrics(m);
      setUsers(u);
    } catch (e) {
      console.error('Error cargando métricas admin:', e);
    } finally {
      setLoading(false);
    }
  }

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
        nit: '',
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
      razonSocial: u.nombre,
      nit: u.documentoIdentidad.startsWith('C.C.') ? '' : u.documentoIdentidad
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Panel de Control Administrativo</span>
          </div>
          <h1 className="text-2xl font-black text-neutral-900 tracking-tight">
            Métricas de Plataforma y Gestión de Usuarios
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Supervisión consolidada de inventario, reservas, facturación fiscal y cuentas registradas.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-xl transition-colors cursor-pointer inline-flex items-center gap-2 shadow-xs"
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
              <div className="space-y-3 pt-2">
                {ingresosMensuales.map((item) => {
                  const percentage = Math.round((item.ingresos / maxIngreso) * 100);
                  return (
                    <div key={item.mes} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-neutral-700 w-24">{item.mes}</span>
                        <span className="font-mono text-neutral-900 tabular-nums font-bold">
                          ${item.ingresos.toLocaleString('es-CO')}
                        </span>
                      </div>
                      <div className="h-3 w-full bg-neutral-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-amber-500 rounded-full transition-all duration-500"
                          style={{ width: `${Math.max(5, percentage)}%` }}
                        ></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
          <p className="text-[11px] text-neutral-400 mt-6 pt-3 border-t border-neutral-100">
            Ingresos netos por contratos liquidados en pasarela.
          </p>
        </div>

        {/* Most Rented Equipment */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs">
          <h3 className="text-sm font-bold text-neutral-900 mb-4 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-amber-600" />
            <span>Equipos Más Alquilados</span>
          </h3>

          {masAlquiladas.length === 0 ? (
            <div className="py-8 text-center text-xs text-neutral-400">
              No hay contratos registrados
            </div>
          ) : (
            <div className="space-y-3">
              {masAlquiladas.map((item, idx) => (
                <div key={item.id} className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm font-extrabold text-amber-600 w-5">
                      #{idx + 1}
                    </span>
                    <div>
                      <h4 className="font-bold text-neutral-900 truncate max-w-[150px]">{item.nombre}</h4>
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
          <h3 className="text-sm font-bold text-neutral-900 mb-4 flex items-center gap-2">
            <Award className="w-4 h-4 text-amber-600" />
            <span>Clientes Frecuentes</span>
          </h3>

          {clientesFrecuentes.length === 0 ? (
            <div className="py-8 text-center text-xs text-neutral-400">
              Sin actividad de clientes
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

      {/* User Management Table */}
      <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
          <h3 className="text-sm font-bold text-neutral-900">
            Gestión Integral de Usuarios (Clientes, Proveedores y Administradores)
          </h3>
          <span className="text-xs text-neutral-500 font-mono">Total: {users.length} cuentas registradas</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                <th className="py-3 px-4">Usuario</th>
                <th className="py-3 px-4">Documento / NIT</th>
                <th className="py-3 px-4">Rol</th>
                <th className="py-3 px-4">Contacto / Dirección</th>
                <th className="py-3 px-4">Estado</th>
                <th className="py-3 px-4 text-right">Acciones Administrativas</th>
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
                          title="Eliminar usuario del sistema (solo si no posee historial)"
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

      {/* Modal Crear Usuario */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-neutral-200 text-left">
            <h3 className="text-base font-bold text-neutral-900 mb-1">Registrar Nuevo Usuario</h3>
            <p className="text-xs text-neutral-500 mb-4">Crea una cuenta administrativa de Cliente o Proveedor.</p>

            <form onSubmit={handleCreateSubmit} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Nombre</label>
                  <input
                    type="text"
                    required
                    value={createForm.nombre}
                    onChange={(e) => setCreateForm({ ...createForm, nombre: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Apellido</label>
                  <input
                    type="text"
                    required
                    value={createForm.apellido}
                    onChange={(e) => setCreateForm({ ...createForm, apellido: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Correo Electrónico</label>
                  <input
                    type="email"
                    required
                    value={createForm.email}
                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Contraseña</label>
                  <input
                    type="password"
                    required
                    value={createForm.password}
                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Rol de Acceso</label>
                  <select
                    value={createForm.rol}
                    onChange={(e) => setCreateForm({ ...createForm, rol: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  >
                    <option value="CLIENTE">CLIENTE</option>
                    <option value="PROPIETARIO">PROPIETARIO / PROVEEDOR</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Teléfono</label>
                  <input
                    type="text"
                    value={createForm.telefono}
                    onChange={(e) => setCreateForm({ ...createForm, telefono: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              {createForm.rol === 'PROPIETARIO' && (
                <div className="grid grid-cols-2 gap-3 p-3 bg-blue-50/60 rounded-xl border border-blue-100">
                  <div>
                    <label className="block text-[11px] font-bold text-blue-900 mb-1">Razón Social</label>
                    <input
                      type="text"
                      value={createForm.razonSocial}
                      onChange={(e) => setCreateForm({ ...createForm, razonSocial: e.target.value })}
                      className="w-full px-3 py-1.5 text-xs border border-blue-200 rounded-lg bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-blue-900 mb-1">NIT</label>
                    <input
                      type="text"
                      placeholder="900123456-1"
                      value={createForm.nit}
                      onChange={(e) => setCreateForm({ ...createForm, nit: e.target.value })}
                      className="w-full px-3 py-1.5 text-xs border border-blue-200 rounded-lg bg-white"
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-700 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg"
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
            <h3 className="text-base font-bold text-neutral-900 mb-1">Editar Usuario</h3>
            <p className="text-xs text-neutral-500 mb-4">{editingUser.email}</p>

            <form onSubmit={handleEditSubmit} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Nombre</label>
                  <input
                    type="text"
                    required
                    value={editForm.nombre}
                    onChange={(e) => setEditForm({ ...editForm, nombre: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-700 mb-1">Apellido</label>
                  <input
                    type="text"
                    required
                    value={editForm.apellido}
                    onChange={(e) => setEditForm({ ...editForm, apellido: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-neutral-700 mb-1">Teléfono</label>
                <input
                  type="text"
                  value={editForm.telefono}
                  onChange={(e) => setEditForm({ ...editForm, telefono: e.target.value })}
                  className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-neutral-700 mb-1">Dirección</label>
                <input
                  type="text"
                  value={editForm.direccion}
                  onChange={(e) => setEditForm({ ...editForm, direccion: e.target.value })}
                  className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                />
              </div>

              {editingUser.rol === 'PROPIETARIO' && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-bold text-neutral-700 mb-1">Razón Social</label>
                    <input
                      type="text"
                      value={editForm.razonSocial}
                      onChange={(e) => setEditForm({ ...editForm, razonSocial: e.target.value })}
                      className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-neutral-700 mb-1">NIT</label>
                    <input
                      type="text"
                      value={editForm.nit}
                      onChange={(e) => setEditForm({ ...editForm, nit: e.target.value })}
                      className="w-full px-3 py-1.5 text-xs border border-neutral-300 rounded-lg"
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setEditingUser(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-700 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg"
                >
                  {submitting ? 'Guardando...' : 'Actualizar'}
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
