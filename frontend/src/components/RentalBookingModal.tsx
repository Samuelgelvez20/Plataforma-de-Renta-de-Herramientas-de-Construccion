import React, { useState } from 'react';
import { Tool } from '../types';
import { api } from '../services/api';
import { X, Calendar, Calculator, CheckCircle2, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface RentalBookingModalProps {
  tool: Tool | null;
  onClose: () => void;
  onSuccess: () => void;
  onOpenAuth: () => void;
}

export const RentalBookingModal: React.FC<RentalBookingModalProps> = ({
  tool,
  onClose,
  onSuccess,
  onOpenAuth
}) => {
  const { user } = useAuth();
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [notasCliente, setNotasCliente] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  if (!tool) return null;

  const calculateDays = () => {
    if (!fechaInicio || !fechaFin) return 0;
    const s = new Date(fechaInicio);
    const e = new Date(fechaFin);
    if (e < s) return 0;
    const diff = Math.ceil(Math.abs(e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
    return diff === 0 ? 1 : diff;
  };

  const days = calculateDays();
  const subtotal = tool.precioPorDia * days;
  const iva = Math.round(subtotal * 0.19);
  const total = subtotal + iva;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      onOpenAuth();
      return;
    }
    if (user.rol !== 'CLIENTE') {
      setErrorMsg('Debe iniciar sesión con una cuenta de CLIENTE para solicitar alquileres.');
      return;
    }
    if (days <= 0) {
      setErrorMsg('La fecha de finalización debe ser igual o posterior a la fecha de inicio.');
      return;
    }

    setSubmitting(true);
    setErrorMsg('');

    try {
      await api.createReservation({
        herramientaId: tool.id,
        fechaInicio,
        fechaFin,
        notasCliente
      });
      alert('¡Reserva creada con éxito en estado PENDIENTE! El propietario revisará la solicitud.');
      onSuccess();
    } catch (err: any) {
      setErrorMsg(err.message || 'Error al procesar la reserva');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-200">
          <div>
            <span className="text-[11px] font-semibold tracking-wider uppercase text-amber-600 block">
              Formulario de Alquiler
            </span>
            <h2 className="text-base font-bold text-neutral-900 leading-tight">
              Solicitar Reserva de Equipo
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {/* Selected Tool Summary */}
          <div className="flex items-center gap-3 p-3 bg-neutral-50 rounded-xl border border-neutral-200 mb-5">
            <img
              src={tool.imagenUrl}
              alt={tool.nombre}
              className="w-16 h-16 object-cover rounded-lg shrink-0 border border-neutral-200"
            />
            <div className="overflow-hidden">
              <h4 className="text-xs font-bold text-neutral-900 truncate">{tool.nombre}</h4>
              <p className="text-[11px] text-neutral-500 font-mono">
                ${tool.precioPorDia.toLocaleString('es-CO')} COP / día
              </p>
              <p className="text-[11px] text-neutral-500 truncate">{tool.ubicacion}</p>
            </div>
          </div>

          {errorMsg && (
            <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Date Range Inputs */}
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Fecha de Inicio *
              </label>
              <div className="relative">
                <input
                  type="date"
                  required
                  min={new Date().toISOString().split('T')[0]}
                  value={fechaInicio}
                  onChange={(e) => setFechaInicio(e.target.value)}
                  className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Fecha de Fin *
              </label>
              <div className="relative">
                <input
                  type="date"
                  required
                  min={fechaInicio || new Date().toISOString().split('T')[0]}
                  value={fechaFin}
                  onChange={(e) => setFechaFin(e.target.value)}
                  className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
            </div>
          </div>

          {/* Notes field */}
          <div className="mb-5">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Notas para el Propietario (Lugar de obra, operador, etc.)
            </label>
            <textarea
              rows={2}
              value={notasCliente}
              onChange={(e) => setNotasCliente(e.target.value)}
              placeholder="Ej: Obra Cimentación Edificio Salitre, Calle 72 con Carrera 10..."
              className="w-full text-xs p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
          </div>

          {/* Calculation Box */}
          <div className="bg-neutral-900 text-white rounded-xl p-4 mb-6">
            <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
              <span>Días calculados:</span>
              <span className="font-mono text-white font-semibold tabular-nums">{days} {days === 1 ? 'día' : 'días'}</span>
            </div>
            <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
              <span>Subtotal ({days} x ${tool.precioPorDia.toLocaleString('es-CO')}):</span>
              <span className="font-mono text-white tabular-nums">${subtotal.toLocaleString('es-CO')} COP</span>
            </div>
            <div className="flex items-center justify-between text-xs text-neutral-400 mb-3 pb-3 border-b border-neutral-800">
              <span>IVA (19% Legal):</span>
              <span className="font-mono text-white tabular-nums">${iva.toLocaleString('es-CO')} COP</span>
            </div>
            <div className="flex items-baseline justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-amber-400">Total a Pagar:</span>
              <span className="text-lg font-extrabold text-amber-400 font-mono tabular-nums">
                ${total.toLocaleString('es-CO')} COP
              </span>
            </div>
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
              disabled={submitting || days <= 0}
              className="px-5 py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors shadow-xs disabled:opacity-50"
            >
              {submitting ? 'Creando Reserva...' : 'Confirmar Solicitud de Reserva'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
