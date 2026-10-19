import React, { useState, useEffect } from 'react';
import { Tool, Reservation, Category, RefundRequest } from '../types';
import { api } from '../services/api';
import { Plus, Check, X, Truck, Wrench, AlertTriangle, ShieldAlert, DollarSign, CheckCircle2, RotateCcw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { resolveToolImage, defaultExcavator } from '../utils/imageResolver';

export const OwnerDashboard: React.FC = () => {
  const { user } = useAuth();
  const { toast } = useToast();
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
    cantidadTotal?: number;
    ubicacion: string;
    marca: string;
    modelo: string;
    especificacionesTecnicas: string;
    imagenUrl: string;
  }>({
    nombre: '',
    descripcion: '',
    categoriaId: '',
    precioPorDia: 100000,
    depositoGarantia: 300000,
    cantidadTotal: 1,
    ubicacion: 'Bogotá - Zona Industrial',
    marca: '',
    modelo: '',
    especificacionesTecnicas: '',
    imagenUrl: ''
  });

  // Modal registrar devolución con daño
  const [returnResv, setReturnResv] = useState<Reservation | null>(null);
  const [hasDamage, setHasDamage] = useState(false);
  const [damageDescription, setDamageDescription] = useState('');
  const [repairCost, setRepairCost] = useState(0);
  const [returnAccepted, setReturnAccepted] = useState(true);
  const [ownerObservation, setOwnerObservation] = useState("");
  const [submittingReturn, setSubmittingReturn] = useState(false);
  // Estados para modales de confirmación interactiva
  const [dispatchModalResv, setDispatchModalResv] = useState<Reservation | null>(null);
  const [rejectModalResv, setRejectModalResv] = useState<Reservation | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [deleteToolId, setDeleteToolId] = useState<string | number | null>(null);
  const [resolveRefundData, setResolveRefundData] = useState<{ id: string | number; aprobar: boolean } | null>(null);
  const [refundResolution, setRefundResolution] = useState('');


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
      categoriaId: categories[0]?.id ? String(categories[0].id) : '',
      precioPorDia: 120000,
      depositoGarantia: 400000,
      cantidadTotal: 1,
      ubicacion: 'Bogotá - Zona Industrial Fontibón',
      marca: 'Caterpillar',
      modelo: '2026 Pro',
      especificacionesTecnicas: 'Equipo certificado con mantenimiento al día.',
      imagenUrl: ''
    });
    setShowToolModal(true);
  };

  const handleOpenEdit = (t: Tool) => {
    setEditingTool(t);
    setFormData({
      nombre: t.nombre,
      descripcion: t.descripcion,
      categoriaId: String(t.categoriaId),
      precioPorDia: t.precioPorDia,
      depositoGarantia: t.depositoGarantia,
      cantidadTotal: t.cantidadTotal || 1,
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
    if (!formData.categoriaId) {
      toast.warning('Debe seleccionar una categoría para el equipo.');
      return;
    }
    if (formData.precioPorDia <= 0) {
      toast.warning('La tarifa diaria debe ser un valor positivo mayor a 0.');
      return;
    }
    if ((formData.cantidadTotal || 1) <= 0) {
      toast.warning('La cantidad total de unidades debe ser mayor a 0.');
      return;
    }
    try {
      if (editingTool) {
        await api.updateTool(editingTool.id, formData);
      } else {
        await api.createTool(formData);
      }
      setShowToolModal(false);
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al guardar la herramienta', 'Error al Guardar');
    }
  };

  const handleDeleteTool = async (id: string | number) => {
    try {
      await api.deleteTool(id);
      toast.success('Equipo eliminado o archivado con éxito');
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al eliminar', 'Error');
    }
  };

  const handleAccept = async (id: string | number) => {
    try {
      await api.acceptReservation(id);
      toast.success('Reserva aceptada con éxito. El cliente ya puede proceder al pago.', 'Reserva Aceptada');
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al aceptar reserva', 'Error');
    }
  };

  const handleReject = async (id: string | number, motivo?: string) => {
    try {
      await api.rejectReservation(id, motivo || 'Solicitud rechazada por el propietario');
      toast.info('La reserva fue rechazada.', 'Reserva Rechazada');
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al rechazar reserva', 'Error');
    }
  };

  const handleDispatch = async (id: string | number) => {
    try {
      await api.dispatchReservation(id);
      toast.success('Equipo despachado y entregado al cliente.', 'Despacho Confirmado');
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al despachar equipo', 'Error');
    }
  };

  const handleConfirmReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!returnResv) return;
    if (hasDamage && !damageDescription.trim()) {
      toast.warning('Debe especificar la descripción de las averías o daños observados.');
      return;
    }

    try {
      setSubmittingReturn(true);
      await api.returnReservation(returnResv.id, {
        aceptada: returnAccepted,
        presentaDanos: hasDamage,
        descripcionDano: damageDescription,
        costoReparacion: hasDamage ? repairCost : 0,
        observaciones: ownerObservation
      });
      if (returnAccepted) {
        toast.success(
          hasDamage
            ? `Devolución aceptada con cargo de reparación por $${repairCost.toLocaleString('es-CO')} COP.`
            : 'Devolución confirmada en buen estado. Stock restablecido en catálogo.',
          'Inspección Finalizada'
        );
      } else {
        toast.error('Devolución observada y rechazada temporalmente por averías graves.', 'Retorno Rechazado');
      }
      setReturnResv(null);
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al registrar devolución', 'Error');
    } finally {
      setSubmittingReturn(false);
    }
  };

  const handleResolveRefund = async (refundId: string | number, aprobar: boolean, resolucion?: string) => {
    try {
      await api.resolveRefund(refundId, aprobar, resolucion || (aprobar ? 'Aprobado' : 'Rechazado'));
      toast.success(`Reembolso ${aprobar ? 'aprobado' : 'rechazado'} correctamente.`);
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al resolver el reembolso', 'Error');
    }
  };

  // Filtros de reservas del propietario
  const pendingRequests = reservations.filter(r => r.estado === 'PENDIENTE');
  const pendingReturns = reservations.filter(r => ['EN_CURSO', 'ENTREGADA'].includes(r.estado) && (r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION'));
  const activeDispatches = reservations.filter(r => ['CONFIRMADA', 'PAGADA', 'EN_CURSO', 'ENTREGADA'].includes(r.estado) && !(r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION'));
  const completedHistory = reservations.filter(r => ['FINALIZADA', 'DEVUELTA', 'CANCELADA', 'EXPIRADA', 'RECHAZADA'].includes(r.estado));

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
                        onClick={() => { setRejectModalResv(r); setRejectReason(''); }}
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

                    {/* Sección Especial: Devoluciones Notificadas por Cliente (Pendientes de Inspección) */}
          {pendingReturns.length > 0 && (
            <div className="bg-amber-50/60 rounded-xl border border-amber-300 overflow-hidden shadow-xs">
              <div className="px-6 py-4 bg-amber-100/70 border-b border-amber-200 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-amber-950 flex items-center gap-1.5">
                    <RotateCcw className="w-4 h-4 text-amber-700" />
                    <span>Devoluciones Notificadas por Clientes ({pendingReturns.length})</span>
                  </h3>
                  <span className="text-xs text-amber-900">
                    Los clientes han notificado el fin de obra y entrega. Realice la inspección física para validar o cobrar daños.
                  </span>
                </div>
              </div>

              <div className="divide-y divide-amber-200/60">
                {pendingReturns.map((r) => (
                  <div key={r.id} className="p-4 sm:px-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white/70 hover:bg-white transition-colors">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-neutral-900">{r.herramientaNombre}</span>
                        <span className="text-xs text-neutral-400">·</span>
                        <span className="text-xs font-mono text-neutral-700">Cliente: {r.clienteNombre}</span>
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-200 text-amber-900">
                          POR INSPECCIONAR
                        </span>
                      </div>
                      <div className="text-xs text-neutral-600 font-mono space-x-2">
                        <span>Periodo: {r.fechaInicio} al {r.fechaFin}</span>
                        <span>·</span>
                        <span>Total Renta: ${r.total.toLocaleString('es-CO')} COP</span>
                      </div>
                      <div className="bg-amber-50 p-2.5 rounded-lg border border-amber-200 text-xs text-amber-900 mt-1 max-w-xl">
                        <span className="font-semibold block text-[11px] text-amber-950">Informe del Cliente:</span>
                        <p className="italic font-mono text-[11px] mt-0.5">
                          {r.notaDevolucionCliente ? `"${r.notaDevolucionCliente}"` : 'El cliente no reportó observaciones de avería.'}
                        </p>
                      </div>
                    </div>

                    <div className="shrink-0">
                      <button
                        onClick={() => {
                          setReturnResv(r);
                          setReturnAccepted(true);
                          setHasDamage(false);
                          setDamageDescription('');
                          setRepairCost(0);
                          setOwnerObservation('');
                        }}
                        className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg shadow-xs inline-flex items-center gap-1.5 cursor-pointer transition-colors"
                      >
                        <Wrench className="w-3.5 h-3.5" />
                        <span>Inspeccionar y Validar Retorno</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

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
                            (r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA')
                              ? 'bg-purple-100 text-purple-800'
                              : r.pagada
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}>
                            {(r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA')
                              ? 'EN CURSO (En obra)'
                              : r.pagada
                              ? 'PAGADA (Lista para entrega)'
                              : 'CONFIRMADA (Pendiente de pago)'}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          {(r.estado === 'CONFIRMADA' || r.estado === 'PAGADA') && r.pagada && (
                            <button
                              onClick={() => setDispatchModalResv(r)}
                              className="px-3 py-1.5 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-lg inline-flex items-center gap-1 cursor-pointer"
                            >
                              <Truck className="w-3.5 h-3.5" />
                              <span>Registrar Entrega</span>
                            </button>
                          )}
                          {(r.estado === 'CONFIRMADA' || r.estado === 'PAGADA') && !r.pagada && (
                            <span className="text-[11px] text-amber-600 font-medium italic">
                              Esperando pago del cliente
                            </span>
                          )}
                          {(r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA') && (
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
                            src={resolveToolImage(t)}
                            alt={t.nombre}
                            className="w-10 h-10 object-cover rounded-md border border-neutral-200"
                            onError={(e) => { (e.target as HTMLImageElement).src = defaultExcavator; }}
                          />
                          <span className="font-bold text-neutral-900 truncate max-w-xs">{t.nombre}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-neutral-600">
                        {t.marca} · {t.modelo}
                      </td>
                      <td className="py-3.5 px-4 font-mono">
                        <span className="font-bold text-emerald-700">{t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1)}</span>
                        <span className="text-neutral-400"> / {t.cantidadTotal || 1}</span>
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
                          onClick={() => setDeleteToolId(t.id)}
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

                    {/* Section 5: Historial Completo de Reservas y Devoluciones Finalizadas */}
          <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
            <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-neutral-900">
                  Historial de Alquileres y Devoluciones Completadas ({completedHistory.length})
                </h3>
                <span className="text-xs text-neutral-500">
                  Registro permanente e histórico de todas las operaciones de renta finalizadas
                </span>
              </div>
            </div>

            {completedHistory.length === 0 ? (
              <div className="p-8 text-center text-xs text-neutral-500">
                Aún no hay registros de reservas finalizadas o devueltas en el histórico.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 uppercase text-[11px]">
                      <th className="py-3 px-4">Reserva</th>
                      <th className="py-3 px-4">Equipo</th>
                      <th className="py-3 px-4">Cliente</th>
                      <th className="py-3 px-4">Fechas</th>
                      <th className="py-3 px-4 text-right">Total Pagado</th>
                      <th className="py-3 px-4">Estado & Inspección</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {completedHistory.map((r) => (
                      <tr key={r.id} className="hover:bg-neutral-50">
                        <td className="py-3.5 px-4 font-mono text-neutral-500">#{r.id}</td>
                        <td className="py-3.5 px-4 font-bold text-neutral-900">{r.herramientaNombre}</td>
                        <td className="py-3.5 px-4 text-neutral-700">{r.clienteNombre}</td>
                        <td className="py-3.5 px-4 font-mono text-neutral-600">{r.fechaInicio} al {r.fechaFin}</td>
                        <td className="py-3.5 px-4 text-right font-mono font-bold text-neutral-900">${r.total.toLocaleString('es-CO')}</td>
                        <td className="py-3.5 px-4">
                          {r.costoReparacion && r.costoReparacion > 0 ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-[11px] font-semibold bg-amber-100 text-amber-900" title={r.descripcionDano}>
                              <Wrench className="w-3 h-3 text-amber-600" />
                              <span>Devuelta con Daños (+${r.costoReparacion.toLocaleString('es-CO')})</span>
                            </span>
                          ) : r.estadoDevolucion === 'RECHAZADA_POR_DANOS' ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-[11px] font-semibold bg-rose-100 text-rose-800">
                              <ShieldAlert className="w-3 h-3 text-rose-600" />
                              <span>Retorno Rechazado por Daños Graves</span>
                            </span>
                          ) : r.estado === 'FINALIZADA' || r.estado === 'DEVUELTA' ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded text-[11px] font-semibold bg-emerald-100 text-emerald-800">
                              <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                              <span>Devuelta en Buen Estado</span>
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-neutral-100 text-neutral-700">
                              {r.estado}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
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
                          onClick={() => { setResolveRefundData({ id: ref.id, aprobar: true }); setRefundResolution(''); }}
                          className="px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg cursor-pointer"
                        >
                          Aprobar Reembolso
                        </button>
                        <button
                          onClick={() => { setResolveRefundData({ id: ref.id, aprobar: false }); setRefundResolution(''); }}
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

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Categoría *</label>
                  <select
                    value={formData.categoriaId}
                    onChange={(e) => setFormData({ ...formData, categoriaId: e.target.value })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg"
                  >
                    {categories.length === 0 ? (
                      <option value="">Cargando categorías...</option>
                    ) : (
                      categories.map((c) => (
                        <option key={c.id} value={c.id}>{c.nombre}</option>
                      ))
                    )}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Tarifa Día (COP) *</label>
                  <input
                    type="number"
                    required
                    min="1000"
                    value={formData.precioPorDia}
                    onChange={(e) => setFormData({ ...formData, precioPorDia: Math.max(0, Number(e.target.value)) })}
                    className="w-full p-2 bg-white border border-neutral-300 rounded-lg font-mono"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Stock Total *</label>
                  <input
                    type="number"
                    required
                    min="1"
                    value={formData.cantidadTotal || 1}
                    onChange={(e) => setFormData({ ...formData, cantidadTotal: Math.max(1, parseInt(e.target.value, 10) || 1) })}
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
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
            <h3 className="text-base font-bold text-neutral-900 mb-1">
              Validación e Inspección Técnica de Retorno
            </h3>
            <p className="text-xs text-neutral-600 mb-4">
              Reserva #{returnResv.id} · Equipo: <strong>{returnResv.herramientaNombre}</strong> · Cliente: <strong>{returnResv.clienteNombre}</strong>
            </p>

            {/* Informe entregado por el cliente */}
            <div className="p-3.5 bg-amber-50/80 rounded-xl border border-amber-200 mb-4 text-xs">
              <span className="font-bold text-amber-950 block mb-1">
                Informe entregado por el cliente:
              </span>
              <p className="italic font-mono text-[11px] text-amber-900">
                {returnResv.notaDevolucionCliente ? `"${returnResv.notaDevolucionCliente}"` : 'El cliente no reportó observaciones de avería.'}
              </p>
            </div>

            <form onSubmit={handleConfirmReturn} className="space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-neutral-800 mb-1.5">
                  Decisión de Recepción de la Maquinaria:
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => { setReturnAccepted(true); setHasDamage(false); }}
                    className={`p-3 rounded-xl border text-left transition-colors cursor-pointer ${
                      returnAccepted && !hasDamage
                        ? 'border-emerald-500 bg-emerald-50 text-emerald-950 shadow-xs'
                        : 'border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-50'
                    }`}
                  >
                    <span className="font-bold block text-xs text-emerald-800">Aceptar en Buen Estado</span>
                    <span className="text-[10px] block text-neutral-500 mt-0.5">Sin averías · Restablece stock</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => { setHasDamage(true); }}
                    className={`p-3 rounded-xl border text-left transition-colors cursor-pointer ${
                      hasDamage
                        ? 'border-amber-500 bg-amber-50 text-amber-950 shadow-xs'
                        : 'border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-50'
                    }`}
                  >
                    <span className="font-bold block text-xs text-amber-800">Reportar Averías / Daños</span>
                    <span className="text-[10px] block text-neutral-500 mt-0.5">Cobro de reparaciones</span>
                  </button>
                </div>
              </div>

              {hasDamage && (
                <div className="space-y-3 p-4 bg-rose-50 border border-rose-200 rounded-xl">
                  <div className="flex items-center gap-1.5 text-rose-900 font-bold text-xs">
                    <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0" />
                    <span>Peritaje Técnico de Daños y Cobro de Reparaciones</span>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-rose-900 mb-1">
                      Descripción de Daños Observados *
                    </label>
                    <textarea
                      required
                      rows={2}
                      value={damageDescription}
                      onChange={(e) => setDamageDescription(e.target.value)}
                      placeholder="Ej: Fuga de aceite hidráulico y fisura en el percutor..."
                      className="w-full text-xs p-2 bg-white border border-rose-300 rounded-md focus:outline-hidden focus:ring-1 focus:ring-rose-500"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-rose-900 mb-1">
                      Costo a Cobrar por Reparaciones (COP) *
                    </label>
                    <input
                      type="number"
                      required
                      min="0"
                      step="5000"
                      value={repairCost}
                      onChange={(e) => setRepairCost(Number(e.target.value))}
                      className="w-full text-xs p-2 bg-white border border-rose-300 rounded-md font-mono"
                    />
                  </div>

                  <div className="pt-2 border-t border-rose-200/60">
                    <label className="block text-[11px] font-semibold text-rose-900 mb-1.5">
                      Acción Operativa ante el Daño:
                    </label>
                    <div className="flex gap-4">
                      <label className="flex items-center gap-1.5 cursor-pointer text-[11px] font-medium text-rose-900">
                        <input
                          type="radio"
                          name="returnResolution"
                          checked={returnAccepted}
                          onChange={() => setReturnAccepted(true)}
                          className="text-amber-600"
                        />
                        <span>Aceptar retorno reteniendo costo de reparación</span>
                      </label>
                      <label className="flex items-center gap-1.5 cursor-pointer text-[11px] font-medium text-rose-900">
                        <input
                          type="radio"
                          name="returnResolution"
                          checked={!returnAccepted}
                          onChange={() => setReturnAccepted(false)}
                          className="text-rose-600"
                        />
                        <span>Rechazar retorno temporalmente</span>
                      </label>
                    </div>
                  </div>
                </div>
              )}

              <div>
                <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                  Observaciones Técnicas Adicionales del Propietario (Opcional)
                </label>
                <input
                  type="text"
                  value={ownerObservation}
                  onChange={(e) => setOwnerObservation(e.target.value)}
                  placeholder="Ej: Recibido por técnico de turno en bodega..."
                  className="w-full text-xs p-2 bg-white border border-neutral-300 rounded-md"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setReturnResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingReturn}
                  className="px-5 py-2 text-xs font-bold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg shadow-xs transition-colors cursor-pointer"
                >
                  {submittingReturn ? 'Guardando...' : 'Guardar Resolución de Devolución'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Confirmación de Despacho y Entrega a Obra */}
      {dispatchModalResv && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-amber-700 shrink-0">
                <Truck className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <span className="text-[11px] font-semibold uppercase tracking-wider text-amber-600 block">
                  Logística y Despacho
                </span>
                <h3 className="text-base font-bold text-neutral-900 leading-tight">
                  Confirmar Entrega en Obra
                </h3>
              </div>
            </div>

            <p className="text-xs text-neutral-600 mb-4 leading-relaxed">
              ¿Desea confirmar el despacho y la entrega física de la maquinaria hacia la obra del cliente?
            </p>

            <div className="bg-neutral-50 rounded-xl p-3.5 border border-neutral-200 text-xs space-y-2 mb-5">
              <div className="flex justify-between">
                <span className="text-neutral-500">Equipo:</span>
                <span className="font-bold text-neutral-900 truncate max-w-[200px]">{dispatchModalResv.herramientaNombre}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Periodo:</span>
                <span className="font-mono text-neutral-700">{dispatchModalResv.fechaInicio} al {dispatchModalResv.fechaFin}</span>
              </div>
              <div className="flex justify-between pt-1 border-t border-neutral-200">
                <span className="text-neutral-500">Nuevo Estado:</span>
                <span className="font-bold text-purple-700 font-mono">EN CURSO (En obra)</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2.5">
              <button
                type="button"
                onClick={() => setDispatchModalResv(null)}
                className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={async () => {
                  const id = dispatchModalResv.id;
                  setDispatchModalResv(null);
                  try {
                    await api.dispatchReservation(id);
                    toast.success('Equipo despachado y entregado al cliente.', 'Despacho Confirmado');
                    loadData();
                  } catch (err: any) {
                    toast.error(err.message || 'Error al despachar equipo', 'Error');
                  }
                }}
                className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                <Truck className="w-3.5 h-3.5" />
                <span>Confirmar Entrega</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Rechazo de Solicitud */}
      {rejectModalResv && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-sm font-bold text-neutral-900 mb-1">
              Rechazar Solicitud de Reserva #{rejectModalResv.id}
            </h3>
            <p className="text-xs text-neutral-600 mb-4">
              Indique el motivo por el cual no es posible aceptar esta solicitud de alquiler:
            </p>
            <form onSubmit={async (e) => {
              e.preventDefault();
              const id = rejectModalResv.id;
              const mot = rejectReason.trim() || 'No disponible en las fechas solicitadas';
              setRejectModalResv(null);
              setRejectReason('');
              try {
                await api.rejectReservation(id, mot);
                toast.info('La reserva fue rechazada.', 'Reserva Rechazada');
                loadData();
              } catch (err: any) {
                toast.error(err.message || 'Error al rechazar reserva', 'Error');
              }
            }}>
              <textarea
                required
                rows={3}
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Ej: Maquinaria asignada a mantenimiento preventivo prioritario..."
                className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg mb-4 focus:ring-1 focus:ring-amber-500 focus:outline-none"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => { setRejectModalResv(null); setRejectReason(''); }}
                  className="px-3.5 py-1.5 text-xs text-neutral-600 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg shadow-xs"
                >
                  Confirmar Rechazo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Eliminar Herramienta */}
      {deleteToolId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-neutral-200 text-center">
            <div className="w-10 h-10 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto mb-3">
              <AlertTriangle className="w-5 h-5 text-rose-600" />
            </div>
            <h3 className="text-sm font-bold text-neutral-900 mb-1">
              ¿Eliminar o Archivar Equipo?
            </h3>
            <p className="text-xs text-neutral-600 mb-5">
              Esta acción retirará la herramienta del catálogo activo de la plataforma.
            </p>
            <div className="flex justify-center gap-2">
              <button
                type="button"
                onClick={() => setDeleteToolId(null)}
                className="px-3.5 py-1.5 text-xs text-neutral-600 hover:bg-neutral-100 rounded-lg"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={async () => {
                  const id = deleteToolId;
                  setDeleteToolId(null);
                  try {
                    await api.deleteTool(id);
                    toast.success('Equipo eliminado o archivado con éxito');
                    loadData();
                  } catch (err: any) {
                    toast.error(err.message || 'Error al eliminar');
                  }
                }}
                className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg shadow-xs"
              >
                Sí, Eliminar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Resolver Reembolso */}
      {resolveRefundData !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-sm font-bold text-neutral-900 mb-1">
              {resolveRefundData.aprobar ? 'Aprobar Solicitud de Reembolso' : 'Rechazar Solicitud de Reembolso'}
            </h3>
            <p className="text-xs text-neutral-600 mb-4">
              Indique el motivo o resolución legal para el cliente:
            </p>
            <form onSubmit={async (e) => {
              e.preventDefault();
              const { id, aprobar } = resolveRefundData;
              const resText = refundResolution.trim() || (aprobar ? 'Aprobado conforme a políticas de servicio' : 'Rechazado');
              setResolveRefundData(null);
              setRefundResolution('');
              try {
                await api.resolveRefund(id, aprobar, resText);
                toast.success(`Reembolso ${aprobar ? 'aprobado' : 'rechazado'} correctamente.`);
                loadData();
              } catch (err: any) {
                toast.error(err.message || 'Error al resolver el reembolso', 'Error');
              }
            }}>
              <textarea
                required
                rows={3}
                value={refundResolution}
                onChange={(e) => setRefundResolution(e.target.value)}
                placeholder="Indique las observaciones técnicas..."
                className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg mb-4 focus:ring-1 focus:ring-amber-500 focus:outline-none"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => { setResolveRefundData(null); setRefundResolution(''); }}
                  className="px-3.5 py-1.5 text-xs text-neutral-600 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className={`px-4 py-2 text-xs font-bold text-white rounded-lg shadow-xs ${
                    resolveRefundData.aprobar ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  {resolveRefundData.aprobar ? 'Aprobar Reembolso' : 'Rechazar Reembolso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
