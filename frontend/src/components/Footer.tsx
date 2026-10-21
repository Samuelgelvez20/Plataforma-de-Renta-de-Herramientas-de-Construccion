import React from 'react';
import { HardHat, ShieldCheck, FileCheck, Truck, Clock, Layers, ArrowUpRight } from 'lucide-react';

interface FooterProps {
  onNavigate?: (view: 'landing' | 'catalog' | 'client' | 'owner' | 'admin') => void;
  onOpenSwagger?: () => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate, onOpenSwagger }) => {
  return (
    <footer className="bg-[#17191C] text-[#F4F5F6] border-t border-[#292D32] pt-14 pb-8 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Main Footer Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 pb-12 border-b border-[#292D32]">
          {/* Brand & Corporate Statement */}
          <div className="lg:col-span-5 flex flex-col items-start space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-lg bg-[#F5A623] flex items-center justify-center text-[#17191C] font-bold shadow-xs">
                <HardHat className="w-5 h-5 text-[#17191C]" />
              </div>
              <div className="flex items-center gap-1.5">
                <span className="text-lg font-extrabold tracking-tight text-[#F4F5F6] font-sans">
                  OBRIXEN
                </span>
                <span className="text-[9px] font-bold uppercase tracking-wider bg-[#292D32] text-[#F5A623] px-1.5 py-0.5 rounded border border-[#F5A623]/30">
                  EQUIPOS
                </span>
              </div>
            </div>

            <p className="text-xs text-neutral-400 max-w-sm leading-relaxed">
              Plataforma tecnológica para la gestión, cotización y alquiler de maquinaria pesada, herramientas de demolición y andamiajes de construcción con disponibilidad verificada en tiempo real en Colombia.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-3 text-[11px] text-neutral-400">
              <div className="flex items-center gap-1.5 bg-[#1F2226] px-2.5 py-1 rounded border border-[#292D32]">
                <ShieldCheck className="w-3.5 h-3.5 text-[#F5A623]" />
                <span>Depósitos en Custodia</span>
              </div>
              <div className="flex items-center gap-1.5 bg-[#1F2226] px-2.5 py-1 rounded border border-[#292D32]">
                <FileCheck className="w-3.5 h-3.5 text-[#F5A623]" />
                <span>Facturación IVA 19%</span>
              </div>
              <div className="flex items-center gap-1.5 bg-[#1F2226] px-2.5 py-1 rounded border border-[#292D32]">
                <Truck className="w-3.5 h-3.5 text-[#F5A623]" />
                <span>Entrega con Acta Técnica</span>
              </div>
            </div>
          </div>

          {/* Quick Links Column 1: Catálogo */}
          <div className="lg:col-span-2 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#F5A623]">
              Catálogo
            </h4>
            <ul className="space-y-2 text-xs text-neutral-400">
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Maquinaria Pesada
                </button>
              </li>
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Equipos de Concreto
                </button>
              </li>
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Andamios Certificados
                </button>
              </li>
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Herramienta Eléctrica
                </button>
              </li>
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Generación & Bombeo
                </button>
              </li>
            </ul>
          </div>

          {/* Quick Links Column 2: Marco Operativo */}
          <div className="lg:col-span-3 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#F5A623]">
              Marco Operativo & Fiscal
            </h4>
            <ul className="space-y-2 text-xs text-neutral-400">
              <li className="flex items-center gap-1.5">
                <Clock className="w-3 h-3 text-neutral-500 shrink-0" />
                <span>Renta flexible desde 1 día</span>
              </li>
              <li className="flex items-center gap-1.5">
                <FileCheck className="w-3 h-3 text-neutral-500 shrink-0" />
                <span>Facturas electrónicas oficiales en PDF</span>
              </li>
              <li className="flex items-center gap-1.5">
                <ShieldCheck className="w-3 h-3 text-neutral-500 shrink-0" />
                <span>Liquidación contra acta de recepción</span>
              </li>
              <li className="flex items-center gap-1.5">
                <Layers className="w-3 h-3 text-neutral-500 shrink-0" />
                <span>Pagos protegidos con idempotencia</span>
              </li>
            </ul>
          </div>

          {/* Quick Links Column 3: Arquitectura y API */}
          <div className="lg:col-span-2 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#F5A623]">
              Plataforma
            </h4>
            <ul className="space-y-2 text-xs text-neutral-400">
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('landing')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Inicio
                </button>
              </li>
              <li>
                <button
                  type="button"
                  onClick={() => onNavigate?.('catalog')}
                  className="hover:text-white transition-colors cursor-pointer text-left"
                >
                  Explorar Catálogo
                </button>
              </li>
              {onOpenSwagger && (
                <li>
                  <button
                    type="button"
                    onClick={onOpenSwagger}
                    className="hover:text-amber-400 transition-colors cursor-pointer text-left inline-flex items-center gap-1"
                  >
                    <span>OpenAPI / Swagger</span>
                    <ArrowUpRight className="w-3 h-3" />
                  </button>
                </li>
              )}
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-6 flex flex-col md:flex-row items-center justify-between gap-4 text-xs text-neutral-500">
          <p>
            © 2026 OBRIXEN. Plataforma de Renta de Equipos de Construcción. Todos los derechos reservados.
          </p>

          <div className="flex flex-wrap items-center gap-2.5 text-[11px] font-mono text-neutral-400">
            <span className="px-2 py-0.5 rounded bg-[#1F2226] border border-[#292D32]">Spring Boot 3</span>
            <span className="px-2 py-0.5 rounded bg-[#1F2226] border border-[#292D32]">Java 21</span>
            <span className="px-2 py-0.5 rounded bg-[#1F2226] border border-[#292D32]">PostgreSQL 16</span>
            <span className="px-2 py-0.5 rounded bg-[#1F2226] border border-[#292D32]">Vite + React 19</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
