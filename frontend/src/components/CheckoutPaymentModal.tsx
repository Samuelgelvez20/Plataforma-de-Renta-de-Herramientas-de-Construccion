import React, { useState } from 'react';
import { Reservation, Invoice } from '../types';
import { api } from '../services/api';
import confetti from 'canvas-confetti';
import { X, CreditCard, Building2, CheckCircle2, FileDown, ShieldCheck, AlertCircle } from 'lucide-react';

interface CheckoutPaymentModalProps {
  reservation: Reservation | null;
  onClose: () => void;
  onPaymentSuccess: (invoice: Invoice) => void;
}

export const CheckoutPaymentModal: React.FC<CheckoutPaymentModalProps> = ({
  reservation,
  onClose,
  onPaymentSuccess
}) => {
  const [metodo, setMetodo] = useState<'PSE' | 'TARJETA_CREDITO' | 'TRANSFERENCIA_BANCARIA'>('PSE');
  const [banco, setBanco] = useState('Bancolombia');
  const [numeroTarjeta, setNumeroTarjeta] = useState('4532 •••• •••• 8821');
  const [titular, setTitular] = useState('Constructora Colpatria Ingeniería');
  const [processing, setProcessing] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [completedInvoice, setCompletedInvoice] = useState<Invoice | null>(null);

  if (!reservation) return null;

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setProcessing(true);
    setErrorMsg('');

    const idempotencyKey = `IDEMP-${reservation.id}-${Date.now()}`;

    try {
      const res = await api.processPayment({
        reservaId: reservation.id,
        metodo,
        claveIdempotencia: idempotencyKey
      });

      setCompletedInvoice(res.factura);
      confetti({
        particleCount: 80,
        spread: 60,
        origin: { y: 0.6 }
      });
      onPaymentSuccess(res.factura);
    } catch (err: any) {
      setErrorMsg(err.message || 'Error al procesar el pago');
    } finally {
      setProcessing(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!completedInvoice) return;
    try {
      await api.downloadInvoicePdf(completedInvoice.id, completedInvoice.numeroFactura);
    } catch (e) {
      alert('Error descargando el PDF de la factura');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-200">
          <div>
            <span className="text-[11px] font-semibold tracking-wider uppercase text-amber-600 block">
              Pasarela de Pagos Segura
            </span>
            <h2 className="text-base font-bold text-neutral-900 leading-tight">
              {completedInvoice ? 'Pago Exitoso y Factura Emitida' : 'Confirmar y Pagar Alquiler'}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {completedInvoice ? (
          /* Payment Success View */
          <div className="p-6 text-center">
            <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-3">
              <CheckCircle2 className="w-7 h-7" />
            </div>
            <h3 className="text-lg font-bold text-neutral-900 mb-1">
              ¡Transacción Aprobada!
            </h3>
            <p className="text-xs text-neutral-600 max-w-sm mx-auto mb-6">
              El pago fue procesado con éxito en la pasarela simulada. Se ha generado la factura legal electrónica número <span className="font-mono font-bold text-neutral-900">{completedInvoice.numeroFactura}</span>.
            </p>

            {/* Invoice card */}
            <div className="bg-neutral-50 rounded-xl p-4 border border-neutral-200 text-left text-xs mb-6 space-y-1.5 font-mono">
              <div className="flex justify-between">
                <span className="text-neutral-500">Factura No:</span>
                <span className="font-bold text-neutral-900">{completedInvoice.numeroFactura}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Cliente:</span>
                <span className="text-neutral-900 truncate max-w-[200px]">{completedInvoice.clienteNombre}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Monto Total (IVA 19% inc.):</span>
                <span className="font-bold text-emerald-700 tabular-nums">
                  ${completedInvoice.total.toLocaleString('es-CO')} COP
                </span>
              </div>
            </div>

            <div className="flex items-center justify-center gap-3">
              <button
                onClick={handleDownloadPdf}
                className="px-4 py-2.5 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors flex items-center gap-1.5 shadow-xs"
              >
                <FileDown className="w-4 h-4" />
                <span>Descargar Factura PDF</span>
              </button>
              <button
                onClick={onClose}
                className="px-4 py-2.5 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors"
              >
                Cerrar
              </button>
            </div>
          </div>
        ) : (
          /* Payment Form */
          <form onSubmit={handlePay} className="p-6">
            {errorMsg && (
              <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Reservation Summary */}
            <div className="bg-neutral-50 rounded-xl p-4 border border-neutral-200 mb-5 text-xs">
              <div className="flex justify-between mb-1.5">
                <span className="text-neutral-500">Equipo:</span>
                <span className="font-semibold text-neutral-900">{reservation.herramientaNombre}</span>
              </div>
              <div className="flex justify-between mb-1.5">
                <span className="text-neutral-500">Periodo reservado:</span>
                <span className="font-mono text-neutral-800">{reservation.fechaInicio} a {reservation.fechaFin} ({reservation.dias} días)</span>
              </div>
              <div className="flex justify-between mb-1.5">
                <span className="text-neutral-500">Subtotal:</span>
                <span className="font-mono tabular-nums text-neutral-900">${reservation.subtotal.toLocaleString('es-CO')} COP</span>
              </div>
              <div className="flex justify-between mb-2">
                <span className="text-neutral-500">IVA (19%):</span>
                <span className="font-mono tabular-nums text-neutral-900">${reservation.iva.toLocaleString('es-CO')} COP</span>
              </div>
              <div className="flex justify-between pt-2 border-t border-neutral-200 font-bold text-sm">
                <span className="text-neutral-900">Total a Pagar:</span>
                <span className="text-amber-600 font-mono tabular-nums">${reservation.total.toLocaleString('es-CO')} COP</span>
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="mb-4">
              <label className="block text-xs font-semibold text-neutral-700 mb-2">
                Seleccione Método de Pago
              </label>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => setMetodo('PSE')}
                  className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                    metodo === 'PSE'
                      ? 'border-amber-500 bg-amber-50/50 text-neutral-900 font-bold'
                      : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                  }`}
                >
                  <Building2 className="w-5 h-5 mx-auto mb-1 text-amber-600" />
                  <span className="text-xs block">PSE Débito</span>
                </button>

                <button
                  type="button"
                  onClick={() => setMetodo('TARJETA_CREDITO')}
                  className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                    metodo === 'TARJETA_CREDITO'
                      ? 'border-amber-500 bg-amber-50/50 text-neutral-900 font-bold'
                      : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                  }`}
                >
                  <CreditCard className="w-5 h-5 mx-auto mb-1 text-amber-600" />
                  <span className="text-xs block">Tarjeta Crédito</span>
                </button>

                <button
                  type="button"
                  onClick={() => setMetodo('TRANSFERENCIA_BANCARIA')}
                  className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                    metodo === 'TRANSFERENCIA_BANCARIA'
                      ? 'border-amber-500 bg-amber-50/50 text-neutral-900 font-bold'
                      : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                  }`}
                >
                  <Building2 className="w-5 h-5 mx-auto mb-1 text-amber-600" />
                  <span className="text-xs block">Transferencia</span>
                </button>
              </div>
            </div>

            {/* Payment Details form depending on method */}
            {metodo === 'PSE' && (
              <div className="mb-5 space-y-3">
                <div>
                  <label className="block text-[11px] font-semibold text-neutral-600 mb-1">Banco Autorizado</label>
                  <select
                    value={banco}
                    onChange={(e) => setBanco(e.target.value)}
                    className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
                  >
                    <option value="Bancolombia">Bancolombia</option>
                    <option value="Banco de Bogotá">Banco de Bogotá</option>
                    <option value="Davivienda">Davivienda</option>
                    <option value="BBVA Colombia">BBVA Colombia</option>
                    <option value="Banco de Occidente">Banco de Occidente</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-neutral-600 mb-1">Titular de la Cuenta Empresarial</label>
                  <input
                    type="text"
                    value={titular}
                    onChange={(e) => setTitular(e.target.value)}
                    className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
              </div>
            )}

            {metodo === 'TARJETA_CREDITO' && (
              <div className="mb-5 space-y-3">
                <div>
                  <label className="block text-[11px] font-semibold text-neutral-600 mb-1">Número de Tarjeta Simulado</label>
                  <input
                    type="text"
                    value={numeroTarjeta}
                    onChange={(e) => setNumeroTarjeta(e.target.value)}
                    className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg font-mono focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-neutral-600 mb-1">Vencimiento</label>
                    <input
                      type="text"
                      defaultValue="08/29"
                      className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-neutral-600 mb-1">CVV</label>
                    <input
                      type="password"
                      defaultValue="724"
                      className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg font-mono"
                    />
                  </div>
                </div>
              </div>
            )}

            {metodo === 'TRANSFERENCIA_BANCARIA' && (
              <div className="mb-5 p-3 rounded-lg bg-neutral-100 text-[11px] text-neutral-700 leading-relaxed font-mono">
                Cuenta Corriente Bancolombia No. 402-981240-12<br />
                A nombre de: Plataforma Equipos de Construcción SAS<br />
                NIT: 900.123.456-1
              </div>
            )}

            <div className="flex items-center gap-2 text-[11px] text-neutral-500 mb-6 bg-neutral-50 p-2 rounded-lg border border-neutral-200">
              <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Transacción cifrada con protección de idempotencia bancaria.</span>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-xs font-semibold text-neutral-700 bg-neutral-100 rounded-lg hover:bg-neutral-200 transition-colors"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={processing}
                className="px-6 py-2.5 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors shadow-xs"
              >
                {processing ? 'Procesando Transacción...' : `Pagar $${reservation.total.toLocaleString('es-CO')} COP`}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
