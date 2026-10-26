import React, { useState, useEffect, useMemo } from 'react';
import { Tool, Reservation, Category, RefundRequest } from '../types';
import { api } from '../services/api';
import {
  Plus,
  Check,
  X,
  Truck,
  Wrench,
  AlertTriangle,
  ShieldAlert,
  DollarSign,
  CheckCircle2,
  RotateCcw,
  Search,
  Calendar,
  ArrowRight,
  Clock,
  XCircle,
  Edit3,
  Trash2,
  Layers,
  Archive,
  Info,
  ShieldCheck,
  Building2
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { resolveToolImage, defaultExcavator } from '../utils/imageResolver';

interface OwnerDashboardProps {
  onNavigateCatalog?: () => void;
}

type TabType = 'flota' | 'reservas' | 'operaciones' | 'reembolsos';

export const OwnerDashboard: React.FC<OwnerDashboardProps> = ({ onNavigateCatalog }) => {
  const { user } = useAuth();
  const { toast } = useToast();

  const [activeTab, setActiveTab] = useState<TabType>('flota');
  const [tools, setTools] = useState<Tool[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [refunds, setRefunds] = useState<RefundRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filtros internos
  const [toolSearch, setToolSearch] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string | number | 'TODAS'>('TODAS');
  const [reservationSearch, setReservationSearch] = useState('');
  const [reservationStatusFilter, setReservationStatusFilter] = useState<string>('TODOS');

  // Modal crear/editar equipo
  const [showToolModal, setShowToolModal] = useState(false);
  const [editingTool, setEditingTool] = useState<Tool | null>(null);
  const [submittingTool, setSubmittingTool] = useState(false);
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

  // Modales operativos
  const [returnResv, setReturnResv] = useState<Reservation | null>(null);
  const [hasDamage, setHasDamage] = useState(false);
  const [damageDescription, setDamageDescription] = useState('');
  const [repairCost, setRepairCost] = useState(0);
  const [returnAccepted, setReturnAccepted] = useState(true);
  const [ownerObservation, setOwnerObservation] = useState('');
  const [submittingReturn, setSubmittingReturn] = useState(false);

  const [dispatchModalResv, setDispatchModalResv] = useState<Reservation | null>(null);
  const [submittingDispatch, setSubmittingDispatch] = useState(false);

  const [rejectModalResv, setRejectModalResv] = useState<Reservation | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [submittingReject, setSubmittingReject] = useState(false);

  const [deleteToolId, setDeleteToolId] = useState<string | number | null>(null);
  const [submittingDelete, setSubmittingDelete] = useState(false);

  const [resolveRefundData, setResolveRefundData] = useState<{ id: string | number; aprobar: boolean } | null>(null);
  const [refundResolution, setRefundResolution] = useState('');
  const [submittingRefundResolution, setSubmittingRefundResolution] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [catsData, toolsData, resvsData, refundsData] = await Promise.all([
        api.getCategories(),
        api.getTools({ propietarioId: user?.rol === 'ADMIN' ? undefined : user?.id }),
        api.getReservations(),
        api.getRefunds()
      ]);
      const ownerTools = user?.rol === 'ADMIN'
        ? toolsData
        : toolsData.filter(t => !user?.id || String(t.propietarioId) === String(user.id));
      setCategories(catsData);
      setTools(ownerTools);
      setReservations(resvsData);
      setRefunds(refundsData);
    } catch (e: unknown) {
      console.error('Error cargando panel propietario:', e);
      const message = e instanceof Error ? e.message : 'No se pudo conectar con el servidor para obtener los datos del propietario.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.id, user?.rol]);

  // Métricas reales verificables
  const metricas = useMemo(() => {
    const flotaTotal = tools.length;
    const unidadesDisponibles = tools.reduce(
      (acc, t) => acc + (t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1)),
      0
    );
    const solicitudesPendientes = reservations.filter(r => r.estado === 'PENDIENTE').length;
    const equiposEnObra = reservations.filter(r => r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA').length;
    const devolucionesPorInspeccionar = reservations.filter(
      r => ['EN_CURSO', 'ENTREGADA'].includes(r.estado) && (r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION')
    ).length;

    return {
      flotaTotal,
      unidadesDisponibles,
      solicitudesPendientes,
      equiposEnObra,
      devolucionesPorInspeccionar
    };
  }, [tools, reservations]);

  // Filtros de herramientas
  const filteredTools = useMemo(() => {
    return tools.filter(t => {
      if (selectedCategoryFilter !== 'TODAS' && t.categoriaId !== selectedCategoryFilter) return false;
      if (toolSearch.trim()) {
        const q = toolSearch.toLowerCase().trim();
        const match =
          t.nombre.toLowerCase().includes(q) ||
          (t.marca || '').toLowerCase().includes(q) ||
          (t.modelo || '').toLowerCase().includes(q) ||
          (t.descripcion || '').toLowerCase().includes(q);
        if (!match) return false;
      }
      return true;
    });
  }, [tools, toolSearch, selectedCategoryFilter]);

  // Grupos de reservas
  const pendingRequests = useMemo(() => reservations.filter(r => r.estado === 'PENDIENTE'), [reservations]);
  const pendingReturns = useMemo(
    () => reservations.filter(r => ['EN_CURSO', 'ENTREGADA'].includes(r.estado) && (r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION')),
    [reservations]
  );
  const activeDispatches = useMemo(
    () => reservations.filter(r => ['CONFIRMADA', 'PAGADA', 'EN_CURSO', 'ENTREGADA'].includes(r.estado) && !(r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION')),
    [reservations]
  );
  const completedHistory = useMemo(
    () => reservations.filter(r => ['FINALIZADA', 'DEVUELTA', 'CANCELADA', 'EXPIRADA', 'RECHAZADA'].includes(r.estado)),
    [reservations]
  );

  // Filtro de pestaña de reservas
  const filteredReservationsList = useMemo(() => {
    return reservations.filter(r => {
      if (reservationStatusFilter !== 'TODOS' && r.estado !== reservationStatusFilter) return false;
      if (reservationSearch.trim()) {
        const q = reservationSearch.toLowerCase().trim();
        const match =
          (r.herramientaNombre || '').toLowerCase().includes(q) ||
          (r.clienteNombre || '').toLowerCase().includes(q) ||
          String(r.id).includes(q);
        if (!match) return false;
      }
      return true;
    });
  }, [reservations, reservationSearch, reservationStatusFilter]);

  // Handlers para herramientas
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
      especificacionesTecnicas: 'Equipo certificado con mantenimiento preventivo.',
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
      setSubmittingTool(true);
      if (editingTool) {
        await api.updateTool(editingTool.id, formData);
        toast.success(`Equipo "${formData.nombre}" actualizado con éxito.`, 'Equipo Actualizado');
      } else {
        await api.createTool(formData);
        toast.success(`Equipo "${formData.nombre}" publicado en el catálogo oficial.`, 'Equipo Publicado');
      }
      setShowToolModal(false);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al guardar la herramienta.';
      toast.error(msg, 'Error al Guardar');
    } finally {
      setSubmittingTool(false);
    }
  };

  const handleDeleteTool = async () => {
    if (!deleteToolId) return;
    try {
      setSubmittingDelete(true);
      await api.deleteTool(deleteToolId);
      toast.success('Equipo retirado o archivado del catálogo con éxito.', 'Equipo Archivado');
      setDeleteToolId(null);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al retirar la herramienta.';
      toast.error(msg, 'Error');
    } finally {
      setSubmittingDelete(false);
    }
  };

  // Handlers para reservas
  const handleAccept = async (id: string | number) => {
    try {
      await api.acceptReservation(id);
      toast.success('Reserva aceptada con éxito. El cliente ya puede proceder al pago.', 'Reserva Aceptada');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al aceptar reserva.';
      toast.error(msg, 'Error al Aceptar');
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectModalResv) return;
    if (!rejectReason.trim()) {
      toast.warning('Debe especificar un motivo justificado para el rechazo.');
      return;
    }
    try {
      setSubmittingReject(true);
      await api.rejectReservation(rejectModalResv.id, rejectReason);
      toast.info('La reserva fue rechazada y el cupo liberado.', 'Reserva Rechazada');
      setRejectModalResv(null);
      setRejectReason('');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al rechazar reserva.';
      toast.error(msg, 'Error al Rechazar');
    } finally {
      setSubmittingReject(false);
    }
  };

  const handleDispatch = async () => {
    if (!dispatchModalResv) return;
    try {
      setSubmittingDispatch(true);
      await api.dispatchReservation(dispatchModalResv.id);
      toast.success('Equipo entregado en obra. La reserva ha iniciado formalmente.', 'Entrega Registrada');
      setDispatchModalResv(null);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar entrega del equipo.';
      toast.error(msg, 'Error de Entrega');
    } finally {
      setSubmittingDispatch(false);
    }
  };

  const handleConfirmReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!returnResv) return;
    if (hasDamage && !damageDescription.trim()) {
      toast.warning('Debe detallar la descripción de las averías o daños observados.');
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
            : 'Devolución confirmada en buen estado. Stock disponible restablecido en catálogo.',
          'Inspección Finalizada'
        );
      } else {
        toast.error('Devolución observada y rechazada temporalmente por averías graves.', 'Retorno Rechazado');
      }
      setReturnResv(null);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar la devolución e inspección.';
      toast.error(msg, 'Error de Inspección');
    } finally {
      setSubmittingReturn(false);
    }
  };

  const handleResolveRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolveRefundData) return;
    try {
      setSubmittingRefundResolution(true);
      await api.resolveRefund(
        resolveRefundData.id,
        resolveRefundData.aprobar,
        refundResolution || (resolveRefundData.aprobar ? 'Reembolso aprobado por el propietario titular' : 'Rechazado tras revisión técnica')
      );
      toast.success(`Solicitud de reembolso ${resolveRefundData.aprobar ? 'aprobada' : 'rechazada'} correctamente.`);
      setResolveRefundData(null);
      setRefundResolution('');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al resolver el reembolso.';
      toast.error(msg, 'Error en Reembolso');
    } finally {
      setSubmittingRefundResolution(false);
    }
  };

  return (
    <div className="w-full bg-[#F4F5F6]/40 min-h-screen pb-16">
      {/* =========================================================================
          1. ENCABEZADO EJECUTIVO INDUSTRIAL (Paleta Carbón / Ámbar de OBRIXEN)
         ========================================================================= */}
      <section className="bg-[#17191C] text-[#F4F5F6] border-b border-[#292D32] pt-8 pb-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider text-[#F5A623] bg-[#292D32] px-2.5 py-0.5 rounded-md border border-[#F5A623]/20">
                  Panel del Propietario
                </span>
                <span className="text-xs text-neutral-400 font-mono">
                  {user?.email}
                </span>
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F5F6]">
                Panel del Propietario {user?.nombre ? `· Flota de ${user.nombre}` : ''}
              </h1>
              <p className="text-xs sm:text-sm text-neutral-300 mt-1 max-w-2xl leading-relaxed">
                Control de inventario, aprobación de solicitudes de alquiler, entrega de maquinaria en obra y actas de inspección técnica.
              </p>
            </div>

            {/* Acciones principales del encabezado */}
            <div className="shrink-0 flex flex-wrap items-center gap-3">
              <button
                onClick={handleOpenCreate}
                className="w-full sm:w-auto px-4 py-2.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-bold text-xs sm:text-sm rounded-lg shadow-xs transition-colors flex items-center justify-center gap-2 cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Publicar Nuevo Equipo</span>
              </button>

              {onNavigateCatalog && (
                <button
                  onClick={onNavigateCatalog}
                  className="w-full sm:w-auto px-4 py-2.5 bg-[#292D32] hover:bg-[#343a40] text-neutral-200 font-semibold text-xs sm:text-sm rounded-lg border border-neutral-700 transition-colors flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Ver catálogo público</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          {/* =========================================================================
              2. TARJETAS KPI CON DATOS REALES DEL PROPIETARIO
             ========================================================================= */}
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-3.5 mt-8">
            {/* KPI 1: Flota Registrada */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-3.5">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-neutral-400 font-medium">Flota Registrada</span>
                <Layers className="w-4 h-4 text-[#F5A623]" />
              </div>
              <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                {loading ? '...' : metricas.flotaTotal}
              </span>
              <span className="text-[11px] text-neutral-400 mt-0.5 block">Modelos de equipo</span>
            </div>

            {/* KPI 2: Unidades Disponibles */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-3.5">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-neutral-400 font-medium">Unidades Stock</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              </div>
              <span className="text-2xl font-extrabold text-emerald-400 font-mono tabular-nums block">
                {loading ? '...' : metricas.unidadesDisponibles}
              </span>
              <span className="text-[11px] text-neutral-400 mt-0.5 block">Disponibles ahora</span>
            </div>

            {/* KPI 3: Solicitudes Pendientes */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-3.5">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-neutral-400 font-medium">Por Decidir</span>
                <Clock className="w-4 h-4 text-amber-400" />
              </div>
              <span className={`text-2xl font-extrabold font-mono tabular-nums block ${metricas.solicitudesPendientes > 0 ? 'text-[#F5A623]' : 'text-[#F4F5F6]'}`}>
                {loading ? '...' : metricas.solicitudesPendientes}
              </span>
              <span className="text-[11px] text-neutral-400 mt-0.5 block">Solicitudes pendientes</span>
            </div>

            {/* KPI 4: Equipos en Obra */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-3.5">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-neutral-400 font-medium">En Obra</span>
                <Truck className="w-4 h-4 text-purple-400" />
              </div>
              <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                {loading ? '...' : metricas.equiposEnObra}
              </span>
              <span className="text-[11px] text-neutral-400 mt-0.5 block">Alquiladas activas</span>
            </div>

            {/* KPI 5: Devoluciones por Inspeccionar */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-3.5 col-span-2 lg:col-span-1">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-neutral-400 font-medium">Por Inspeccionar</span>
                <Wrench className="w-4 h-4 text-rose-400" />
              </div>
              <span className={`text-2xl font-extrabold font-mono tabular-nums block ${metricas.devolucionesPorInspeccionar > 0 ? 'text-amber-400' : 'text-[#F4F5F6]'}`}>
                {loading ? '...' : metricas.devolucionesPorInspeccionar}
              </span>
              <span className="text-[11px] text-neutral-400 mt-0.5 block">Retornos notificados</span>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          3. NAVEGACIÓN POR PESTAÑAS Y CONTENIDO PRINCIPAL
         ========================================================================= */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        {/* Banner de Error si falla la API */}
        {error && (
          <div className="mb-6 p-4 rounded-xl border border-rose-200 bg-rose-50 text-rose-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0" />
              <div>
                <span className="text-xs font-bold block">Error de sincronización con el servidor</span>
                <span className="text-xs text-rose-700">{error}</span>
              </div>
            </div>
            <button
              onClick={loadData}
              className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-lg transition-colors cursor-pointer shrink-0"
            >
              Reintentar
            </button>
          </div>
        )}

        {/* Pestañas de Navegación Interna */}
        <div className="flex items-center gap-2 border-b border-neutral-200 mb-6 overflow-x-auto pb-1 scrollbar-thin">
          <button
            onClick={() => setActiveTab('flota')}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'flota'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Flota y Maquinaria</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'flota' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {tools.length}
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('reservas'); loadData(); }}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'reservas'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Solicitudes y Reservas</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'reservas' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {reservations.length}
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('operaciones'); loadData(); }}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'operaciones'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Despachos y Devoluciones</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'operaciones' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {activeDispatches.length + pendingReturns.length}
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('reembolsos'); loadData(); }}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'reembolsos'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Garantías y Reembolsos</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'reembolsos' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {refunds.length}
            </span>
          </button>
        </div>

        {/* =========================================================================
            PESTAÑA 1: FLOTA Y MAQUINARIA (INVENTARIO)
           ========================================================================= */}
        {activeTab === 'flota' && (
          <div>
            {/* Barra de Filtros para Flota */}
            <div className="bg-white rounded-xl border border-neutral-200 p-3.5 mb-6 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="flex flex-col sm:flex-row items-center gap-3 w-full sm:w-auto flex-1">
                <div className="relative w-full sm:w-72">
                  <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Buscar por nombre, marca o modelo..."
                    value={toolSearch}
                    onChange={(e) => setToolSearch(e.target.value)}
                    className="w-full pl-9 pr-3 py-1.5 text-xs bg-neutral-50 border border-neutral-200 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                  />
                </div>

                <div className="w-full sm:w-auto">
                  <select
                    value={selectedCategoryFilter}
                    onChange={(e) => setSelectedCategoryFilter(e.target.value)}
                    className="w-full sm:w-auto py-1.5 px-3 text-xs bg-neutral-50 border border-neutral-200 rounded-lg text-neutral-700 font-medium focus:outline-none focus:border-[#F5A623] cursor-pointer"
                  >
                    <option value="TODAS">Todas las categorías ({tools.length})</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.nombre}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="text-xs text-neutral-500 font-medium self-end sm:self-auto">
                Mostrando {filteredTools.length} de {tools.length} equipos
              </div>
            </div>

            {loading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((n) => (
                  <div key={n} className="bg-white rounded-xl border border-neutral-200 p-4 animate-pulse">
                    <div className="h-4 bg-neutral-200 rounded w-1/4 mb-2" />
                    <div className="h-3 bg-neutral-100 rounded w-1/2" />
                  </div>
                ))}
              </div>
            ) : tools.length === 0 ? (
              <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <Layers className="w-12 h-12 text-neutral-400 mx-auto mb-3" />
                <h3 className="text-base font-bold text-[#17191C]">No tienes equipos registrados en tu flota</h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 mb-5 leading-relaxed">
                  Publica tu maquinaria pesada, herramientas o andamiajes para recibir solicitudes de alquiler con tarifas en COP y pólizas de garantía.
                </p>
                <button
                  onClick={handleOpenCreate}
                  className="px-5 py-2.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-bold text-xs rounded-lg shadow-xs transition-colors cursor-pointer inline-flex items-center gap-2"
                >
                  <Plus className="w-4 h-4" />
                  <span>Publicar Primer Equipo</span>
                </button>
              </div>
            ) : filteredTools.length === 0 ? (
              <div className="text-center py-12 px-4 bg-white rounded-xl border border-neutral-200">
                <p className="text-xs text-neutral-500">No hay equipos que coincidan con los filtros aplicados.</p>
                <button
                  onClick={() => { setToolSearch(''); setSelectedCategoryFilter('TODAS'); }}
                  className="text-xs font-bold text-[#F5A623] hover:underline mt-2 cursor-pointer"
                >
                  Limpiar filtros
                </button>
              </div>
            ) : (
              <div>
                {/* Vista Escritorio (Tabla refinada de inventario) */}
                <div className="hidden md:block bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-neutral-200 bg-neutral-50 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                        <th className="py-3.5 px-4">Equipo</th>
                        <th className="py-3.5 px-4">Marca / Modelo</th>
                        <th className="py-3.5 px-4">Ubicación</th>
                        <th className="py-3.5 px-4 text-center">Stock Disp. / Total</th>
                        <th className="py-3.5 px-4 text-right">Tarifa Diaria (COP)</th>
                        <th className="py-3.5 px-4">Estado Operativo</th>
                        <th className="py-3.5 px-4 text-right">Acciones</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {filteredTools.map((t) => {
                        const stockDisp = t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1);
                        return (
                          <tr key={t.id} className="hover:bg-neutral-50/80 transition-colors">
                            <td className="py-3.5 px-4">
                              <div className="flex items-center gap-3">
                                <img
                                  src={resolveToolImage(t)}
                                  alt={t.nombre}
                                  className="w-11 h-11 object-cover rounded-lg border border-neutral-200 shrink-0"
                                  onError={(e) => { (e.target as HTMLImageElement).src = defaultExcavator; }}
                                />
                                <div className="truncate max-w-xs">
                                  <span className="font-bold text-[#17191C] block truncate">{t.nombre}</span>
                                  <span className="text-[11px] text-neutral-500 block truncate">{t.descripcion}</span>
                                </div>
                              </div>
                            </td>
                            <td className="py-3.5 px-4 font-mono text-neutral-600 whitespace-nowrap">
                              {t.marca || 'Estándar'} · {t.modelo || 'N/A'}
                            </td>
                            <td className="py-3.5 px-4 text-neutral-600 truncate max-w-[140px]">
                              {t.ubicacion || 'Bogotá, D.C.'}
                            </td>
                            <td className="py-3.5 px-4 text-center font-mono">
                              <span className={`font-bold ${stockDisp > 0 ? 'text-emerald-700' : 'text-rose-600'}`}>
                                {stockDisp}
                              </span>
                              <span className="text-neutral-400"> / {t.cantidadTotal || 1}</span>
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              <span className="font-mono font-bold text-[#17191C] tabular-nums block">
                                ${t.precioPorDia.toLocaleString('es-CO')}
                              </span>
                              <span className="text-[10px] text-neutral-400 block">+ IVA 19%</span>
                            </td>
                            <td className="py-3.5 px-4">
                              <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
                                t.estadoOperativo === 'DISPONIBLE' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' :
                                t.estadoOperativo === 'ALQUILADA' ? 'bg-purple-50 text-purple-800 border border-purple-200' :
                                'bg-amber-50 text-amber-800 border border-amber-200'
                              }`}>
                                {t.estadoOperativo}
                              </span>
                            </td>
                            <td className="py-3.5 px-4 text-right whitespace-nowrap space-x-1.5">
                              <button
                                onClick={() => handleOpenEdit(t)}
                                className="px-2.5 py-1 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                                <span>Editar</span>
                              </button>
                              <button
                                onClick={() => setDeleteToolId(t.id)}
                                className="px-2.5 py-1 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1"
                                title="Retirar o archivar equipo"
                              >
                                <Trash2 className="w-3.5 h-3.5 text-rose-600" />
                                <span>Archivar</span>
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Vista Móvil (Tarjetas táctiles sin scroll horizontal) */}
                <div className="md:hidden space-y-4">
                  {filteredTools.map((t) => {
                    const stockDisp = t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1);
                    return (
                      <div
                        key={t.id}
                        className="bg-white rounded-xl border border-neutral-200 p-4 shadow-xs flex flex-col justify-between"
                      >
                        <div className="flex items-start gap-3 pb-3 border-b border-neutral-100">
                          <img
                            src={resolveToolImage(t)}
                            alt={t.nombre}
                            className="w-14 h-14 object-cover rounded-lg border border-neutral-200 shrink-0"
                            onError={(e) => { (e.target as HTMLImageElement).src = defaultExcavator; }}
                          />
                          <div className="flex-1 min-w-0">
                            <span className="font-bold text-sm text-[#17191C] block truncate">{t.nombre}</span>
                            <span className="text-xs text-neutral-500 font-mono block">
                              {t.marca} · {t.modelo}
                            </span>
                            <span className="text-[11px] text-neutral-400 block truncate mt-0.5">
                              {t.ubicacion}
                            </span>
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-2 py-3 text-xs bg-neutral-50 p-2.5 rounded-lg border border-neutral-100 my-3">
                          <div>
                            <span className="text-[10px] text-neutral-400 block font-medium">Stock (Disp / Total)</span>
                            <span className="font-mono text-xs font-bold text-neutral-800">
                              {stockDisp} de {t.cantidadTotal || 1} unid.
                            </span>
                          </div>
                          <div className="text-right">
                            <span className="text-[10px] text-neutral-400 block font-medium">Tarifa Diaria (+ IVA)</span>
                            <span className="font-mono text-sm font-extrabold text-[#17191C]">
                              ${t.precioPorDia.toLocaleString('es-CO')}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 pt-1 border-t border-neutral-100">
                          <button
                            onClick={() => handleOpenEdit(t)}
                            className="flex-1 py-2 px-3 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-1.5"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>Editar</span>
                          </button>
                          <button
                            onClick={() => setDeleteToolId(t.id)}
                            className="py-2 px-3 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-1"
                          >
                            <Trash2 className="w-3.5 h-3.5 text-rose-600" />
                            <span>Archivar</span>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* =========================================================================
            PESTAÑA 2: SOLICITUDES Y RESERVAS
           ========================================================================= */}
        {activeTab === 'reservas' && (
          <div className="space-y-6">
            {/* Sección de Solicitudes Pendientes (Decisión Inmediata) */}
            <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
              <div className="px-6 py-4 border-b border-neutral-200 bg-amber-50/50 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                    <Clock className="w-4 h-4 text-[#F5A623]" />
                    <span>Solicitudes Pendientes de Aprobación ({pendingRequests.length})</span>
                  </h3>
                  <p className="text-xs text-neutral-500 mt-0.5">
                    Decide si apruebas o rechazas solicitudes de tus clientes para habilitar el pago.
                  </p>
                </div>
              </div>

              {pendingRequests.length === 0 ? (
                <div className="p-8 text-center text-xs text-neutral-500">
                  No hay solicitudes pendientes de decisión en este momento.
                </div>
              ) : (
                <div className="divide-y divide-neutral-100">
                  {pendingRequests.map((r) => (
                    <div
                      key={r.id}
                      className="p-4 sm:px-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-neutral-50 transition-colors"
                    >
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-bold text-sm text-[#17191C]">{r.herramientaNombre}</span>
                          <span className="text-xs text-neutral-400">·</span>
                          <span className="text-xs font-mono text-neutral-600">Cliente: {r.clienteNombre || 'Contratista'}</span>
                          <span className="text-xs font-mono text-neutral-400">#{r.id}</span>
                        </div>
                        <div className="text-xs text-neutral-600 font-mono space-x-2">
                          <span>Periodo: {r.fechaInicio} al {r.fechaFin} ({r.dias} días)</span>
                          <span>·</span>
                          <strong className="text-[#17191C]">${r.total.toLocaleString('es-CO')} COP</strong>
                        </div>
                        {r.notasCliente && (
                          <p className="text-xs text-neutral-500 mt-1 italic">
                            "Nota del cliente: {r.notasCliente}"
                          </p>
                        )}
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          onClick={() => handleAccept(r.id)}
                          className="px-4 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer shadow-xs"
                        >
                          <Check className="w-4 h-4" />
                          <span>Aceptar Reserva</span>
                        </button>
                        <button
                          onClick={() => { setRejectModalResv(r); setRejectReason(''); }}
                          className="px-3 py-2 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-lg border border-rose-200 transition-colors flex items-center gap-1.5 cursor-pointer"
                        >
                          <X className="w-4 h-4 text-rose-600" />
                          <span>Rechazar</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Historial General de Reservas */}
            <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
              <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-bold text-[#17191C]">
                    Historial Completo de Reservas ({reservations.length})
                  </h3>
                  <p className="text-xs text-neutral-500 mt-0.5">
                    Registro de todas las solicitudes, alquileres activos y operaciones finalizadas.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <select
                    value={reservationStatusFilter}
                    onChange={(e) => setReservationStatusFilter(e.target.value)}
                    className="py-1 px-2.5 text-xs bg-white border border-neutral-300 rounded-lg text-neutral-700 font-medium cursor-pointer"
                  >
                    <option value="TODOS">Todos los estados</option>
                    <option value="PENDIENTE">PENDIENTE</option>
                    <option value="CONFIRMADA">CONFIRMADA</option>
                    <option value="PAGADA">PAGADA</option>
                    <option value="EN_CURSO">EN_CURSO</option>
                    <option value="FINALIZADA">FINALIZADA</option>
                    <option value="CANCELADA">CANCELADA</option>
                  </select>
                </div>
              </div>

              {filteredReservationsList.length === 0 ? (
                <div className="p-8 text-center text-xs text-neutral-500">
                  No se encontraron reservas con el filtro seleccionado.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-neutral-200 bg-neutral-50 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                        <th className="py-3 px-4">Reserva</th>
                        <th className="py-3 px-4">Equipo</th>
                        <th className="py-3 px-4">Cliente</th>
                        <th className="py-3 px-4">Periodo de Obra</th>
                        <th className="py-3 px-4 text-right">Total (COP)</th>
                        <th className="py-3 px-4">Estado</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {filteredReservationsList.map((r) => (
                        <tr key={r.id} className="hover:bg-neutral-50">
                          <td className="py-3 px-4 font-mono font-bold text-neutral-600">#{r.id}</td>
                          <td className="py-3 px-4 font-bold text-[#17191C]">{r.herramientaNombre}</td>
                          <td className="py-3 px-4 text-neutral-700">{r.clienteNombre || 'Contratista'}</td>
                          <td className="py-3 px-4 font-mono text-neutral-600 whitespace-nowrap">
                            {r.fechaInicio} al {r.fechaFin}
                          </td>
                          <td className="py-3 px-4 text-right font-mono font-bold text-[#17191C]">
                            ${r.total.toLocaleString('es-CO')}
                          </td>
                          <td className="py-3 px-4">
                            <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-neutral-100 text-neutral-700">
                              {r.estado}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* =========================================================================
            PESTAÑA 3: DESPACHOS Y DEVOLUCIONES TÉCNICAS
           ========================================================================= */}
        {activeTab === 'operaciones' && (
          <div className="space-y-6">
            {/* Devoluciones Notificadas que Requieren Inspección */}
            <div className="bg-amber-50/60 rounded-xl border border-amber-300 overflow-hidden shadow-xs">
              <div className="px-6 py-4 bg-amber-100/70 border-b border-amber-200 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-amber-950 flex items-center gap-2">
                    <RotateCcw className="w-4 h-4 text-amber-800" />
                    <span>Devoluciones Notificadas por Clientes ({pendingReturns.length})</span>
                  </h3>
                  <p className="text-xs text-amber-900 mt-0.5">
                    El cliente concluyó el uso del equipo. Realiza la inspección técnica para validar el estado físico o reportar averías.
                  </p>
                </div>
              </div>

              {pendingReturns.length === 0 ? (
                <div className="p-8 text-center text-xs text-neutral-500 bg-white">
                  No hay equipos en espera de inspección técnica en este momento.
                </div>
              ) : (
                <div className="divide-y divide-amber-200/60 bg-white">
                  {pendingReturns.map((r) => (
                    <div
                      key={r.id}
                      className="p-4 sm:px-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-neutral-50 transition-colors"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-[#17191C]">{r.herramientaNombre}</span>
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
                        {r.notaDevolucionCliente && (
                          <div className="bg-amber-50 p-2.5 rounded-lg border border-amber-200 text-xs text-amber-900 mt-1 max-w-xl">
                            <span className="font-semibold block text-[11px] text-amber-950">Informe del Cliente:</span>
                            <p className="italic font-mono text-[11px] mt-0.5">"{r.notaDevolucionCliente}"</p>
                          </div>
                        )}
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
                          className="px-4 py-2 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg shadow-xs inline-flex items-center gap-1.5 cursor-pointer transition-colors"
                        >
                          <Wrench className="w-3.5 h-3.5" />
                          <span>Inspeccionar y Validar Retorno</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Operaciones de Despacho y Entrega */}
            <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
              <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-[#17191C] flex items-center gap-2">
                    <Truck className="w-4 h-4 text-[#F5A623]" />
                    <span>Control de Despachos y Operaciones en Obra ({activeDispatches.length})</span>
                  </h3>
                  <p className="text-xs text-neutral-500 mt-0.5">
                    Registra la entrega física de maquinaria pagada o confirma la recepción de retorno.
                  </p>
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
                      <tr className="border-b border-neutral-200 bg-neutral-50 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                        <th className="py-3 px-4">Reserva</th>
                        <th className="py-3 px-4">Equipo</th>
                        <th className="py-3 px-4">Cliente</th>
                        <th className="py-3 px-4">Estado Operativo</th>
                        <th className="py-3 px-4 text-right">Acción Operativa</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {activeDispatches.map((r) => (
                        <tr key={r.id} className="hover:bg-neutral-50">
                          <td className="py-3.5 px-4 font-mono font-bold text-neutral-600">#{r.id}</td>
                          <td className="py-3.5 px-4 font-bold text-[#17191C]">{r.herramientaNombre}</td>
                          <td className="py-3.5 px-4 text-neutral-700">{r.clienteNombre || 'Contratista'}</td>
                          <td className="py-3.5 px-4">
                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
                              (r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA')
                                ? 'bg-purple-50 text-purple-800 border border-purple-200'
                                : r.pagada
                                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                                : 'bg-amber-50 text-amber-800 border border-amber-200'
                            }`}>
                              {(r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA')
                                ? 'EN OBRA (Alquilada)'
                                : r.pagada
                                ? 'PAGADA (Lista para entrega)'
                                : 'CONFIRMADA (Esperando pago)'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            {(r.estado === 'CONFIRMADA' || r.estado === 'PAGADA') && r.pagada && (
                              <button
                                onClick={() => setDispatchModalResv(r)}
                                className="px-3.5 py-1.5 text-xs font-bold text-white bg-[#17191C] hover:bg-neutral-800 rounded-lg inline-flex items-center gap-1.5 cursor-pointer shadow-xs"
                              >
                                <Truck className="w-3.5 h-3.5 text-[#F5A623]" />
                                <span>Registrar Entrega</span>
                              </button>
                            )}
                            {(r.estado === 'CONFIRMADA' || r.estado === 'PAGADA') && !r.pagada && (
                              <span className="text-[11px] text-amber-700 font-medium italic">
                                Esperando pago del cliente
                              </span>
                            )}
                            {(r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA') && (
                              <button
                                onClick={() => {
                                  setReturnResv(r);
                                  setReturnAccepted(true);
                                  setHasDamage(false);
                                  setDamageDescription('');
                                  setRepairCost(0);
                                  setOwnerObservation('');
                                }}
                                className="px-3.5 py-1.5 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg inline-flex items-center gap-1.5 cursor-pointer shadow-xs"
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
          </div>
        )}

        {/* =========================================================================
            PESTAÑA 4: GARANTÍAS Y REEMBOLSOS
           ========================================================================= */}
        {activeTab === 'reembolsos' && (
          <div>
            <div className="bg-white rounded-xl border border-neutral-200 p-4 mb-6 shadow-xs">
              <h3 className="text-sm font-bold text-[#17191C] flex items-center gap-2">
                <DollarSign className="w-4 h-4 text-[#F5A623]" />
                <span>Solicitudes de Reembolso Recibidas ({refunds.length})</span>
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">
                Evalúa las solicitudes radicadas por los clientes emitiendo tu dictamen técnico de aprobación o denegación.
              </p>
            </div>

            {loading ? (
              <div className="space-y-3">
                {[1, 2].map((n) => (
                  <div key={n} className="bg-white rounded-xl border border-neutral-200 p-6 animate-pulse">
                    <div className="h-4 bg-neutral-200 rounded w-1/4 mb-2" />
                    <div className="h-3 bg-neutral-100 rounded w-1/2" />
                  </div>
                ))}
              </div>
            ) : refunds.length === 0 ? (
              <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <ShieldCheck className="w-12 h-12 text-neutral-400 mx-auto mb-3" />
                <h3 className="text-base font-bold text-[#17191C]">No hay solicitudes de reembolso registradas</h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 leading-relaxed">
                  Cuando un cliente radique una solicitud de garantía o devolución monetaria, podrás revisarla y resolverla aquí.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {refunds.map((ref) => (
                  <div
                    key={ref.id}
                    className="p-4 bg-white rounded-xl border border-neutral-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs"
                  >
                    <div>
                      <div className="flex items-center gap-2 font-mono text-neutral-600 mb-1">
                        <span className="font-bold text-[#17191C]">Solicitud #{ref.id}</span>
                        <span>·</span>
                        <span>Monto: <strong className="text-[#17191C]">${ref.monto.toLocaleString('es-CO')} COP</strong></span>
                        <span>·</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          ref.estado === 'APROBADO' || (ref.estado as string) === 'REEMBOLSADO' ? 'bg-emerald-100 text-emerald-800' :
                          ref.estado === 'RECHAZADO' || (ref.estado as string) === 'FALLIDO' ? 'bg-rose-100 text-rose-800' :
                          'bg-amber-100 text-amber-800'
                        }`}>
                          {ref.estado === 'APROBADO' || (ref.estado as string) === 'REEMBOLSADO' ? 'APROBADO' :
                           ref.estado === 'RECHAZADO' || (ref.estado as string) === 'FALLIDO' ? 'RECHAZADO' :
                           ref.estado}
                        </span>
                      </div>
                      <p className="text-neutral-700 italic mb-1">"{ref.motivo}"</p>
                      {ref.resolucion && (
                        <p className="text-[11px] text-neutral-500">Dictamen técnico: {ref.resolucion}</p>
                      )}
                    </div>

                    {ref.estado === 'PENDIENTE' && (
                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          onClick={() => { setResolveRefundData({ id: ref.id, aprobar: true }); setRefundResolution(''); }}
                          className="px-3.5 py-1.5 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg cursor-pointer shadow-xs"
                        >
                          Aprobar
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
            )}
          </div>
        )}
      </div>

      {/* =========================================================================
          4. MODALES CONTEXTUALES OPERATIVOS
         ========================================================================= */}

      {/* Modal 1: Publicar / Editar Equipo */}
      {showToolModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#17191C]/70 backdrop-blur-xs p-4 overflow-y-auto animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-base font-bold text-[#17191C] mb-4">
              {editingTool ? 'Editar Maquinaria / Equipo' : 'Publicar Nuevo Equipo en la Flota'}
            </h3>
            <form onSubmit={handleSaveTool} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-neutral-800 mb-1">Nombre Comercial *</label>
                <input
                  type="text"
                  required
                  value={formData.nombre}
                  onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                  placeholder="Ej: Retroexcavadora CAT 420F"
                  className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-800 mb-1">Categoría *</label>
                  <select
                    value={formData.categoriaId}
                    onChange={(e) => setFormData({ ...formData, categoriaId: e.target.value })}
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] text-neutral-900 cursor-pointer"
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
                  <label className="block font-semibold text-neutral-800 mb-1">Marca</label>
                  <input
                    type="text"
                    value={formData.marca}
                    onChange={(e) => setFormData({ ...formData, marca: e.target.value })}
                    placeholder="Ej: Layher, Caterpillar"
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-neutral-800 mb-1">Modelo</label>
                  <input
                    type="text"
                    value={formData.modelo}
                    onChange={(e) => setFormData({ ...formData, modelo: e.target.value })}
                    placeholder="Ej: 420F, Allround"
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-800 mb-1">Tarifa Día (COP) *</label>
                  <input
                    type="number"
                    min="1000"
                    step="1000"
                    required
                    value={formData.precioPorDia}
                    onChange={(e) => setFormData({ ...formData, precioPorDia: parseFloat(e.target.value) || 0 })}
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-neutral-800 mb-1">Depósito Garantía (COP)</label>
                  <input
                    type="number"
                    min="0"
                    step="10000"
                    value={formData.depositoGarantia}
                    onChange={(e) => setFormData({ ...formData, depositoGarantia: parseFloat(e.target.value) || 0 })}
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-neutral-800 mb-1">Unidades en Flota *</label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    required
                    value={formData.cantidadTotal || 1}
                    onChange={(e) => setFormData({ ...formData, cantidadTotal: parseInt(e.target.value, 10) || 1 })}
                    className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-neutral-800 mb-1">Ubicación de Despacho</label>
                <input
                  type="text"
                  value={formData.ubicacion}
                  onChange={(e) => setFormData({ ...formData, ubicacion: e.target.value })}
                  placeholder="Ej: Bogotá - Zona Industrial Fontibón"
                  className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                />
              </div>

              <div>
                <label className="block font-semibold text-neutral-800 mb-1">Descripción Comercial</label>
                <textarea
                  rows={2}
                  value={formData.descripcion}
                  onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                  placeholder="Capacidad operativa, potencia y aplicaciones en obra..."
                  className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingTool}
                  onClick={() => setShowToolModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingTool}
                  className="px-5 py-2 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingTool ? 'Guardando...' : (editingTool ? 'Actualizar Equipo' : 'Publicar Equipo')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 2: Registrar Entrega / Despacho en Obra */}
      {dispatchModalResv && (
        <div className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <div className="w-11 h-11 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center mx-auto mb-3">
              <Truck className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#17191C] text-center mb-1">
              Confirmar Despacho y Entrega
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              ¿Confirmas la entrega en obra del equipo <strong>{dispatchModalResv.herramientaNombre}</strong> para la reserva <strong>#{dispatchModalResv.id}</strong>?
            </p>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
              <button
                type="button"
                disabled={submittingDispatch}
                onClick={() => setDispatchModalResv(null)}
                className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={submittingDispatch}
                onClick={handleDispatch}
                className="px-4 py-2 text-xs font-bold text-white bg-[#17191C] hover:bg-neutral-800 rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
              >
                {submittingDispatch ? 'Registrando...' : 'Confirmar Entrega'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 3: Rechazar Reserva */}
      {rejectModalResv && (
        <div className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <div className="w-11 h-11 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
              <X className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#17191C] text-center mb-1">
              Rechazar Solicitud #{rejectModalResv.id}
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              Indica el motivo de rechazo para la reserva de <strong>{rejectModalResv.herramientaNombre}</strong>.
            </p>

            <form onSubmit={handleReject}>
              <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                Motivo Justificado <span className="text-rose-500">*</span>
              </label>
              <textarea
                required
                rows={3}
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Ej: Equipo en mantenimiento preventivo, indisponibilidad de operador..."
                className="w-full text-xs p-3 bg-neutral-50 border border-neutral-300 rounded-lg mb-4 focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
              />

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingReject}
                  onClick={() => setRejectModalResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingReject}
                  className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingReject ? 'Rechazando...' : 'Confirmar Rechazo'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 4: Inspección Técnica y Retorno */}
      {returnResv && (
        <div className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-neutral-200 animate-in fade-in duration-150">
            <div className="w-11 h-11 rounded-xl bg-amber-50 text-[#F5A623] flex items-center justify-center mx-auto mb-3">
              <Wrench className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#17191C] text-center mb-1">
              Acta de Inspección Técnica y Retorno
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              Verifica el estado físico y operativo de <strong>{returnResv.herramientaNombre}</strong> (Reserva #{returnResv.id}).
            </p>

            <form onSubmit={handleConfirmReturn} className="space-y-3.5 text-xs">
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 space-y-1 font-mono">
                <div className="flex justify-between">
                  <span className="text-neutral-500">Cliente:</span>
                  <span className="font-bold text-[#17191C]">{returnResv.clienteNombre || 'Contratista'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500">Periodo:</span>
                  <span>{returnResv.fechaInicio} al {returnResv.fechaFin}</span>
                </div>
              </div>

              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200">
                <label className="flex items-center gap-2 cursor-pointer font-bold text-neutral-800">
                  <input
                    type="checkbox"
                    checked={hasDamage}
                    onChange={(e) => setHasDamage(e.target.checked)}
                    className="rounded text-[#F5A623] focus:ring-[#F5A623] accent-[#F5A623]"
                  />
                  <span>¿El equipo presenta daños, averías o faltantes?</span>
                </label>

                {hasDamage && (
                  <div className="mt-3 space-y-3 pt-3 border-t border-neutral-200 animate-in fade-in duration-150">
                    <div>
                      <label className="block font-semibold text-neutral-700 mb-1">Descripción de Daños *</label>
                      <textarea
                        required
                        rows={2}
                        value={damageDescription}
                        onChange={(e) => setDamageDescription(e.target.value)}
                        placeholder="Ej: Rotura en eje secundario, desgaste anormal en brocas..."
                        className="w-full p-2 bg-white border border-neutral-300 rounded-lg text-neutral-900"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-neutral-700 mb-1">Costo Estimado de Reparación (COP)</label>
                      <input
                        type="number"
                        min="0"
                        step="1000"
                        value={repairCost}
                        onChange={(e) => setRepairCost(parseFloat(e.target.value) || 0)}
                        className="w-full p-2 bg-white border border-neutral-300 rounded-lg font-mono text-neutral-900"
                      />
                    </div>
                  </div>
                )}
              </div>

              <div>
                <label className="block font-semibold text-neutral-800 mb-1">Observaciones del Propietario</label>
                <textarea
                  rows={2}
                  value={ownerObservation}
                  onChange={(e) => setOwnerObservation(e.target.value)}
                  placeholder="Detalles sobre el estado de entrega o limpieza del equipo..."
                  className="w-full p-2 bg-neutral-50 border border-neutral-300 rounded-lg text-neutral-900"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingReturn}
                  onClick={() => setReturnResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingReturn}
                  className="px-5 py-2 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingReturn ? 'Registrando...' : 'Finalizar Inspección'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 5: Confirmar Archivar / Retirar Equipo */}
      {deleteToolId && (
        <div className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <div className="w-11 h-11 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
              <Archive className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#17191C] text-center mb-1">
              Retirar Equipo del Catálogo
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              ¿Deseas archivar o retirar este equipo de la plataforma? No se mostrará a nuevos clientes en el catálogo público.
            </p>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
              <button
                type="button"
                disabled={submittingDelete}
                onClick={() => setDeleteToolId(null)}
                className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={submittingDelete}
                onClick={handleDeleteTool}
                className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
              >
                {submittingDelete ? 'Retirando...' : 'Confirmar Retiro'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 6: Resolver Reembolso */}
      {resolveRefundData && (
        <div className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-base font-bold text-[#17191C] mb-1">
              {resolveRefundData.aprobar ? 'Aprobar Solicitud de Reembolso' : 'Rechazar Solicitud de Reembolso'}
            </h3>
            <p className="text-xs text-neutral-600 mb-4">
              Registra el dictamen o resolución técnica oficial que se notificará al cliente.
            </p>

            <form onSubmit={handleResolveRefund}>
              <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                Dictamen / Resolución Técnica
              </label>
              <textarea
                required
                rows={3}
                value={refundResolution}
                onChange={(e) => setRefundResolution(e.target.value)}
                placeholder="Detalla las consideraciones técnicas tomadas..."
                className="w-full text-xs p-3 bg-neutral-50 border border-neutral-300 rounded-lg mb-4 text-neutral-900"
              />

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingRefundResolution}
                  onClick={() => setResolveRefundData(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingRefundResolution}
                  className={`px-4 py-2 text-xs font-bold text-white rounded-lg shadow-xs cursor-pointer ${
                    resolveRefundData.aprobar ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  {submittingRefundResolution ? 'Guardando...' : (resolveRefundData.aprobar ? 'Confirmar Aprobación' : 'Confirmar Rechazo')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
