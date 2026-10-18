import React, { useState, useEffect } from 'react';
import { Tool, Reservation, Category, RefundRequest } from '../types';
import { api } from '../services/api';
import { Plus, Check, X, Truck, Wrench, AlertTriangle, ShieldAlert, DollarSign } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import defaultToolImage from '../assets/images/heavy_excavator_construction_1792091729475.jpg';

export const OwnerDashboard: React.FC = () => {
  const { user } = useAuth();
  const [tools, setTools] = useState<Tool[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [refunds, setRefunds] = useState<RefundRequest[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal crear/editar equipo
  const [showToolModal, setShowToolModal] = useState(false);
  const [editingTool, setEditingTool] = useState<Tool | null>(null);
  const [formData, setFormData] = useState<{
    nombre: string;
    descripcion: string;
    categoriaId: string | number;
    precioPorDia: number;
    depositoGarantia: number;
    ubicacion: string;
    marca: string;
    modelo: string;
    especificacionesTecnicas: string;
    imagenUrl: string;
  }>({
    nombre: '',
    descripcion: '',
    categoriaId: 1,
    precioPorDia: 100000,
    depositoGarantia: 300000,
    ubicacion: 'Bogotá - Zona Industrial',
    marca: '',
    modelo: '',
    especificacionesTecnicas: '',
    imagenUrl: defaultToolImage
  });

  // Modal registrar devolución con daño
  const [returnResv, setReturnResv] = useState<Reservation | null>(null);
  const [hasDamage, setHasDamage] = useState(false);
  const [damageDescription, setDamageDescription] = useState('');
  const [repairCost, setRepairCost] = useState(0);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const [catsData, toolsData, resvsData, refundsData] = await Promise.all([
        api.getCategories(),
        api.getTools({ propietarioId: user?.rol === 'ADMIN' ? undefined : user?.id }),
        api.getReservations(),
        api.getRefunds()
      ]);
      setCategories(catsData);
      setTools(toolsData);
      setReservations(resvsData);
      setRefunds(refundsData);
    } catch (e) {
      console.error('Error cargando panel propietario:', e);
    } finally {
      setLoading(false);
    }
  }

  const handleOpenCreate = () => {
    setEditingTool(null);
    setFormData({
      nombre: '',
      descripcion: '',
      categoriaId: categories[0]?.id || 1,
      precioPorDia: 120000,
      depositoGarantia: 400000,
      ubicacion: 'Bogotá - Zona Industrial Fontibón',
      marca: 'Caterpillar',
      modelo: '2026 Pro',
      especificacionesTecnicas: 'Equipo certificado con mantenimiento al día.',
      imagenUrl: defaultToolImage
    });
    setShowToolModal(true);
  };

  const handleOpenEdit = (t: Tool) => {
    setEditingTool(t);
    setFormData({
      nombre: t.nombre,
      descripcion: t.descripcion,
      categoriaId: t.categoriaId,
      precioPorDia: t.precioPorDia,
      depositoGarantia: t.depositoGarantia,
      ubicacion: t.ubicacion,
      marca: t.marca,
      modelo: t.modelo,
      especificacionesTecnicas: t.especificacionesTecnicas,
      imagenUrl: t.imagenUrl
    });
    setShowToolModal(true);
  };

  const handleSaveTool = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingTool) {
        await api.updateTool(editingTool.id, formData);
      } else {
        await api.createTool(formData);
      }
      setShowToolModal(false);
      loadData();
    } catch (err: any) {
      alert(err.message || 'Error al guardar la herramienta');
    }
  };

  const handleDeleteTool = async (id: string | number) => {
    if (confirm('¿Está seguro de eliminar o archivar este equipo?')) {
      try {
        await api.deleteTool(id);
        loadData();
      } catch (err: any) {
        alert(err.message || 'Error al eliminar');
      }
    }
  };

  const handleAccept = async (id: string | number) => {
    try {
      await api.acceptReservation(id);
      loadData();
    } catch (err: any) {
      alert(err.message || 'Error al aceptar reserva');
    }
  };

  const handleReject = async (id: string | number) => {
    const motivo = prompt('Motivo del rechazo de la solicitud:');
    if (motivo !== null) {
      try {
        await api.rejectReservation(id, motivo);
        loadData();
      } catch (err: any) {
        alert(err.message || 'Error al rechazar reserva');
      }
    }
  };

  const handleDispatch = async (id: string | number) => {
    if (confirm('¿Confirmar entrega y despacho físico de la maquinaria hacia la obra del cliente?')) {
      try {
        await api.dispatchReservation(id);
        loadData();
      } catch (err: any) {
        alert(err.message || 'Error al despachar equipo');
      }
    }
  };

  const handleConfirmReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!returnResv) return;

    try {
      await api.returnReservation(returnResv.id, {
        presentaDanos: hasDamage,
        descripcionDano: damageDescription,
        costoReparacion: repairCost
      });
      alert('Devolución registrada exitosamente.');
      setReturnResv(null);
      loadData();
    } catch (err: any) {
      alert(err.message || 'Error al registrar devolución');
    }
  };

  const handleResolveRefund = async (refundId: string | number, aprobar: boolean) => {
    const resolucion = prompt(`Indique motivo o resolución para ${aprobar ? 'aprobar' : 'rechazar'} el reembolso:`);
    if (resolucion !== null) {
      try {
        await api.resolveRefund(refundId, aprobar, resolucion);
        loadData();
      } catch (err: any) {
        alert(err.message || 'Error al resolver el reembolso');
      }
    }
  };

  // Pending reservations for this owner
  const pendingRequests = reservations.filter(r => r.estado === 'PENDIENTE');
  const activeDispatches = reservations.filter(r => ['PAGADA', 'ENTREGADA'].includes(r.estado));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="border-b border-neutral-200 pb-6 mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold tracking-wider uppercase text-amber-600 block mb-1">
            Gestión de Flota y Alquileres
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Panel del Propietario / Proveedor
          </h1>
        </div>

        <button
          onClick={handleOpenCreate}
          className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors flex items-center gap-1.5 shadow-xs cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Publicar Nuevo Equipo</span>
        </button>
      </div>

      {loading ? (
        <div className="p-12 text-center text-sm text-neutral-500">Cargando inventario y solicitudes...</div>
      ) : (
        <div className="space-y-10">
          {/* Section 1: Pending Rental Requests (Accept / Reject) */}
          <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
            <div className="px-6 py-4 border-b border-neutral-200 bg-amber-50/50 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-neutral-900">
                  Solicitudes de Alquiler Pendientes ({pendingRequests.length})
                </h3>
                <span className="text-xs text-neutral-500">Decida si aprueba o rechaza solicitudes de sus clientes</span>
              </div>
            </div>

            {pendingRequests.length === 0 ? (
              <div className="p-8 text-center text-xs text-neutral-500">
                No hay solicitudes pendientes de aprobación en este momento.
              </div>
            ) : (
              <div className="divide-y divide-neutral-100">
                {pendingRequests.map((r) => (
                  <div key={r.id} className="p-4 sm:px-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-neutral-50">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-bold text-sm text-neutral-900">{r.herramientaNombre}</span>
                        <span className="text-xs text-neutral-400">·</span>
                        <span className="text-xs font-mono text-neutral-600">Cliente: {r.clienteNombre}</span>
                      </div>
                      <div className="text-xs text-neutral-600 font-mono space-x-2">
                        <span>Periodo: {r.fechaInicio} al {r.fechaFin} ({r.dias} días)</span>
                        <span>·</span>
                        <strong className="text-neutral-900">${r.total.toLocaleString('es-CO')} COP</strong>
                      </div>
                      {r.notasCliente && (
                        <p className="text-xs text-neutral-500 mt-1 italic">
                          "Nota: {r.notasCliente}"
                        </p>
                      )}
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => handleAccept(r.id)}
                        className="px-3.5 py-1.5 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Aceptar</span>
                      </button>
                      <button
                        onClick={() => handleReject(r.id)}
                        className="px-3.5 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-lg border border-rose-200 transition-colors flex items-center gap-1 cursor-pointer"
                      >
                        <X className="w-3.5 h-3.5" />
                        <span>Rechazar</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section 2: Active Dispatches & Returns (Entrega y Devolución) */}
          <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
            <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-neutral-900">
                  Operaciones en Curso: Despacho y Devoluciones ({activeDispatches.length})
                </h3>
                <span className="text-xs text-neutral-500">Controle la entrega física y el retorno de la maquinaria con inspección de daños</span>
              </div>
            </div>

            {activeDispatches.length === 0 ? (
              <div className="p-8 text-center text-xs text-neutral-500">
                No hay equipos en fase de despacho o alquiler en obra actualmente.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                      <th className="py-3 px-4">Reserva</th>
                      <th className="py-3 px-4">Equipo</th>
                      <th className="py-3 px-4">Cliente</th>
                      <th className="py-3 px-4">Estado</th>
                      <th className="py-3 px-4 text-right">Acción Operativa</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {activeDispatches.map((r) => (
                      <tr key={r.id} className="hover:bg-neutral-50">
                        <td className="py-3.5 px-4 font-mono text-neutral-500">#{r.id}</td>
                        <td className="py-3.5 px-4 font-bold text-neutral-900">{r.herramientaNombre}</td>
                        <td className="py-3.5 px-4 text-neutral-700">{r.clienteNombre}</td>
                        <td className="py-3.5 px-4">
                          <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                            r.estado === 'PAGADA' ? 'bg-emerald-100 text-emerald-800' : 'bg-purple-100 text-purple-800'
                          }`}>
                            {r.estado === 'PAGADA' ? 'PAGADA (Lista para entregar)' : 'ENTREGADA (En obra)'}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          {r.estado === 'PAGADA' && (
                            <button
                              onClick={() => handleDispatch(r.id)}
                              className="px-3 py-1.5 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-lg inline-flex items-center gap-1 cursor-pointer"
                            >
                              <Truck className="w-3.5 h-3.5" />
                              <span>Registrar Entrega</span>
                            </button>
                          )}
                          {r.estado === 'ENTREGADA' && (
                            <button
                              onClick={() => {
                                setReturnResv(r);
                                setHasDamage(false);
                                setDamageDescription('');
                                setRepairCost(0);
                              }}
                              className="px-3 py-1.5 text-xs font-bold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg inline-flex items-center gap-1 cursor-pointer shadow-xs"
                            >
                              <Wrench className="w-3.5 h-3.5" />
                              <span>Confirmar Devolución</span>
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Section 3: Flota de Equipos (CRUD y Estado Operativo) */}
          <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
            <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-neutral-900">
                  Inventario de Maquinaria Registrada ({tools.length})
                </h3>
                <span className="text-xs text-neutral-500">Gestión de tarifas, publicación y estado físico</span>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                    <th className="py-3 px-4">Equipo</th>
                    <th className="py-3 px-4">Marca/Modelo</th>
                    <th className="py-3 px-4">Ubicación</th>
                    <th className="py-3 px-4 text-right">Tarifa Día</th>
                    <th className="py-3 px-4">Estado Operativo</th>
                    <th className="py-3 px-4 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {tools.map((t) => (
                    <tr key={t.id} className="hover:bg-neutral-50">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2.5">
                          <img
                            src={t.imagenUrl}
                            alt=""
                            className="w-10 h-10 object-cover rounded-md border border-neutral-200"
                          />
                          <span className="font-bold text-neutral-900 truncate max-w-xs">{t.nombre}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-neutral-600">
                        {t.marca} · {t.modelo}
                      </td>
                      <td className="py-3.5 px-4 text-neutral-600 truncate max-w-[150px]">
                        {t.ubicacion}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono tabular-nums font-bold text-neutral-900">
                        ${t.precioPorDia.toLocaleString('es-CO')}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                          t.estadoOperativo === 'DISPONIBLE' ? 'bg-emerald-100 text-emerald-800' :
                          t.estadoOperativo === 'ALQUILADA' ? 'bg-purple-100 text-purple-800' :
                          'bg-amber-100 text-amber-800'
                        }`}>
                          {t.estadoOperativo}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right space-x-2">
                        <button
                          onClick={() => handleOpenEdit(t)}
                          className="px-2.5 py-1 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-md cursor-pointer"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => handleDeleteTool(t.id)}
                          className="px-2 py-1 text-xs text-rose-600 hover:text-rose-800 cursor-pointer"
                        >
                          Eliminar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Section 4: Refund Review Requests */}
          {refunds.length > 0 && (
            <div className="bg-white rounded-xl border border-neutral-200 p-6 shadow-xs">
              <h3 className="text-sm font-bold text-neutral-900 mb-4 flex items-center gap-2">
                <DollarSign className="w-4 h-4 text-amber-600" />
                <span>Solicitudes de Reembolso Recibidas</span>
              </h3>
              <div className="space-y-3">
                {refunds.map((ref) => (
                  <div key={ref.id} className="p-4 bg-neutral-50 rounded-xl border border-neutral-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs">
                    <div>
                      <div className="flex items-center gap-2 font-mono text-neutral-600 mb-1">
                        <span className="font-bold text-neutral-900">Reembolso #{ref.id}</span>
                        <span>·</span>
                        <span>Monto: ${ref.monto.toLocaleString('es-CO')} COP</span>
                      </div>
                      <p className="text-neutral-700 italic mb-1">"{ref.motivo}"</p>
                      <span className="text-[11px] text-neutral-500">Estado actual: {ref.estado}</span>
                    </div>

                    {ref.estado === 'PENDIENTE' && (
                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          onClick={() => handleResolveRefund(ref.id, true)}
                          className="px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg cursor-pointer"
                        >
                          Aprobar Reembolso
                        </button>
                        <button
                          onClick={() => handleResolveRefund(ref.id, false)}
                          className="px-3 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg cursor-pointer"
                        >
                          Rechazar
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal Crear / Editar Equipo */}
      {showToolModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-neutral-200 animate-in fade-in duration-150">
            <h3 className="text-base font-bold text-neutral-900 mb-4">
              {editingTool ? 'Editar Maquinaria / Equipo' : 'Registrar Nuevo Equipo al Catálogo'}
            </h3>
            <form onSubmit={handleSaveTool} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Nombre Comercial *</label>
                <input
                  type="text"
                  required
                  value={formData.nombre}
                  onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                  className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Categoría *</label>
                  <select
                    value={formData.categoriaId}
                    onChange={(e) => setFormData({ ...formData, categoriaId: Number(e.target.value) })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.nombre}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Tarifa Día (COP) *</label>
                  <input
                    type="number"
                    required
                    min="1000"
                    value={formData.precioPorDia}
                    onChange={(e) => setFormData({ ...formData, precioPorDia: Number(e.target.value) })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Marca</label>
                  <input
                    type="text"
                    value={formData.marca}
                    onChange={(e) => setFormData({ ...formData, marca: e.target.value })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Modelo</label>
                  <input
                    type="text"
                    value={formData.modelo}
                    onChange={(e) => setFormData({ ...formData, modelo: e.target.value })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Ubicación / Patio de Despacho *</label>
                <input
                  type="text"
                  required
                  value={formData.ubicacion}
                  onChange={(e) => setFormData({ ...formData, ubicacion: e.target.value })}
                  className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Descripción del Equipo</label>
                <textarea
                  rows={2}
                  required
                  value={formData.descripcion}
                  onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                  className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Especificaciones Técnicas</label>
                <textarea
                  rows={2}
                  value={formData.especificacionesTecnicas}
                  onChange={(e) => setFormData({ ...formData, especificacionesTecnicas: e.target.value })}
                  className="w-full p-2 bg-white border border-neutral-300 rounded-lg font-mono text-[11px]"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-neutral-200">
                <button
                  type="button"
                  onClick={() => setShowToolModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg shadow-xs"
                >
                  Guardar Equipo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Devolución e Inspección de Daños */}
      {returnResv && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-sm font-bold text-neutral-900 mb-2">
              Inspección de Retorno de Maquinaria
            </h3>
            <p className="text-xs text-neutral-600 mb-4">
              Reserva #{returnResv.id} ({returnResv.herramientaNombre}). Verifique el estado físico y funcional del equipo.
            </p>

            <form onSubmit={handleConfirmReturn} className="space-y-3.5 text-xs">
              <label className="flex items-center gap-2.5 p-3 rounded-lg border border-neutral-200 bg-neutral-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={hasDamage}
                  onChange={(e) => setHasDamage(e.target.checked)}
                  className="rounded text-amber-600 focus:ring-amber-500 w-4 h-4"
                />
                <span className="font-semibold text-neutral-900">
                  ¿El equipo presenta averías, roturas o daños funcionales?
                </span>
              </label>

              {hasDamage && (
                <div className="space-y-3 p-3.5 bg-rose-50 border border-rose-200 rounded-xl">
                  <div className="flex items-center gap-1.5 text-rose-800 font-bold text-xs">
                    <ShieldAlert className="w-4 h-4 text-rose-600" />
                    <span>Reporte Técnico de Daños (Mantenimiento)</span>
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-rose-900 mb-1">Descripción del Daño</label>
                    <textarea
                      required
                      rows={2}
                      value={damageDescription}
                      onChange={(e) => setDamageDescription(e.target.value)}
                      placeholder="Ej: Manguera hidráulica rota, fuga en el retén..."
                      className="w-full text-xs p-2 bg-white border border-rose-300 rounded-md"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-rose-900 mb-1">Costo Estimado de Reparación (COP)</label>
                    <input
                      type="number"
                      required
                      min="0"
                      value={repairCost}
                      onChange={(e) => setRepairCost(Number(e.target.value))}
                      className="w-full text-xs p-2 bg-white border border-rose-300 rounded-md font-mono"
                    />
                  </div>
                  <p className="text-[10px] text-rose-700 italic">
                    Al confirmar, el estado operativo del equipo pasará a "EN_MANTENIMIENTO" hasta su reparación.
                  </p>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setReturnResv(null)}
                  className="px-3 py-1.5 text-xs text-neutral-600 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg shadow-xs"
                >
                  Confirmar Retorno
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
