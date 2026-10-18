import React, { useState, useEffect } from 'react';
import { DashboardMetrics, User } from '../types';
import { api } from '../services/api';
import { DollarSign, Users, Wrench, Calendar, TrendingUp, CheckCircle, ShieldCheck, UserCheck, UserX } from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

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
      loadData();
    } catch (e: any) {
      alert(e.message || 'Error al modificar estado del usuario');
    }
  };

  if (loading || !metrics) {
    return <div className="p-12 text-center text-sm text-neutral-500">Cargando métricas y analítica del sistema...</div>;
  }

  const { resumen, disponibilidadInventario, masAlquiladas, clientesFrecuentes, ingresosMensuales } = metrics;
  const maxIngreso = Math.max(...ingresosMensuales.map(m => m.ingresos), 1);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="border-b border-neutral-200 pb-6 mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold tracking-wider uppercase text-amber-600 block mb-1">
            Supervisión y Control Ejecutivo
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Panel de Administración Global
          </h1>
        </div>
        <div className="flex items-center gap-2 text-xs font-mono bg-neutral-100 text-neutral-700 px-3 py-1.5 rounded-lg border border-neutral-200">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          <span>PostgreSQL 16 · Flyway Migrado · Spring Security 6 JWT</span>
        </div>
      </div>

      {/* KPI Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 text-xs font-semibold mb-2">
            <span>Ingresos Totales (COP)</span>
            <DollarSign className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            ${resumen.totalIngresos.toLocaleString('es-CO')}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {resumen.totalFacturas} facturas emitidas con IVA 19%
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 text-xs font-semibold mb-2">
            <span>Reservas Activas / Totales</span>
            <Calendar className="w-4 h-4 text-amber-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            {resumen.reservasActivas} <span className="text-sm font-normal text-neutral-500">/ {resumen.totalReservas}</span>
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {resumen.reembolsosPendientes} solicitudes reembolso pendientes
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 text-xs font-semibold mb-2">
            <span>Disponibilidad Inventario</span>
            <Wrench className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 font-mono tabular-nums">
            {disponibilidadInventario.disponibles} <span className="text-sm font-normal text-neutral-500">libres</span>
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {disponibilidadInventario.alquiladas} en obra · {disponibilidadInventario.enMantenimiento} mant.
          </span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 text-xs font-semibold mb-2">
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
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-10">
        {/* Monthly Revenue Bar Chart */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-amber-600" />
                <span>Evolución de Ingresos Mensuales (COP)</span>
              </h3>
              <span className="text-xs text-neutral-500 font-mono">Año 2026</span>
            </div>

            <div className="space-y-3 pt-2">
              {ingresosMensuales.map((item) => {
                const percentage = Math.round((item.ingresos / maxIngreso) * 100);
                return (
                  <div key={item.mes} className="space-y-1">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-neutral-700 w-24">{item.mes}</span>
                      <span className="font-mono text-neutral-900 tabular-nums font-bold">
                        ${item.ingresos.toLocaleString('es-CO')} COP
                      </span>
                    </div>
                    <div className="h-3 w-full bg-neutral-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-amber-500 rounded-full transition-all duration-500"
                        style={{ width: `${percentage}%` }}
                      ></div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          <p className="text-[11px] text-neutral-400 mt-6 pt-3 border-t border-neutral-100">
            Ingresos generados por comisiones y tarifas netas liquidadas a través de la pasarela simulada.
          </p>
        </div>

        {/* Most Rented Equipment */}
        <div className="bg-white p-6 rounded-xl border border-neutral-200 shadow-xs">
          <h3 className="text-sm font-bold text-neutral-900 mb-4 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-amber-600" />
            <span>Equipos con Mayor Frecuencia de Alquiler</span>
          </h3>

          <div className="space-y-3.5">
            {masAlquiladas.map((item, idx) => (
              <div key={item.id} className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 flex items-center justify-between text-xs">
                <div className="flex items-center gap-3">
                  <span className="font-mono text-sm font-extrabold text-amber-600 w-5">
                    #{idx + 1}
                  </span>
                  <div>
                    <h4 className="font-bold text-neutral-900 truncate max-w-xs">{item.nombre}</h4>
                    <span className="text-[11px] text-neutral-500 font-mono">
                      Tarifa: ${item.precioPorDia.toLocaleString('es-CO')} COP / día
                    </span>
                  </div>
                </div>
                <div className="text-right">
                  <span className="px-2.5 py-1 rounded-md bg-amber-100 text-amber-900 font-bold font-mono tabular-nums text-xs">
                    {item.vecesAlquilada} contratos
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* User Management Table */}
      <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
          <h3 className="text-sm font-bold text-neutral-900">
            Gestión de Cuentas de Usuario (Clientes, Propietarios y Administradores)
          </h3>
          <span className="text-xs text-neutral-500">Total: {users.length} usuarios</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                <th className="py-3 px-4">Usuario</th>
                <th className="py-3 px-4">Documento / NIT</th>
                <th className="py-3 px-4">Rol</th>
                <th className="py-3 px-4">Contacto</th>
                <th className="py-3 px-4">Estado</th>
                <th className="py-3 px-4 text-right">Acción</th>
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
                    <div>{u.telefono}</div>
                    <div className="text-[10px] text-neutral-400 truncate max-w-[150px]">{u.direccion}</div>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                      u.activo ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                    }`}>
                      {u.activo ? 'Activo' : 'Suspendido'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    {u.rol !== 'ADMIN' && (
                      <button
                        onClick={() => handleToggleUser(u.id)}
                        className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-colors cursor-pointer inline-flex items-center gap-1 ${
                          u.activo
                            ? 'text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200'
                            : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200'
                        }`}
                      >
                        {u.activo ? (
                          <>
                            <UserX className="w-3 h-3" />
                            <span>Desactivar</span>
                          </>
                        ) : (
                          <>
                            <UserCheck className="w-3 h-3" />
                            <span>Reactivar</span>
                          </>
                        )}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
