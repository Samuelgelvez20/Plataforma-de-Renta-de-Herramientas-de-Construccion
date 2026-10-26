import React, { useState } from 'react';
import { Reservation, Invoice } from '../types';
import { api } from '../services/api';
import confetti from 'canvas-confetti';
import { X, CreditCard, Building2, CheckCircle2, FileDown, ShieldCheck, AlertCircle } from 'lucide-react';
import { useToast } from '../context/ToastContext';

const LISTA_BANCOS_COLOMBIA = [
  'Bancolombia (Opción a elegir para pagar con fondos de Nequi)',
  'Banco de Bogotá',
  'Banco Davivienda',
  'BBVA Colombia',
  'Banco de Occidente',
  'Banco Popular',
  'Banco Itaú',
  'Scotiabank Colpatria',
  'Banco Caja Social',
  'Banco Agrario de Colombia',
  'Banco AV Villas',
  'Banco Pichincha',
  'Banco Santander Colombia',
  'Citibank (Banca corporativa)',
  'Banco GNB Sudameris',
  'Lulo Bank (Primer banco 100% digital del país)',
  'Dale! (Billetera digital del Grupo Aval)',
  'Rappipay / Daviplata (Aparecen unificados en el selector técnico bajo Davivienda)',
  'Banco Finandina (Enfoque digital y automotriz)',
  'Banco Falabella',
  'Bancamía',
  'Banco Mundo Mujer',
  'Banco Serfinanza',
  'Bancoomeva',
  'Banco Cooperativo Coopcentral',
  'Banco ProCredit',
  'CFA Cooperativa Financiera',
  'Cooperativa Financiera JFK (Otras cooperativas regionales se canalizan mediante la red Coopcentral)',
  'JP Morgan (Exclusivo para clientes corporativos)',
  'Nequi',
  'Nu (Nubank)'
];

interface CheckoutPaymentModalProps {
  reservation?: Reservation | null;
  invoice?: Invoice | null;
  onClose: () => void;
  onPaymentSuccess: (invoice: Invoice) => void;
}

