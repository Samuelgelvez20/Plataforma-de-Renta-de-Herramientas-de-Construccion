import React, { useState } from 'react';
import { Tool } from '../types';
import { api } from '../services/api';
import { X, CheckCircle2, AlertTriangle, ShieldCheck, MapPin, Calendar, Clock } from 'lucide-react';

interface ToolDetailModalProps {
  tool: Tool | null;
  onClose: () => void;
  onBook: (tool: Tool) => void;
}

export const ToolDetailModal: React.FC<ToolDetailModalProps> = ({ tool, onClose, onBook }) => {
  const [checkStart, setCheckStart] = useState('');
  const [checkEnd, setCheckEnd] = useState('');
  const [availabilityResult, setAvailabilityResult] = useState<{ disponible: boolean; estadoOperativo: string; conflictos: any[] } | null>(null);
  const [checking, setChecking] = useState(false);

  if (!tool) return null;

  const handleCheck = async () => {
    if (!checkStart || !checkEnd) {
      alert('Seleccione fecha de inicio y fin para verificar disponibilidad');
      return;
    }
    setChecking(true);
    try {
      const res = await api.checkAvailability(tool.id, checkStart, checkEnd);
      setAvailabilityResult(res);
    } catch (e: any) {
      alert(e.message || 'Error al consultar disponibilidad');
    } finally {
      setChecking(false);
    }
  };

  const isAvailable = tool.estadoOperativo === 'DISPONIBLE';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-3xl w-full overflow-hidden shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-200">
          <div>
            <span className="text-[11px] font-semibold tracking-wider uppercase text-amber-600 block">
              Ficha Técnica de Maquinaria
            </span>
            <h2 className="text-lg font-bold text-neutral-900 leading-tight">
              {tool.nombre}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 max-h-[75vh] overflow-y-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            {/* Image */}
            <div className="rounded-xl overflow-hidden bg-neutral-100 border border-neutral-200 h-64">
              <img
                src={tool.imagenUrl}
                alt={tool.nombre}
                className="w-full h-full object-cover"
              />
            </div>

            {/* Quick Summary */}
            <div className="flex flex-col justify-between">
              <div>
                {/* Unboxed Metadata */}
                <div className="flex items-center gap-2 text-xs font-medium text-neutral-500 mb-2">
                  <span>Marca: {tool.marca}</span>
                  <span aria-hidden="true">·</span>
                  <span>Modelo: {tool.modelo}</span>
                </div>

                <div className="flex items-center gap-1.5 text-xs text-neutral-600 mb-4">
                  <MapPin className="w-3.5 h-3.5 text-neutral-400" />
                  <span>{tool.ubicacion}</span>
                </div>

                <p className="text-sm text-neutral-700 mb-4">
                  {tool.descripcion}
                </p>
              </div>

              {/* Rates Box */}
              <div className="bg-neutral-50 rounded-xl p-4 border border-neutral-200">
                <div className="flex items-baseline justify-between mb-2">
                  <span className="text-xs text-neutral-500 font-medium">Tarifa por día:</span>
                  <span className="text-xl font-extrabold text-neutral-900 font-mono tabular-nums">
                    ${tool.precioPorDia.toLocaleString('es-CO')} <span className="text-xs text-neutral-500 font-normal">COP</span>
                  </span>
                </div>
                <div className="flex items-baseline justify-between text-xs text-neutral-600 mb-2">
                  <span>Depósito de garantía:</span>
                  <span className="font-mono tabular-nums font-semibold">
                    ${tool.depositoGarantia.toLocaleString('es-CO')} COP
                  </span>
                </div>
                <div className="flex items-baseline justify-between text-[11px] text-neutral-500 pt-2 border-t border-neutral-200">
                  <span>Impuesto IVA aplicable:</span>
                  <span className="font-semibold text-neutral-800">19% (Ley Tributaria Colombia)</span>
                </div>
              </div>
            </div>
          </div>

          {/* Technical Specifications */}
          <div className="mb-6">
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-900 mb-2">
              Especificaciones y Requisitos de Obra
            </h4>
            <div className="bg-neutral-50 p-3.5 rounded-xl border border-neutral-200 text-xs text-neutral-700 leading-relaxed font-mono">
              {tool.especificacionesTecnicas || 'Consulte los requerimientos de carga y transporte con el proveedor.'}
            </div>
          </div>

          {/* Live Date Overlap & Availability Checker */}
          <div className="bg-amber-50/70 p-4 rounded-xl border border-amber-200 mb-6">
            <h4 className="text-xs font-bold text-amber-950 flex items-center gap-1.5 mb-2">
              <Calendar className="w-4 h-4 text-amber-700" />
              <span>Comprobar Disponibilidad en Calendario</span>
            </h4>
            <p className="text-xs text-amber-900 mb-3">
              Verifique si el equipo se encuentra libre de solapamientos antes de enviar la solicitud.
            </p>

            <div className="flex flex-wrap items-center gap-3">
              <input
                type="date"
                value={checkStart}
                onChange={(e) => setCheckStart(e.target.value)}
                className="text-xs bg-white border border-amber-300 rounded-lg px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
              <span className="text-xs text-amber-800">hasta</span>
              <input
                type="date"
                value={checkEnd}
                onChange={(e) => setCheckEnd(e.target.value)}
                className="text-xs bg-white border border-amber-300 rounded-lg px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
              <button
                onClick={handleCheck}
                disabled={checking}
                className="px-3 py-1.5 text-xs font-semibold bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition-colors cursor-pointer"
              >
                {checking ? 'Verificando...' : 'Comprobar'}
              </button>
            </div>

            {availabilityResult && (
              <div className="mt-3 pt-3 border-t border-amber-200/60 text-xs">
                {availabilityResult.disponible ? (
                  <div className="flex items-center gap-1.5 text-emerald-800 font-semibold">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    <span>¡Equipo disponible en el rango consultado! No presenta conflictos de solapamiento.</span>
                  </div>
                ) : (
                  <div className="flex items-start gap-1.5 text-rose-800 font-semibold">
                    <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <div>
                      <span>No disponible: Hay reservas activas en ese periodo.</span>
                      {availabilityResult.conflictos?.map((c, i) => (
                        <div key={i} className="text-[11px] font-normal text-rose-700 font-mono mt-0.5">
                          Conflicto: del {c.fechaInicio} al {c.fechaFin} (Estado: {c.estado})
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Footer actions */}
        <div className="px-6 py-4 bg-neutral-50 border-t border-neutral-200 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-neutral-700 bg-white border border-neutral-300 rounded-lg hover:bg-neutral-100 transition-colors"
          >
            Cerrar
          </button>
          <button
            onClick={() => { onClose(); onBook(tool); }}
            disabled={!isAvailable}
            className={`px-5 py-2 text-xs font-bold rounded-lg transition-colors ${
              isAvailable
                ? 'bg-amber-500 hover:bg-amber-600 text-slate-950 shadow-xs'
                : 'bg-neutral-300 text-neutral-500 cursor-not-allowed'
            }`}
          >
            {isAvailable ? 'Solicitar Reserva de Este Equipo' : 'Equipo No Disponible'}
          </button>
        </div>
      </div>
    </div>
  );
};
