import React, { useState, useEffect } from 'react';
import { Reservation, Invoice, RefundRequest } from '../types';
import { api } from '../services/api';
import { Calendar, CreditCard, FileDown, RotateCcw, CheckCircle2, Clock, XCircle, AlertTriangle } from 'lucide-react';
import { useToast } from '../context/ToastContext';

interface ClientDashboardProps {
  onPayReservation: (reservation: Reservation) => void;
  onViewInvoice: (invoiceId: number) => void;
}

export const ClientDashboard: React.FC<ClientDashboardProps> = ({
  onPayReservation,
  onViewInvoice
}) => {
  const { toast } = useToast();
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [refunds, setRefunds] = useState<RefundRequest[]>([]);
  const [loading, setLoading] = useState(true);

  // Refund modal state
  const [selectedRefundResv, setSelectedRefundResv] = useState<Reservation | null>(null);
  const [refundReason, setRefundReason] = useState('');
  const [submittingRefund, setSubmittingRefund] = useState(false);

  // Return modal state for client
  const [returnConfirmResv, setReturnConfirmResv] = useState<Reservation | null>(null);
  const [submittingReturn, setSubmittingReturn] = useState(false);
  const [returnClientNote, setReturnClientNote] = useState("");

  // Cancel reservation modal state
  const [cancelConfirmResv, setCancelConfirmResv] = useState<Reservation | null>(null);
  const [submittingCancel, setSubmittingCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  const handleCancelReservation = async () => {
    if (!cancelConfirmResv) return;
    try {
      setSubmittingCancel(true);
      await api.cancelReservation(cancelConfirmResv.id, cancelReason);
      toast.success('Reserva cancelada con éxito. Se ha liberado la disponibilidad del equipo.', 'Reserva Cancelada');
      setCancelConfirmResv(null);
      setCancelReason('');
      await loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error al cancelar la reserva', 'Fallo de Cancelación');
    } finally {
      setSubmittingCancel(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const [resvs, invs, refs] = await Promise.all([
        api.getReservations(),
        api.getInvoices(),
        api.getRefunds()
      ]);
      setReservations(resvs);
      setInvoices(invs);
      setRefunds(refs);
    } catch (e) {
      console.error('Error cargando panel cliente:', e);
    } finally {
      setLoading(false);
    }
  }

  const handleDownloadPdf = async (inv: Invoice) => {
    try {
      await api.downloadInvoicePdf(inv.id, inv.numeroFactura);
    } catch (e) {
      toast.error('Error descargando el comprobante PDF', 'Fallo de Descarga');
    }
  };

  const handleOpenRefund = (r: Reservation) => {
    setSelectedRefundResv(r);
    setRefundReason('');
  };

  const handleSubmitRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRefundResv) return;

    // Find invoice and payment
    const inv = invoices.find(f => f.reservaId === selectedRefundResv.id);
    if (!inv) {
      toast.error('No se localizó el registro de pago para esta reserva', 'Registro no encontrado');
      return;
    }

    setSubmittingRefund(true);
    try {
      await api.requestRefund(inv.pagoId || selectedRefundResv.id, refundReason);
      toast.success('Solicitud de reembolso registrada correctamente. El propietario o administrador la evaluará.', 'Reembolso Solicitado');
      setSelectedRefundResv(null);
      loadData();
    } catch (err: any) {
      toast.error(err.message || 'Error solicitando reembolso', 'Fallo en Solicitud');
    } finally {
      setSubmittingRefund(false);
    }
  };

  const renderStatus = (r: Reservation) => {
    const estado = r.estado;
    switch (estado) {
      case 'PENDIENTE':
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-amber-700">
            <Clock className="w-3.5 h-3.5 text-amber-500" />
            <span>Pendiente Aprobación</span>
          </span>
        );
      case 'CONFIRMADA':
      case 'ACEPTADA':
        if (r.pagada) {
          return (
            <span className="flex items-center gap-1.5 text-xs font-medium text-emerald-700">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
              <span>Pagada (Preparando Despacho)</span>
            </span>
          );
        }
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-blue-700">
            <CheckCircle2 className="w-3.5 h-3.5 text-blue-500" />
            <span>Confirmada (Pendiente de Pago)</span>
          </span>
        );
      case 'EN_CURSO':
      case 'ENTREGADA':
        if (r.solicitudDevolucion || r.estadoDevolucion === 'PENDIENTE_REVISION') {
          return (
            <div>
              <span className="flex items-center gap-1.5 text-xs font-semibold text-amber-700">
                <Clock className="w-3.5 h-3.5 text-amber-500" />
                <span>Devolución Notificada (En Espera de Inspección)</span>
              </span>
              {r.notaDevolucionCliente && (
                <span className="text-[11px] text-neutral-500 block italic truncate max-w-xs mt-0.5">
                  Informe: "{r.notaDevolucionCliente}"
                </span>
              )}
            </div>
          );
        }
        if (r.estadoDevolucion === 'RECHAZADA_POR_DANOS') {
          return (
            <div>
              <span className="flex items-center gap-1.5 text-xs font-semibold text-rose-700">
                <AlertTriangle className="w-3.5 h-3.5 text-rose-500" />
                <span>Retorno Observado por Daños</span>
              </span>
              {r.costoReparacion && r.costoReparacion > 0 ? (
                <span className="text-[11px] text-rose-600 block font-mono font-bold mt-0.5">
                  Cargos por avería: ${r.costoReparacion.toLocaleString('es-CO')} COP
                </span>
              ) : null}
            </div>
          );
        }
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-purple-700">
            <Clock className="w-3.5 h-3.5 text-purple-500" />
            <span>En Obra (Alquilada)</span>
          </span>
        );
      case 'PAGADA':
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-emerald-700">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            <span>Pagada (Por Despachar)</span>
          </span>
        );
      case 'FINALIZADA':
      case 'DEVUELTA':
        if (r.costoReparacion && r.costoReparacion > 0) {
          return (
            <div>
              <span className="flex items-center gap-1.5 text-xs font-semibold text-amber-800">
                <CheckCircle2 className="w-3.5 h-3.5 text-amber-600" />
                <span>Devuelta (con Cobro de Daños)</span>
              </span>
              <span className="text-[11px] text-amber-700 font-mono block mt-0.5">
                Reparación: ${r.costoReparacion.toLocaleString('es-CO')} COP
              </span>
            </div>
          );
        }
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-emerald-700">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            <span>Devuelta en Buen Estado</span>
          </span>
        );
      case 'CANCELADA':
      case 'EXPIRADA':
      case 'RECHAZADA':
        return (
          <span className="flex items-center gap-1.5 text-xs font-medium text-rose-700">
            <XCircle className="w-3.5 h-3.5 text-rose-500" />
            <span>{estado}</span>
          </span>
        );
      default:
        return <span className="text-xs text-neutral-500">{estado}</span>;
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="border-b border-neutral-200 pb-6 mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold tracking-wider uppercase text-amber-600 block mb-1">
            Portal del Cliente / Contratista
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Mis Alquileres, Pagos y Facturación
          </h1>
        </div>
        <div className="flex items-center gap-3 text-xs font-mono text-neutral-500 bg-neutral-100 px-3 py-1.5 rounded-lg border border-neutral-200">
          <span>Total Reservas: <strong className="text-neutral-900 tabular-nums">{reservations.length}</strong></span>
          <span>·</span>
          <span>Facturas Emitidas: <strong className="text-neutral-900 tabular-nums">{invoices.length}</strong></span>
        </div>
      </div>

      {loading ? (
        <div className="p-12 text-center text-sm text-neutral-500">Cargando información del cliente...</div>
      ) : reservations.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-neutral-200">
          <Calendar className="w-12 h-12 text-neutral-400 mx-auto mb-3" />
          <h3 className="text-sm font-bold text-neutral-900 mb-1">Aún no tiene reservas registradas</h3>
          <p className="text-xs text-neutral-500 mb-4">Explore nuestro catálogo para seleccionar herramientas y maquinaria certificada.</p>
        </div>
      ) : (
        <div className="space-y-8">
          {/* Active / Current Rentals Table */}
          <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
            <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
              <h2 className="text-sm font-bold text-neutral-900">
                Historial de Reservas y Solicitudes de Alquiler
              </h2>
              <span className="text-xs text-neutral-500">Moneda: COP · Tasa IVA 19%</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                    <th className="py-3 px-4">ID</th>
                    <th className="py-3 px-4">Equipo</th>
                    <th className="py-3 px-4">Periodo [Inicio - Fin)</th>
                    <th className="py-3 px-4 text-right">Días</th>
                    <th className="py-3 px-4 text-right">Total (COP)</th>
                    <th className="py-3 px-4">Estado</th>
                    <th className="py-3 px-4 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {reservations.map((r) => {
                    const invoice = invoices.find(inv => inv.reservaId === r.id);
                    const canPay = (r.estado === 'CONFIRMADA' || r.estado === 'ACEPTADA') && !r.pagada;
                    const canRefund = ['CONFIRMADA', 'PAGADA', 'EN_CURSO', 'ENTREGADA', 'FINALIZADA', 'DEVUELTA'].includes(r.estado) && invoice;

                    return (
                      <tr key={r.id} className="hover:bg-neutral-50/80 transition-colors">
                        <td className="py-3.5 px-4 font-mono text-neutral-500">#{r.id}</td>
                        <td className="py-3.5 px-4">
                          <span className="font-bold text-neutral-900 block truncate max-w-xs">{r.herramientaNombre}</span>
                          {r.notasCliente && (
                            <span className="text-[11px] text-neutral-500 truncate max-w-xs block">
                              Nota: {r.notasCliente}
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 font-mono text-neutral-700">
                          {r.fechaInicio} al {r.fechaFin}
                        </td>
                        <td className="py-3.5 px-4 text-right font-mono tabular-nums text-neutral-800">
                          {r.dias}d
                        </td>
                        <td className="py-3.5 px-4 text-right font-mono tabular-nums font-bold text-neutral-900">
                          ${r.total.toLocaleString('es-CO')}
                        </td>
                        <td className="py-3.5 px-4">
                          {renderStatus(r)}
                        </td>
                        <td className="py-3.5 px-4 text-right whitespace-nowrap space-x-2">
                          {canPay && (
                            <button
                              onClick={() => onPayReservation(r)}
                              className="px-3 py-1.5 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                            >
                              <CreditCard className="w-3.5 h-3.5" />
                              <span>Pagar en Línea</span>
                            </button>
                          )}

                          {(r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA') && (
                            <button
                              onClick={() => {
                                setCancelConfirmResv(r);
                                setCancelReason('');
                              }}
                              className="px-2.5 py-1 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 hover:bg-rose-100 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                              title="Cancelar solicitud o reserva de equipo"
                            >
                              <XCircle className="w-3.5 h-3.5 text-rose-600" />
                              <span>Cancelar</span>
                            </button>
                          )}

                          {invoice && (
                            <button
                              onClick={() => handleDownloadPdf(invoice)}
                              className="px-2.5 py-1 text-xs font-semibold text-neutral-700 bg-white border border-neutral-300 hover:bg-neutral-50 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                              title="Descargar PDF Factura"
                            >
                              <FileDown className="w-3.5 h-3.5 text-amber-600" />
                              <span>PDF Factura</span>
                            </button>
                          )}

                          {(r.estado === 'EN_CURSO' || r.estado === 'ENTREGADA') && !r.solicitudDevolucion && r.estadoDevolucion !== 'PENDIENTE_REVISION' && (
                            <button
                              onClick={() => {
                                setReturnConfirmResv(r);
                                setReturnClientNote('');
                              }}
                              className="px-2.5 py-1 text-xs font-semibold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1 shadow-xs"
                              title="Notificar devolución al propietario"
                            >
                              <RotateCcw className="w-3.5 h-3.5" />
                              <span>Devolver Equipo</span>
                            </button>
                          )}

                          {canRefund && (
                            <button
                              onClick={() => handleOpenRefund(r)}
                              className="px-2 py-1 text-xs font-medium text-neutral-600 hover:text-neutral-900 underline cursor-pointer"
                              title="Solicitar reembolso por cancelación justificada"
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
          </div>

          {/* Invoices and PDF Downloads */}
          {invoices.length > 0 && (
            <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden shadow-xs">
              <div className="px-6 py-4 border-b border-neutral-200 bg-neutral-50 flex items-center justify-between">
                <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <FileDown className="w-4 h-4 text-amber-600" />
                  <span>Facturas Comerciales Emitidas</span>
                </h3>
                <span className="text-xs text-neutral-500">Comprobantes válidos con desglose de IVA (19%)</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-neutral-200 bg-neutral-100/60 text-neutral-600 font-semibold uppercase tracking-wider text-[11px]">
                      <th className="py-3 px-4">Nº Factura</th>
                      <th className="py-3 px-4">Reserva Asociada</th>
                      <th className="py-3 px-4">Emisor</th>
                      <th className="py-3 px-4 text-right">Subtotal</th>
                      <th className="py-3 px-4 text-right">IVA 19%</th>
                      <th className="py-3 px-4 text-right">Total (COP)</th>
                      <th className="py-3 px-4 text-right">Acción</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {invoices.map((inv) => (
                      <tr key={inv.id} className="hover:bg-neutral-50/80 transition-colors">
                        <td className="py-3 px-4 font-mono font-bold text-neutral-900">{inv.numeroFactura}</td>
                        <td className="py-3 px-4 font-mono text-neutral-600">#{inv.reservaId}</td>
                        <td className="py-3 px-4 text-neutral-700">{inv.propietarioNombre || 'Proveedor Maquinaria'}</td>
                        <td className="py-3 px-4 text-right font-mono tabular-nums text-neutral-700">${inv.subtotal.toLocaleString('es-CO')}</td>
                        <td className="py-3 px-4 text-right font-mono tabular-nums text-neutral-700">${inv.iva.toLocaleString('es-CO')}</td>
                        <td className="py-3 px-4 text-right font-mono tabular-nums font-bold text-neutral-900">${inv.total.toLocaleString('es-CO')}</td>
                        <td className="py-3 px-4 text-right">
                          <button
                            onClick={() => handleDownloadPdf(inv)}
                            className="px-3 py-1 text-xs font-semibold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg inline-flex items-center gap-1 cursor-pointer shadow-xs"
                          >
                            <FileDown className="w-3.5 h-3.5" />
                            <span>Descargar PDF</span>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Refund Requests History */}
          {refunds.length > 0 && (
            <div className="bg-white rounded-xl border border-neutral-200 p-6 shadow-xs">
              <h3 className="text-sm font-bold text-neutral-900 mb-4 flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-amber-600" />
                <span>Solicitudes de Reembolso Registradas</span>
              </h3>
              <div className="space-y-3">
                {refunds.map((ref) => (
                  <div key={ref.id} className="p-3.5 bg-neutral-50 rounded-xl border border-neutral-200 flex items-center justify-between text-xs">
                    <div>
                      <div className="flex items-center gap-2 font-mono text-neutral-600 mb-1">
                        <span>Solicitud #{ref.id}</span>
                        <span>·</span>
                        <span>Reserva #{ref.reservaId}</span>
                        <span>·</span>
                        <strong className="text-neutral-900">${ref.monto.toLocaleString('es-CO')} COP</strong>
                      </div>
                      <p className="text-neutral-700 italic">"{ref.motivo}"</p>
                      {ref.resolucion && (
                        <p className="text-[11px] text-neutral-500 mt-1">Resolución: {ref.resolucion}</p>
                      )}
                    </div>
                    <div>
                      <span className={`px-2.5 py-1 rounded-md font-bold text-[11px] ${
                        ref.estado === 'APROBADO' ? 'bg-emerald-100 text-emerald-800' :
                        ref.estado === 'RECHAZADO' ? 'bg-rose-100 text-rose-800' :
                        'bg-amber-100 text-amber-800'
                      }`}>
                        {ref.estado}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal Solicitar Reembolso */}
      {selectedRefundResv && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200">
            <h3 className="text-sm font-bold text-neutral-900 mb-2">Solicitud de Reembolso</h3>
            <p className="text-xs text-neutral-600 mb-4">
              Indique el motivo justificado de la solicitud para la reserva #{selectedRefundResv.id} ({selectedRefundResv.herramientaNombre}).
            </p>
            <form onSubmit={handleSubmitRefund}>
              <textarea
                required
                rows={3}
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                placeholder="Ej: Falla en obra, no disponibilidad del operador, entrega retrasada..."
                className="w-full text-xs p-3 bg-white border border-neutral-300 rounded-lg mb-4 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedRefundResv(null)}
                  className="px-3 py-1.5 text-xs text-neutral-600 hover:bg-neutral-100 rounded-lg"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submittingRefund}
                  className="px-4 py-1.5 text-xs font-semibold text-white bg-amber-600 hover:bg-amber-700 rounded-lg"
                >
                  {submittingRefund ? 'Enviando...' : 'Radicar Solicitud'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* Client Return Confirmation Modal Card */}
      {returnConfirmResv && (
        <div className="fixed inset-0 z-50 bg-neutral-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-neutral-200 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="p-6">
              <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center mx-auto mb-4">
                <RotateCcw className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-neutral-900 text-center mb-1">
                Registrar Retorno de Maquinaria
              </h3>
              <p className="text-xs text-neutral-600 text-center leading-relaxed mb-4">
                Notifique al propietario que ha concluido el uso de <strong>{returnConfirmResv.herramientaNombre}</strong> y entrega el equipo para su inspección técnica.
              </p>

              <div className="bg-neutral-50 p-3.5 rounded-xl border border-neutral-200 text-xs text-neutral-700 font-mono mb-4 space-y-1">
                <div className="flex justify-between">
                  <span className="text-neutral-500">Reserva:</span>
                  <span className="font-bold text-neutral-900">#{returnConfirmResv.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500">Periodo de Renta:</span>
                  <span>{returnConfirmResv.fechaInicio} al {returnConfirmResv.fechaFin}</span>
                </div>
              </div>

              <div className="mb-5">
                <label className="block text-xs font-semibold text-neutral-800 mb-1.5">
                  Informe o Novedades del Estado del Equipo <span className="text-neutral-400 font-normal">(Opcional)</span>
                </label>
                <textarea
                  rows={3}
                  value={returnClientNote}
                  onChange={(e) => setReturnClientNote(e.target.value)}
                  placeholder="Ej: El equipo funcionó correctamente en obra. Se entrega limpio y con accesorios completos..."
                  className="w-full p-2.5 text-xs bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-1 focus:ring-amber-500 leading-relaxed"
                />
                <span className="text-[11px] text-neutral-500 block mt-1">
                  Este informe será revisado por el propietario durante la inspección de recepción.
                </span>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
                <button
                  type="button"
                  disabled={submittingReturn}
                  onClick={() => setReturnConfirmResv(null)}
                  className="px-4 py-2 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  disabled={submittingReturn}
                  onClick={async () => {
                    setSubmittingReturn(true);
                    try {
                      await api.requestReturn(returnConfirmResv.id, { notaCliente: returnClientNote });
                      toast.success('Devolución notificada al propietario. El equipo queda en espera de inspección técnica.', 'Devolución Notificada');
                      setReturnConfirmResv(null);
                      loadData();
                    } catch (err: any) {
                      toast.error(err.message || 'Error al notificar la devolución');
                    } finally {
                      setSubmittingReturn(false);
                    }
                  }}
                  className="px-4 py-2 text-xs font-bold text-slate-950 bg-amber-400 hover:bg-amber-500 rounded-lg transition-colors cursor-pointer shadow-xs inline-flex items-center gap-1.5"
                >
                  {submittingReturn ? 'Enviando...' : 'Enviar Devolución a Propietario'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