export const CheckoutPaymentModal: React.FC<CheckoutPaymentModalProps> = ({
  reservation,
  invoice,
  onClose,
  onPaymentSuccess
}) => {
  const { toast } = useToast();
  const [metodo, setMetodo] = useState<'PSE' | 'TARJETA_CREDITO' | 'TRANSFERENCIA_BANCARIA'>('PSE');
  const [banco, setBanco] = useState(LISTA_BANCOS_COLOMBIA[0]);
  const [numeroTarjeta, setNumeroTarjeta] = useState('4532 •••• •••• 8821');
  const [titular, setTitular] = useState('Constructora Colpatria Ingeniería');
  const [processing, setProcessing] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [completedInvoice, setCompletedInvoice] = useState<Invoice | null>(null);

  if (!reservation && !invoice) return null;

  const isRepairPayment = Boolean(invoice);
  const targetReservaId = invoice ? invoice.reservaId : reservation?.id;
  const targetFacturaId = invoice?.id;
  const targetTotal = invoice ? invoice.total : (reservation?.total ?? 0);

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setProcessing(true);
    setErrorMsg('');

    const refId = targetFacturaId || targetReservaId;
    const idempotencyKey = `IDEMP-${refId}-${Date.now()}`;

    try {
      const res = await api.processPayment({
        reservaId: targetReservaId,
        facturaId: targetFacturaId,
        metodo,
        claveIdempotencia: idempotencyKey
      });

      const resultingInvoice = res.factura || invoice;
      setCompletedInvoice(resultingInvoice);
      confetti({
        particleCount: 80,
        spread: 60,
        origin: { y: 0.6 }
      });
      onPaymentSuccess(resultingInvoice);
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
      toast.success('Descarga de factura PDF completada exitosamente', 'Factura Descargada');
    } catch (e) {
      toast.error('Error descargando el PDF de la factura', 'Fallo de Descarga');
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
              {completedInvoice ? 'Pago Exitoso y Factura Emitida' : (isRepairPayment ? 'Pago de Costos de Reparación' : 'Confirmar y Pagar Alquiler')}
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
                <span className="text-neutral-500">Concepto:</span>
                <span className="text-neutral-900 truncate max-w-[200px]">{completedInvoice.concepto || completedInvoice.herramientaNombre || 'Servicio'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Monto Total:</span>
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

            {/* Reservation / Invoice Summary */}
            {isRepairPayment && invoice ? (
              <div className="bg-amber-50/60 rounded-xl p-4 border border-amber-200 mb-5 text-xs">
                <div className="flex justify-between mb-1.5">
                  <span className="text-neutral-500">Concepto:</span>
                  <span className="font-semibold text-neutral-900">{invoice.concepto || `Costos de reparación de ${invoice.herramientaNombre || 'equipo'}`}</span>
                </div>
                <div className="flex justify-between mb-1.5">
                  <span className="text-neutral-500">Herramienta:</span>
                  <span className="font-medium text-neutral-800">{invoice.herramientaNombre || 'Equipo'}</span>
                </div>
                {invoice.descripcionDano && (
                  <div className="flex justify-between mb-1.5 bg-amber-100/60 p-2 rounded text-neutral-800">
                    <span className="text-neutral-600 font-medium">Daño reportado:</span>
                    <span className="font-semibold text-right max-w-[240px]">{invoice.descripcionDano}</span>
                  </div>
                )}
                <div className="flex justify-between mb-1.5">
                  <span className="text-neutral-500">Referencia Reserva:</span>
                  <span className="font-mono text-neutral-800">#{invoice.reservaId}</span>
                </div>
                <div className="flex justify-between pt-2 border-t border-amber-200 font-bold text-sm">
                  <span className="text-neutral-900">Total a Pagar (COP):</span>
                  <span className="text-amber-700 font-mono tabular-nums">${targetTotal.toLocaleString('es-CO')} COP</span>
                </div>
              </div>
            ) : reservation ? (
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
                  <span className="text-amber-600 font-mono tabular-nums">${targetTotal.toLocaleString('es-CO')} COP</span>
                </div>
              </div>
            ) : null}

            {/* Payment Method Selector */}
            <div className="mb-4">
              <label className="block text-xs font-semibold text-neutral-700 mb-2">
                Seleccione Método de Pago Seguro
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setMetodo('PSE')}
                  className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                    metodo === 'PSE'
                      ? 'border-amber-500 bg-amber-50/60 text-neutral-900 font-bold shadow-xs ring-1 ring-amber-500'
                      : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                  }`}
                >
                  <Building2 className="w-5 h-5 mx-auto mb-1 text-amber-600" />
                  <span className="text-xs block font-bold">PSE Débito</span>
                  <span className="text-[10px] text-neutral-500 block">Cuentas bancarias y billeteras</span>
                </button>

                <button
                  type="button"
                  onClick={() => setMetodo('TARJETA_CREDITO')}
                  className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                    metodo === 'TARJETA_CREDITO'
                      ? 'border-amber-500 bg-amber-50/60 text-neutral-900 font-bold shadow-xs ring-1 ring-amber-500'
                      : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                  }`}
                >
                  <CreditCard className="w-5 h-5 mx-auto mb-1 text-amber-600" />
                  <span className="text-xs block font-bold">Tarjeta de Crédito</span>
                  <span className="text-[10px] text-neutral-500 block">Visa, Mastercard, Amex</span>
                </button>
              </div>
            </div>

            {/* Payment Details form depending on method */}
            {metodo === 'PSE' && (
              <div className="mb-5 space-y-3">
                <div>
                  <label className="block text-[11px] font-semibold text-neutral-600 mb-1">
                    Banco o Entidad Financiera ({LISTA_BANCOS_COLOMBIA.length} entidades disponibles)
                  </label>
                  <select
                    value={banco}
                    onChange={(e) => setBanco(e.target.value)}
                    className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500 font-medium"
                  >
                    {LISTA_BANCOS_COLOMBIA.map((nombreBanco) => (
                      <option key={nombreBanco} value={nombreBanco}>
                        {nombreBanco}
                      </option>
                    ))}
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
                {processing ? 'Procesando Transacción...' : `Pagar $${targetTotal.toLocaleString('es-CO')} COP`}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
