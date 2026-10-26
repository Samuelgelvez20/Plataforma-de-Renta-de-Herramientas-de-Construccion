import React, { useState, useEffect, useMemo } from 'react';
import { Reservation, Invoice, RefundRequest } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  Calendar,
  CreditCard,
  FileDown,
  RotateCcw,
  CheckCircle2,
  Clock,
  XCircle,
  AlertTriangle,
  ArrowRight,
  FileText,
  ShieldCheck,
  Truck,
  Info,
  X,
  Receipt,
  Search,
  Filter,
  Wrench
} from 'lucide-react';
import { CheckoutPaymentModal } from './CheckoutPaymentModal';

interface ClientDashboardProps {
  onPayReservation: (reservation: Reservation) => void;
  onViewInvoice: (invoiceId: number) => void;
  onNavigateCatalog?: () => void;
}

type TabType = 'reservas' | 'facturas' | 'reembolsos';

export const ClientDashboard: React.FC<ClientDashboardProps> = ({
  onPayReservation,
  onNavigateCatalog
}) => {
  const { user } = useAuth();
  const { toast } = useToast();

  const [activeTab, setActiveTab] = useState<TabType>('reservas');
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [refunds, setRefunds] = useState<RefundRequest[]>([]);
  const [toolMap, setToolMap] = useState<Record<string | number, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filtro interno para reservas
  const [reservationSearch, setReservationSearch] = useState('');
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<string | number | null>(null);
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  const pendingRepairs = useMemo(() => {
    return invoices.filter(inv => inv.tipo === 'REPARACION' && inv.estado === 'PENDIENTE');
  }, [invoices]);

  // Modales
  const [selectedRefundResv, setSelectedRefundResv] = useState<Reservation | null>(null);
  const [refundReason, setRefundReason] = useState('');
  const [submittingRefund, setSubmittingRefund] = useState(false);

  const [returnConfirmResv, setReturnConfirmResv] = useState<Reservation | null>(null);
  const [submittingReturn, setSubmittingReturn] = useState(false);
  const [returnClientNote, setReturnClientNote] = useState('');

  const [cancelConfirmResv, setCancelConfirmResv] = useState<Reservation | null>(null);
  const [submittingCancel, setSubmittingCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [resvs, invs, refs, toolsData] = await Promise.all([
        api.getReservations(),
        api.getInvoices(),
        api.getRefunds(),
        api.getTools().catch(() => [])
      ]);
      const map: Record<string | number, string> = {};
      toolsData.forEach(t => {
        if (t.id) map[t.id] = t.nombre;
      });
      setToolMap(map);
      setReservations(resvs);
      setInvoices(invs);
      setRefunds(refs);
    } catch (e: unknown) {
      console.error('Error cargando panel cliente:', e);
      const message = e instanceof Error ? e.message : 'No se pudo conectar con el servidor para obtener los datos del cliente.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Métricas reales verificables (sin duplicaciones ni datos inventados)
  const metricas = useMemo(() => {
    // 1. En obra / activas: alquileres en curso o entregados
    const enObra = reservations.filter(
      r => r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA'
    ).length;

    // 2. Por pagar: confirmadas o aceptadas pero no pagadas
    const porPagar = reservations.filter(
      r => (r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada
    ).length;

    // 3. Pendientes de aprobación por parte del propietario
    const pendientes = reservations.filter(
      r => r.estado === 'PENDIENTE'
    ).length;

    // 4. Facturas oficiales emitidas y total facturado en COP
    const facturasTotal = invoices.length;
    const totalFacturadoCop = invoices.reduce((acc, inv) => acc + (inv.total || 0), 0);

    return {
      enObra,
      porPagar,
      pendientes,
      facturasTotal,
      totalFacturadoCop
    };
  }, [reservations, invoices]);

  // Filtrado de reservas
  const filteredReservations = useMemo(() => {
    if (!reservationSearch.trim()) return reservations;
    const q = reservationSearch.toLowerCase().trim();
    return reservations.filter(r => {
      const toolName = (r.herramientaId && toolMap[r.herramientaId]) || r.herramientaNombre || '';
      return (
        toolName.toLowerCase().includes(q) ||
        String(r.id).includes(q) ||
        (r.notasCliente || '').toLowerCase().includes(q) ||
        r.estado.toLowerCase().includes(q)
      );
    });
  }, [reservations, reservationSearch, toolMap]);

  const handleDownloadPdf = async (inv: Invoice) => {
    try {
      setDownloadingInvoiceId(inv.id);
      await api.downloadInvoicePdf(inv.id, inv.numeroFactura);
      toast.success(`Factura ${inv.numeroFactura} descargada con éxito.`, 'Descarga Exitosa');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Error descargando el comprobante PDF oficial.';
      toast.error(msg, 'Fallo de Descarga');
    } finally {
      setDownloadingInvoiceId(null);
    }
  };

  const handleCancelReservation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelConfirmResv) return;
    if (!cancelReason.trim()) {
      toast.error('Debe indicar un motivo para cancelar la solicitud.', 'Campo requerido');
      return;
    }
    try {
      setSubmittingCancel(true);
      await api.cancelReservation(cancelConfirmResv.id, cancelReason);
      toast.success('Reserva cancelada correctamente. Se ha liberado la disponibilidad del equipo.', 'Reserva Cancelada');
      setCancelConfirmResv(null);
      setCancelReason('');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al cancelar la reserva.';
      toast.error(msg, 'Fallo de Cancelación');
    } finally {
      setSubmittingCancel(false);
    }
  };

  const handleReturnEquipment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!returnConfirmResv) return;
    try {
      setSubmittingReturn(true);
      await api.requestReturn(returnConfirmResv.id, { notaCliente: returnClientNote });
      toast.success('Retorno notificado al propietario. El equipo queda en espera de inspección técnica en obra.', 'Devolución Notificada');
      setReturnConfirmResv(null);
      setReturnClientNote('');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al notificar la devolución del equipo.';
      toast.error(msg, 'Fallo en Devolución');
    } finally {
      setSubmittingReturn(false);
    }
  };

  const handleSubmitRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRefundResv) return;
    if (!refundReason.trim()) {
      toast.error('Debe explicar el motivo justificado de la solicitud de reembolso.', 'Campo requerido');
      return;
    }

    const inv = invoices.find(f => f.reservaId === selectedRefundResv.id);
    if (!inv) {
      toast.error('No se localizó la factura o pago asociado a esta reserva para radicar el reembolso.', 'Pago no Encontrado');
      return;
    }

    try {
      setSubmittingRefund(true);
      await api.requestRefund(inv.pagoId || selectedRefundResv.id, refundReason, selectedRefundResv.id, inv.total);
      toast.success('Solicitud de reembolso radicada con éxito. Será evaluada por el propietario y administración.', 'Reembolso Solicitado');
      setSelectedRefundResv(null);
      setRefundReason('');
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al solicitar el reembolso.';
      toast.error(msg, 'Fallo en Solicitud');
    } finally {
      setSubmittingRefund(false);
    }
  };

  const renderStatusBadge = (r: Reservation) => {
    const estado = r.estado;
    switch (estado) {
      case 'PENDIENTE':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            <Clock className="w-3.5 h-3.5 text-[#F5A623] shrink-0" />
            <span>Pendiente de Aprobación</span>
          </span>
        );
      case 'CONFIRMADA':
      case 'ACEPTADA':
        if (r.pagada) {
          return (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>Pagada · Por Despachar</span>
            </span>
          );
        }
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-blue-50 text-blue-800 border border-blue-200">
            <CheckCircle2 className="w-3.5 h-3.5 text-blue-600 shrink-0" />
            <span>Aprobada · Pendiente de Pago</span>
          </span>
        );
      case 'PAGADA':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
            <span>Pagada · Por Despachar</span>
          </span>
        );
      case 'EN_CURSO':
      case 'ENTREGADA':
        if (r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION') {
          return (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
              <Clock className="w-3.5 h-3.5 text-[#F5A623] shrink-0" />
              <span>Devolución en Inspección</span>
            </span>
          );
        }
        if (r.estadoDevolucion === 'RECHAZADA_POR_DANOS') {
          return (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-rose-50 text-rose-800 border border-rose-200">
              <AlertTriangle className="w-3.5 h-3.5 text-rose-600 shrink-0" />
              <span>Retorno Observado por Daños</span>
            </span>
          );
        }
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-purple-50 text-purple-800 border border-purple-200">
            <Truck className="w-3.5 h-3.5 text-purple-600 shrink-0" />
            <span>En Obra (Alquilada)</span>
          </span>
        );
      case 'FINALIZADA':
      case 'DEVUELTA':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
            <span>Devuelta · Finalizada</span>
          </span>
        );
      case 'CANCELADA':
      case 'EXPIRADA':
      case 'RECHAZADA':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-rose-50 text-rose-800 border border-rose-200">
            <XCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />
            <span>{estado}</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 text-neutral-700">
            {estado}
          </span>
        );
    }
  };

  return (
    <div className="w-full bg-[#F4F5F6]/40 min-h-screen pb-16">
      {/* =========================================================================
          1. ENCABEZADO PRINCIPAL (Paleta Carbón / Ámbar de OBRIXEN)
         ========================================================================= */}
      <section className="bg-[#17191C] text-[#F4F5F6] border-b border-[#292D32] pt-8 pb-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider text-[#F5A623] bg-[#292D32] px-2.5 py-0.5 rounded-md border border-[#F5A623]/20">
                  Panel de Cliente
                </span>
                <span className="text-xs text-neutral-400 font-mono">
                  {user?.email}
                </span>
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F5F6]">
                {user?.nombre ? `Hola, ${user.nombre}` : 'Mi espacio de cliente'}
              </h1>
              <p className="text-xs sm:text-sm text-neutral-300 mt-1 max-w-2xl leading-relaxed">
                Gestiona tus solicitudes de alquiler de maquinaria, cronograma de obra, facturación oficial con IVA del 19 % y devoluciones técnicas.
              </p>
            </div>

            {/* Acción de navegación hacia el catálogo */}
            <div className="shrink-0 flex items-center gap-3">
              {onNavigateCatalog && (
                <button
                  onClick={onNavigateCatalog}
                  className="w-full sm:w-auto px-4 py-2.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-bold text-xs sm:text-sm rounded-lg shadow-xs transition-colors flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Explorar equipos</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          {/* Banner de alerta: Cargos por daños y reparaciones pendientes */}
          {pendingRepairs.length > 0 && (
            <div className="mt-6 bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-in fade-in">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-amber-500/20 text-amber-400 rounded-lg shrink-0">
                  <Wrench className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-amber-200 flex items-center gap-2">
                    <span>Atención: {pendingRepairs.length} cargo{pendingRepairs.length > 1 ? 's' : ''} pendiente{pendingRepairs.length > 1 ? 's' : ''} por daños y reparación de equipo</span>
                  </h4>
                  <p className="text-xs text-amber-300/80 mt-0.5">
                    El propietario ha confirmado la devolución con reporte de daños. Revisa los detalles en «Mis facturas» y efectúa el pago para completar el cierre de tu reserva.
                  </p>
                </div>
              </div>
              <button
                onClick={() => setActiveTab('facturas')}
                className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold text-xs rounded-lg transition-colors shrink-0 cursor-pointer shadow-xs"
              >
                Ver facturas de reparación
              </button>
            </div>
          )}

          {/* =========================================================================
              2. TARJETAS DE MÉTRICAS (KPIs basados en datos reales verificables)
             ========================================================================= */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-8">
            {/* KPI 1: En Obra */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-neutral-400 font-medium">En Obra</span>
                <Truck className="w-4 h-4 text-purple-400" />
              </div>
              <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                {loading ? '...' : metricas.enObra}
              </span>
              <span className="text-[11px] text-neutral-400 mt-1 block">Equipos en uso</span>
            </div>

            {/* KPI 2: Por Pagar */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-neutral-400 font-medium">Por Pagar</span>
                <CreditCard className="w-4 h-4 text-[#F5A623]" />
              </div>
              <span className="text-2xl font-extrabold text-[#F5A623] font-mono tabular-nums block">
                {loading ? '...' : metricas.porPagar}
              </span>
              <span className="text-[11px] text-neutral-400 mt-1 block">Aprobadas listas</span>
            </div>

            {/* KPI 3: Pendientes */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-neutral-400 font-medium">Pendientes</span>
                <Clock className="w-4 h-4 text-amber-400" />
              </div>
              <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                {loading ? '...' : metricas.pendientes}
              </span>
              <span className="text-[11px] text-neutral-400 mt-1 block">En revisión propietario</span>
            </div>

            {/* KPI 4: Facturas */}
            <div className="bg-[#292D32]/70 border border-[#292D32] rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-neutral-400 font-medium">Facturas Oficiales</span>
                <Receipt className="w-4 h-4 text-emerald-400" />
              </div>
              <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                {loading ? '...' : metricas.facturasTotal}
              </span>
              <span className="text-[11px] text-neutral-400 mt-1 block truncate">
                {metricas.totalFacturadoCop > 0 ? `$${(metricas.totalFacturadoCop / 1000).toLocaleString('es-CO')}k COP` : 'IVA 19%'}
              </span>
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
                <span className="text-xs font-bold block">Error al sincronizar datos</span>
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
            onClick={() => { setActiveTab('reservas'); loadData(); }}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'reservas'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Mis Reservas</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'reservas' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {reservations.length}
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('facturas'); loadData(); }}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'facturas'
                ? 'border-[#F5A623] text-[#17191C]'
                : 'border-transparent text-neutral-500 hover:text-neutral-800'
            }`}
          >
            <span>Facturas y Pagos</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
              activeTab === 'facturas' ? 'bg-[#17191C] text-white' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {invoices.length}
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
            PESTAÑA 1: MIS RESERVAS
           ========================================================================= */}
        {activeTab === 'reservas' && (
          <div>
            {/* Barra de Filtro para Reservas */}
            {reservations.length > 0 && (
              <div className="bg-white rounded-xl border border-neutral-200 p-3 mb-6 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
                <div className="relative w-full sm:w-80">
                  <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Buscar por equipo o ID de reserva..."
                    value={reservationSearch}
                    onChange={(e) => setReservationSearch(e.target.value)}
                    className="w-full pl-9 pr-3 py-1.5 text-xs bg-neutral-50 border border-neutral-200 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                  />
                </div>
                <div className="text-xs text-neutral-500 font-medium self-end sm:self-auto">
                  Mostrando {filteredReservations.length} de {reservations.length} reservas
                </div>
              </div>
            )}

            {loading ? (
              <div className="space-y-4">
                {[1, 2, 3].map((n) => (
                  <div key={n} className="bg-white rounded-xl border border-neutral-200 p-6 animate-pulse">
                    <div className="h-4 bg-neutral-200 rounded w-1/4 mb-3" />
                    <div className="h-3 bg-neutral-100 rounded w-1/2 mb-4" />
                    <div className="h-8 bg-neutral-200 rounded w-1/5" />
                  </div>
                ))}
              </div>
            ) : reservations.length === 0 ? (
              <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <div className="w-12 h-12 rounded-xl bg-amber-50 text-[#F5A623] flex items-center justify-center mx-auto mb-3">
                  <Calendar className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-[#17191C]">Aún no tienes alquileres registrados</h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 mb-5 leading-relaxed">
                  Explora el catálogo de maquinaria pesada, herramientas eléctricas y andamiajes de OBRIXEN para cotizar y reservar en línea.
                </p>
                {onNavigateCatalog && (
                  <button
                    onClick={onNavigateCatalog}
                    className="px-5 py-2.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-bold text-xs rounded-lg shadow-xs transition-colors cursor-pointer inline-flex items-center gap-2"
                  >
                    <span>Explorar catálogo</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                )}
              </div>
            ) : filteredReservations.length === 0 ? (
              <div className="text-center py-12 px-4 bg-white rounded-xl border border-neutral-200">
                <p className="text-xs text-neutral-500">No hay reservas que coincidan con "{reservationSearch}".</p>
                <button
                  onClick={() => setReservationSearch('')}
                  className="text-xs font-bold text-[#F5A623] hover:underline mt-2 cursor-pointer"
                >
                  Limpiar búsqueda
                </button>
              </div>
            ) : (
              <div>
                {/* Vista Escritorio (Tabla refinada) */}
                <div className="hidden md:block bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-neutral-200 bg-neutral-50 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                        <th className="py-3.5 px-4">ID</th>
                        <th className="py-3.5 px-4">Equipo / Notas</th>
                        <th className="py-3.5 px-4">Periodo de Obra</th>
                        <th className="py-3.5 px-4 text-right">Días</th>
                        <th className="py-3.5 px-4 text-right">Total (COP)</th>
                        <th className="py-3.5 px-4">Estado</th>
                        <th className="py-3.5 px-4 text-right">Acciones</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {filteredReservations.map((r) => {
                        const invoice = invoices.find(inv => inv.reservaId === r.id);
                        const canPay = (r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada;
                        const canCancel = r.estado === 'PENDIENTE' || ((r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada);
                        const canReturn = (r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA') && !r.solicitudDevolucion && r.estadoDevolucion !== 'PENDIENTE_REVISION';
                        const canRefund = ['CONFIRMADA', 'PAGADA', 'EN_CURSO', 'ENTREGADA', 'FINALIZADA', 'DEVUELTA'].includes(r.estado) && invoice;

                        return (
                          <tr key={r.id} className="hover:bg-neutral-50/80 transition-colors">
                            <td className="py-3.5 px-4 font-mono font-bold text-neutral-600">
                              #{r.id}
                            </td>
                            <td className="py-3.5 px-4">
                              <span className="font-bold text-[#17191C] block max-w-xs truncate" title={(r.herramientaId && toolMap[r.herramientaId]) || r.herramientaNombre || 'Equipo de Construcción'}>
                                {(r.herramientaId && toolMap[r.herramientaId]) || r.herramientaNombre || 'Equipo de Construcción'}
                              </span>
                              {r.notasCliente && (
                                <span className="text-[11px] text-neutral-500 block max-w-xs truncate mt-0.5">
                                  Nota: {r.notasCliente}
                                </span>
                              )}
                            </td>
                            <td className="py-3.5 px-4 font-mono text-neutral-700 whitespace-nowrap">
                              <span>{r.fechaInicio}</span>
                              <span className="text-neutral-400 mx-1.5">&rarr;</span>
                              <span>{r.fechaFin}</span>
                            </td>
                            <td className="py-3.5 px-4 text-right font-mono tabular-nums text-neutral-800">
                              {r.dias}d
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              <span className="font-mono font-bold text-[#17191C] tabular-nums block">
                                ${r.total.toLocaleString('es-CO')}
                              </span>
                              <span className="text-[10px] text-neutral-400 block">IVA 19% incl.</span>
                            </td>
                            <td className="py-3.5 px-4">
                              {renderStatusBadge(r)}
                            </td>
                            <td className="py-3.5 px-4 text-right whitespace-nowrap space-x-1.5">
                              {canPay && (
                                <button
                                  onClick={() => onPayReservation(r)}
                                  className="px-3 py-1.5 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                                >
                                  <CreditCard className="w-3.5 h-3.5" />
                                  <span>Pagar</span>
                                </button>
                              )}

                              {canReturn && (
                                <button
                                  onClick={() => {
                                    setReturnConfirmResv(r);
                                    setReturnClientNote('');
                                  }}
                                  className="px-2.5 py-1.5 text-xs font-semibold text-neutral-800 bg-amber-100 hover:bg-amber-200 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1"
                                  title="Notificar devolución al propietario para inspección"
                                >
                                  <RotateCcw className="w-3.5 h-3.5 text-[#F5A623]" />
                                  <span>Devolver</span>
                                </button>
                              )}

                              {invoice && (
                                <button
                                  onClick={() => handleDownloadPdf(invoice)}
                                  disabled={downloadingInvoiceId === invoice.id}
                                  className="px-2.5 py-1.5 text-xs font-semibold text-neutral-700 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                                  title="Descargar factura PDF con IVA 19%"
                                >
                                  <FileDown className="w-3.5 h-3.5 text-[#F5A623]" />
                                  <span>{downloadingInvoiceId === invoice.id ? 'Descargando...' : 'Factura PDF'}</span>
                                </button>
                              )}

                              {canCancel && (
                                <button
                                  onClick={() => {
                                    setCancelConfirmResv(r);
                                    setCancelReason('');
                                  }}
                                  className="px-2.5 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1"
                                  title="Cancelar solicitud de alquiler"
                                >
                                  <XCircle className="w-3.5 h-3.5 text-rose-600" />
                                  <span>Cancelar</span>
                                </button>
                              )}

                              {canRefund && (
                                <button
                                  onClick={() => {
                                    setSelectedRefundResv(r);
                                    setRefundReason('');
                                  }}
                                  className="px-2 py-1 text-xs font-medium text-neutral-500 hover:text-neutral-800 underline cursor-pointer"
                                  title="Solicitar reembolso justificado"
                                >
                                  Reembolso
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Vista Móvil (Tarjetas táctiles sin scroll horizontal) */}
                <div className="md:hidden space-y-4">
                  {filteredReservations.map((r) => {
                    const invoice = invoices.find(inv => inv.reservaId === r.id);
                    const canPay = (r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada;
                    const canCancel = r.estado === 'PENDIENTE' || ((r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada);
                    const canReturn = (r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA') && !r.solicitudDevolucion && r.estadoDevolucion !== 'PENDIENTE_REVISION';
                    const canRefund = ['CONFIRMADA', 'PAGADA', 'EN_CURSO', 'ENTREGADA', 'FINALIZADA', 'DEVUELTA'].includes(r.estado) && invoice;

                    return (
                      <div
                        key={r.id}
                        className="bg-white rounded-xl border border-neutral-200 p-4 shadow-xs flex flex-col justify-between"
                      >
                        {/* Cabecera de la tarjeta */}
                        <div className="flex items-center justify-between gap-2 pb-3 border-b border-neutral-100">
                          <span className="font-mono text-xs font-bold text-neutral-500">#{r.id}</span>
                          <div>{renderStatusBadge(r)}</div>
                        </div>

                        {/* Contenido del equipo */}
                        <div className="py-3">
                          <h4 className="text-sm font-bold text-[#17191C] mb-1">
                            {(r.herramientaId && toolMap[r.herramientaId]) || r.herramientaNombre || 'Equipo de Construcción'}
                          </h4>
                          {r.notasCliente && (
                            <p className="text-xs text-neutral-500 italic mb-2">
                              Nota: {r.notasCliente}
                            </p>
                          )}

                          <div className="grid grid-cols-2 gap-2 text-xs text-neutral-600 bg-neutral-50 p-2.5 rounded-lg border border-neutral-100">
                            <div>
                              <span className="text-[10px] text-neutral-400 block font-medium">Periodo ({r.dias} días)</span>
                              <span className="font-mono text-[11px] font-semibold text-neutral-800">
                                {r.fechaInicio} al {r.fechaFin}
                              </span>
                            </div>
                            <div className="text-right">
                              <span className="text-[10px] text-neutral-400 block font-medium">Total (IVA 19% incl.)</span>
                              <span className="font-mono text-sm font-extrabold text-[#17191C]">
                                ${r.total.toLocaleString('es-CO')}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Botonera de acciones adaptada para pantalla táctil */}
                        <div className="pt-3 border-t border-neutral-100 flex flex-wrap items-center gap-2">
                          {canPay && (
                            <button
                              onClick={() => onPayReservation(r)}
                              className="flex-1 py-2 px-3 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-1.5 shadow-xs"
                            >
                              <CreditCard className="w-4 h-4" />
                              <span>Pagar en Línea</span>
                            </button>
                          )}

                          {canReturn && (
                            <button
                              onClick={() => {
                                setReturnConfirmResv(r);
                                setReturnClientNote('');
                              }}
                              className="flex-1 py-2 px-3 text-xs font-bold text-neutral-900 bg-amber-100 hover:bg-amber-200 rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-1.5"
                            >
                              <RotateCcw className="w-4 h-4 text-[#F5A623]" />
                              <span>Devolver Equipo</span>
                            </button>
                          )}

                          {invoice && (
                            <button
                              onClick={() => handleDownloadPdf(invoice)}
                              disabled={downloadingInvoiceId === invoice.id}
                              className="py-2 px-3 text-xs font-semibold text-neutral-700 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-1 shadow-xs"
                            >
                              <FileDown className="w-4 h-4 text-[#F5A623]" />
                              <span>{downloadingInvoiceId === invoice.id ? 'Descargando...' : 'Factura'}</span>
                            </button>
                          )}

                          {canCancel && (
                            <button
                              onClick={() => {
                                setCancelConfirmResv(r);
                                setCancelReason('');
                              }}
                              className="py-2 px-3 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-lg hover:bg-rose-100 transition-colors cursor-pointer flex items-center justify-center gap-1"
                            >
                              <XCircle className="w-4 h-4 text-rose-600" />
                              <span>Cancelar</span>
                            </button>
                          )}

                          {canRefund && (
                            <button
                              onClick={() => {
                                setSelectedRefundResv(r);
                                setRefundReason('');
                              }}
                              className="py-2 px-2 text-xs font-medium text-neutral-500 hover:text-neutral-800 underline cursor-pointer"
                            >
                              Reembolso
                            </button>
                          )}
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
            PESTAÑA 2: FACTURAS Y PAGOS
           ========================================================================= */}
        {activeTab === 'facturas' && (
          <div>
            <div className="bg-white rounded-xl border border-neutral-200 p-4 mb-6 shadow-xs flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-[#17191C] flex items-center gap-2">
                  <Receipt className="w-4 h-4 text-[#F5A623]" />
                  <span>Comprobantes Oficiales de Facturación Electrónica</span>
                </h3>
                <p className="text-xs text-neutral-500 mt-0.5">
                  Descarga tus facturas legales en formato PDF emitidas con IVA del 19 % desglosado en pesos colombianos (COP).
                </p>
              </div>
            </div>

            {loading ? (
              <div className="space-y-4">
                {[1, 2].map((n) => (
                  <div key={n} className="bg-white rounded-xl border border-neutral-200 p-6 animate-pulse">
                    <div className="h-4 bg-neutral-200 rounded w-1/3 mb-2" />
                    <div className="h-3 bg-neutral-100 rounded w-1/2" />
                  </div>
                ))}
              </div>
            ) : invoices.length === 0 ? (
              <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <Receipt className="w-12 h-12 text-neutral-400 mx-auto mb-3" />
                <h3 className="text-base font-bold text-[#17191C]">No tienes facturas emitidas todavía</h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 mb-4 leading-relaxed">
                  Las facturas oficiales se generan automáticamente una vez completado el pago de tus reservas aprobadas.
                </p>
                {onNavigateCatalog && (
                  <button
                    onClick={onNavigateCatalog}
                    className="px-4 py-2 bg-[#17191C] hover:bg-neutral-800 text-white text-xs font-bold rounded-lg transition-colors cursor-pointer"
                  >
                    Ver catálogo de equipos
                  </button>
                )}
              </div>
            ) : (
              <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-neutral-200 bg-neutral-50 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                        <th className="py-3.5 px-4">Comprobante y Estado</th>
                        <th className="py-3.5 px-4">Concepto y Detalle del Cargo</th>
                        <th className="py-3.5 px-4">Reserva</th>
                        <th className="py-3.5 px-4">Fecha Emisión</th>
                        <th className="py-3.5 px-4 text-right">Importe (COP)</th>
                        <th className="py-3.5 px-4 text-right">Acción</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {invoices.map((inv) => {
                        const isReparacion = inv.tipo === 'REPARACION';
                        const isPendiente = inv.estado === 'PENDIENTE';
                        const toolName = inv.herramientaNombre || (inv.reservaId && toolMap[inv.reservaId]) || 'Herramienta';
                        const title = isReparacion
                          ? `Costos de reparación de ${toolName}`
                          : (inv.concepto || `Alquiler de ${toolName}`);

                        return (
                          <tr
                            key={inv.id}
                            className={`transition-colors ${isReparacion && isPendiente ? 'bg-amber-50/50 hover:bg-amber-50/80 border-l-4 border-l-amber-500' : 'hover:bg-neutral-50/80'}`}
                          >
                            <td className="py-4 px-4 align-top">
                              <span className="font-mono font-bold text-[#17191C] block text-xs">
                                {inv.numeroFactura}
                              </span>
                              <div className="flex flex-wrap items-center gap-1.5 mt-1">
                                {isReparacion ? (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                                    REPARACIÓN
                                  </span>
                                ) : inv.tipo === 'NOTA_CREDITO' ? (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800 border border-purple-200">
                                    NOTA CRÉDITO
                                  </span>
                                ) : (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
                                    ALQUILER
                                  </span>
                                )}

                                {isPendiente ? (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500 text-slate-950 flex items-center gap-1">
                                    <Clock className="w-3 h-3" />
                                    <span>PENDIENTE</span>
                                  </span>
                                ) : (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 flex items-center gap-1 border border-emerald-200">
                                    <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                                    <span>PAGADA</span>
                                  </span>
                                )}
                              </div>
                            </td>

                            <td className="py-4 px-4 align-top max-w-sm">
                              <span className="font-bold text-neutral-900 block text-xs leading-snug">
                                {title}
                              </span>
                              <span className="text-[11px] text-neutral-500 block mt-0.5">
                                Herramienta: <strong className="text-neutral-700">{toolName}</strong>
                              </span>
                              {isReparacion && inv.descripcionDano && (
                                <div className="mt-2 p-2.5 rounded-lg bg-amber-100/70 border border-amber-300 text-amber-950 text-[11px] leading-relaxed">
                                  <span className="font-bold text-amber-900 block mb-0.5 flex items-center gap-1">
                                    <Wrench className="w-3 h-3" />
                                    <span>Descripción del daño registrada por el propietario:</span>
                                  </span>
                                  <span>{inv.descripcionDano}</span>
                                </div>
                              )}
                            </td>

                            <td className="py-4 px-4 align-top font-mono text-neutral-600 text-xs">
                              <span className="bg-neutral-100 px-2 py-1 rounded text-neutral-800 font-semibold">
                                #{inv.reservaId}
                              </span>
                            </td>

                            <td className="py-4 px-4 align-top text-neutral-600 text-xs">
                              {inv.fechaEmision
                                ? new Date(inv.fechaEmision).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })
                                : '—'}
                            </td>

                            <td className="py-4 px-4 align-top text-right font-mono tabular-nums">
                              <span className="font-extrabold text-[#17191C] text-sm block">
                                ${inv.total.toLocaleString('es-CO')} COP
                              </span>
                              {inv.iva > 0 && (
                                <span className="text-[10px] text-neutral-500 block">
                                  IVA: ${inv.iva.toLocaleString('es-CO')}
                                </span>
                              )}
                            </td>

                            <td className="py-4 px-4 align-top text-right">
                              {isReparacion && isPendiente ? (
                                <button
                                  onClick={() => setPayingInvoice(inv)}
                                  className="px-4 py-2 text-xs font-extrabold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1.5 shadow-sm active:scale-95"
                                >
                                  <CreditCard className="w-3.5 h-3.5" />
                                  <span>Pagar</span>
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleDownloadPdf(inv)}
                                  disabled={downloadingInvoiceId === inv.id}
                                  className="px-3 py-1.5 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1.5 shadow-xs"
                                >
                                  <FileDown className="w-3.5 h-3.5" />
                                  <span>{downloadingInvoiceId === inv.id ? 'Descargando...' : 'Descargar PDF'}</span>
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* =========================================================================
            PESTAÑA 3: GARANTÍAS Y REEMBOLSOS
           ========================================================================= */}
        {activeTab === 'reembolsos' && (
          <div>
            <div className="bg-white rounded-xl border border-neutral-200 p-4 mb-6 shadow-xs">
              <h3 className="text-sm font-bold text-[#17191C] flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-[#F5A623]" />
                <span>Gestión de Garantías y Reembolsos Radicados</span>
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">
                Las solicitudes son revisadas por el área de operaciones técnicas ante contingencias en obra o cancelaciones justificadas.
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
                <h3 className="text-base font-bold text-[#17191C]">No tienes solicitudes de reembolso radicadas</h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 leading-relaxed">
                  Si presentas algún inconveniente justificado con un equipo entregado o una cancelación fortuita, puedes solicitar el reembolso desde la pestaña de "Mis Reservas".
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {refunds.map((ref) => (
                  <div
                    key={ref.id}
                    className="p-4 bg-white rounded-xl border border-neutral-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                  >
                    <div>
                      <div className="flex items-center gap-2 font-mono text-xs text-neutral-600 mb-1">
                        <span className="font-bold text-[#17191C]">Solicitud #{ref.id}</span>
                        <span>·</span>
                        <span>Reserva #{ref.reservaId}</span>
                        <span>·</span>
                        <strong className="text-[#17191C]">${ref.monto.toLocaleString('es-CO')} COP</strong>
                      </div>
                      <p className="text-xs text-neutral-700 italic">
                        "{ref.motivo}"
                      </p>
                      {ref.resolucion && (
                        <div className="mt-2 text-xs bg-neutral-50 p-2 rounded-md border border-neutral-200 text-neutral-700">
                          <strong>Resolución técnica:</strong> {ref.resolucion}
                        </div>
                      )}
                    </div>

                    <div className="shrink-0">
                      <span className={`px-3 py-1 rounded-md font-bold text-xs ${
                        ref.estado === 'APROBADO' || (ref.estado as string) === 'REEMBOLSADO' ? 'bg-emerald-100 text-emerald-800' :
                        ref.estado === 'RECHAZADO' || (ref.estado as string) === 'FALLIDO' ? 'bg-rose-100 text-rose-800' :
                        'bg-amber-100 text-amber-800'
                      }`}>
                        {ref.estado === 'APROBADO' || (ref.estado as string) === 'REEMBOLSADO' ? 'APROBADO' :
                         ref.estado === 'RECHAZADO' || (ref.estado as string) === 'FALLIDO' ? 'RECHAZADO' :
                         ref.estado}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* =========================================================================
          4. MODALES CONTEXTUALES REDISEÑADOS Y ACCESIBLES
         ========================================================================= */}

      {/* Modal 1: Cancelar Reserva */}
      {cancelConfirmResv && (
        <div
          className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
          aria-labelledby="cancel-modal-title"
        >
          <div className="bg-white rounded-2xl max-w-md w-full border border-neutral-200 shadow-2xl p-6">
            <div className="w-11 h-11 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
              <XCircle className="w-6 h-6" />
            </div>
            <h3 id="cancel-modal-title" className="text-base font-bold text-[#17191C] text-center mb-1">
              Cancelar Reserva #{cancelConfirmResv.id}
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              ¿Estás seguro de cancelar la solicitud para <strong>{cancelConfirmResv.herramientaNombre}</strong>? Se liberará la disponibilidad del equipo.
            </p>

            <form onSubmit={handleCancelReservation}>
              <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                Motivo de la cancelación <span className="text-rose-500">*</span>
              </label>
              <textarea
                required
                rows={3}
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Indica la razón de la cancelación (ej. cambio en el cronograma de obra, lluvia prolongada...)"
                className="w-full text-xs p-3 bg-neutral-50 border border-neutral-300 rounded-lg mb-4 focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
              />

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingCancel}
                  onClick={() => setCancelConfirmResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Regresar
                </button>
                <button
                  type="submit"
                  disabled={submittingCancel}
                  className="px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingCancel ? 'Cancelando...' : 'Confirmar Cancelación'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 2: Notificar Devolución de Maquinaria */}
      {returnConfirmResv && (
        <div
          className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
          aria-labelledby="return-modal-title"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full border border-neutral-200 shadow-2xl p-6">
            <div className="w-11 h-11 rounded-xl bg-amber-50 text-[#F5A623] flex items-center justify-center mx-auto mb-3">
              <RotateCcw className="w-6 h-6" />
            </div>
            <h3 id="return-modal-title" className="text-base font-bold text-[#17191C] text-center mb-1">
              Registrar Retorno de Maquinaria
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              Notifica al propietario que has concluido el uso de <strong>{returnConfirmResv.herramientaNombre}</strong> y entregas el equipo para su inspección técnica.
            </p>

            <div className="bg-neutral-50 p-3 rounded-xl border border-neutral-200 text-xs text-neutral-700 font-mono mb-4 space-y-1">
              <div className="flex justify-between">
                <span className="text-neutral-500">Reserva:</span>
                <span className="font-bold text-[#17191C]">#{returnConfirmResv.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Periodo de Obra:</span>
                <span>{returnConfirmResv.fechaInicio} al {returnConfirmResv.fechaFin}</span>
              </div>
            </div>

            <form onSubmit={handleReturnEquipment}>
              <div className="mb-4">
                <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                  Informe o Novedades del Equipo <span className="text-neutral-400 font-normal">(Opcional)</span>
                </label>
                <textarea
                  rows={3}
                  value={returnClientNote}
                  onChange={(e) => setReturnClientNote(e.target.value)}
                  placeholder="Ej: El equipo funcionó con normalidad. Se entrega limpio y con todos los accesorios completos..."
                  className="w-full p-2.5 text-xs bg-neutral-50 border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
                />
                <span className="text-[11px] text-neutral-500 block mt-1">
                  Este informe será revisado durante el acta de inspección y recepción técnica.
                </span>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingReturn}
                  onClick={() => setReturnConfirmResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingReturn}
                  className="px-4 py-2 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingReturn ? 'Enviando...' : 'Enviar Devolución'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 3: Solicitar Reembolso */}
      {selectedRefundResv && (
        <div
          className="fixed inset-0 z-50 bg-[#17191C]/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
          aria-labelledby="refund-modal-title"
        >
          <div className="bg-white rounded-2xl max-w-md w-full border border-neutral-200 shadow-2xl p-6">
            <div className="w-11 h-11 rounded-xl bg-amber-50 text-[#F5A623] flex items-center justify-center mx-auto mb-3">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 id="refund-modal-title" className="text-base font-bold text-[#17191C] text-center mb-1">
              Solicitud de Reembolso
            </h3>
            <p className="text-xs text-neutral-600 text-center mb-4 leading-relaxed">
              Radica una solicitud justificada de reembolso para la reserva <strong>#{selectedRefundResv.id}</strong> ({selectedRefundResv.herramientaNombre}).
            </p>

            <form onSubmit={handleSubmitRefund}>
              <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                Causa o Motivo Justificado <span className="text-rose-500">*</span>
              </label>
              <textarea
                required
                rows={3}
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                placeholder="Describe el inconveniente en obra, retraso justificado o motivo de fuerza mayor..."
                className="w-full text-xs p-3 bg-neutral-50 border border-neutral-300 rounded-lg mb-4 focus:outline-none focus:border-[#F5A623] focus:bg-white text-neutral-900"
              />

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingRefund}
                  onClick={() => setSelectedRefundResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-600 hover:bg-neutral-100 rounded-lg cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingRefund}
                  className="px-4 py-2 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg transition-colors cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submittingRefund ? 'Radicando...' : 'Radicar Solicitud'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal para pagar factura de reparación */}
      {payingInvoice && (
        <CheckoutPaymentModal
          invoice={payingInvoice}
          onClose={() => setPayingInvoice(null)}
          onPaymentSuccess={() => {
            setPayingInvoice(null);
            toast.success('Pago de costos de reparación procesado exitosamente', 'Factura Pagada');
            loadData();
          }}
        />
      )}
    </div>
  );
};
