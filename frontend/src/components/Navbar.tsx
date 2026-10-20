import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { HardHat, User as UserIcon, LogOut, FileCode, Menu, X } from 'lucide-react';

interface NavbarProps {
  currentView: 'catalog' | 'client' | 'owner' | 'admin';
  onNavigate: (view: 'catalog' | 'client' | 'owner' | 'admin') => void;
  onOpenSwagger: () => void;
  onOpenAuth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentView,
  onNavigate,
  onOpenSwagger,
  onOpenAuth
}) => {
  const { user, logout, switchDemoRole, isDemoEnabled } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);


  return (
    <header className="sticky top-0 z-40 bg-white border-b border-neutral-200">
      {/* Top Bar Contract: Zone 1 (Brand) — Zone 2 (4-5 Nav Links) — Zone 3 (Primary Action / User) */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-8">
        {/* Zone 1: Brand Wordmark (OBRIXEN) */}
        <div
          onClick={() => onNavigate('catalog')}
          className="flex items-center gap-2.5 cursor-pointer whitespace-nowrap shrink-0 group"
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onNavigate('catalog'); }}
          aria-label="Ir al catálogo principal de OBRIXEN"
        >
          <div className="w-9 h-9 rounded-lg bg-[#F5A623] flex items-center justify-center text-[#17191C] font-bold shadow-xs transition-transform group-hover:scale-105">
            <HardHat className="w-5 h-5 text-[#17191C]" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-base font-extrabold tracking-tight text-[#17191C] block leading-tight font-sans">
                OBRIXEN
              </span>
              <span className="text-[9px] font-bold uppercase tracking-wider bg-[#17191C] text-[#F5A623] px-1.5 py-0.5 rounded">
                EQUIPOS
              </span>
            </div>
            <span className="text-[11px] font-medium text-neutral-500 block">
              Renta de Maquinaria de Construcción
            </span>
          </div>
        </div>

        {/* Zone 2: Concise Single-Line Navigation Links */}
        <nav className="hidden md:flex items-center gap-7 text-sm font-medium text-neutral-600">
          <button
            onClick={() => onNavigate('catalog')}
            className={`whitespace-nowrap shrink-0 transition-colors py-1 cursor-pointer ${
              currentView === 'catalog'
                ? 'text-neutral-900 border-b-2 border-amber-500 font-semibold'
                : 'hover:text-neutral-900'
            }`}
          >
            Catálogo de Equipos
          </button>

          {user?.rol === 'CLIENTE' && (
            <button
              onClick={() => onNavigate('client')}
              className={`whitespace-nowrap shrink-0 transition-colors py-1 cursor-pointer ${
                currentView === 'client'
                  ? 'text-neutral-900 border-b-2 border-amber-500 font-semibold'
                  : 'hover:text-neutral-900'
              }`}
            >
              Mis Alquileres & Facturas
            </button>
          )}

          {(user?.rol === 'PROPIETARIO' || user?.rol === 'ADMIN') && (
            <button
              onClick={() => onNavigate('owner')}
              className={`whitespace-nowrap shrink-0 transition-colors py-1 cursor-pointer ${
                currentView === 'owner'
                  ? 'text-neutral-900 border-b-2 border-amber-500 font-semibold'
                  : 'hover:text-neutral-900'
              }`}
            >
              Gestión Propietario
            </button>
          )}

          {user?.rol === 'ADMIN' && (
            <button
              onClick={() => onNavigate('admin')}
              className={`whitespace-nowrap shrink-0 transition-colors py-1 cursor-pointer ${
                currentView === 'admin'
                  ? 'text-neutral-900 border-b-2 border-amber-500 font-semibold'
                  : 'hover:text-neutral-900'
              }`}
            >
              Panel Administrador
            </button>
          )}

          <button
            onClick={onOpenSwagger}
            className="flex items-center gap-1.5 text-neutral-600 hover:text-neutral-900 whitespace-nowrap shrink-0 transition-colors py-1 cursor-pointer"
            title="Ver documentación Swagger OpenAPI y Arquitectura Hexagonal"
          >
            <FileCode className="w-4 h-4 text-amber-600" />
            <span>OpenAPI & Arquitectura</span>
          </button>
        </nav>

        {/* Zone 3: Single Primary Action & Role Quick-Switcher */}
        <div className="flex items-center gap-3 shrink-0">
          {/* Mobile hamburger menu toggle */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-menu"
            aria-label={mobileMenuOpen ? 'Cerrar menú' : 'Abrir menú de navegación'}
            className="md:hidden p-2 text-neutral-600 hover:text-neutral-900 rounded-lg hover:bg-neutral-100 cursor-pointer"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
          {/* Quick Demo Switcher Selector (Solo en modo demo explícito para desarrollo, sin roles privilegiados) */}
          {isDemoEnabled && (
            <div className="hidden lg:flex items-center bg-neutral-100 rounded-lg p-1 border border-neutral-200">
              <span className="text-[11px] font-medium text-neutral-500 px-2">Demo dev:</span>
              <button
                onClick={() => { switchDemoRole('CLIENTE'); onNavigate('catalog'); }}
                className={`text-xs px-2.5 py-1 rounded-md font-medium transition-colors cursor-pointer ${
                  user?.rol === 'CLIENTE'
                    ? 'bg-white text-neutral-900 shadow-xs'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                Cliente
              </button>
              <button
                onClick={() => { switchDemoRole('PROPIETARIO'); onNavigate('owner'); }}
                className={`text-xs px-2.5 py-1 rounded-md font-medium transition-colors cursor-pointer ${
                  user?.rol === 'PROPIETARIO'
                    ? 'bg-white text-neutral-900 shadow-xs'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                Propietario
              </button>
            </div>
          )}

          {user ? (
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <span className="text-xs font-semibold text-neutral-900 block truncate max-w-[150px]">
                  {user.nombre}
                </span>
                <span className="text-[10px] text-neutral-500 font-mono">
                  {user.rol}
                </span>
              </div>
              <button
                onClick={() => { logout(); onNavigate('catalog'); }}
                className="p-2 text-neutral-500 hover:text-rose-600 rounded-lg hover:bg-neutral-100 transition-colors cursor-pointer"
                title="Cerrar sesión"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={onOpenAuth}
              className="px-4 py-2 text-xs font-semibold text-white bg-neutral-900 rounded-lg hover:bg-neutral-800 transition-colors whitespace-nowrap cursor-pointer flex items-center gap-1.5"
            >
              <UserIcon className="w-3.5 h-3.5" />
              <span>Iniciar Sesión</span>
            </button>
          )}
        </div>
      </div>
      {/* Mobile Navigation Drawer */}
      {mobileMenuOpen && (
        <nav
          id="mobile-menu"
          aria-label="Menú de navegación móvil"
          className="md:hidden border-t border-neutral-200 bg-white px-4 pt-3 pb-5 space-y-1.5 animate-in fade-in slide-in-from-top-2 duration-150"
        >
          <button
            onClick={() => { onNavigate('catalog'); setMobileMenuOpen(false); }}
            className={`w-full text-left px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
              currentView === 'catalog' ? 'bg-amber-50 text-amber-900 font-bold border-l-4 border-amber-500' : 'text-neutral-700 hover:bg-neutral-100'
            }`}
          >
            Catálogo de Equipos
          </button>

          {user?.rol === 'CLIENTE' && (
            <button
              onClick={() => { onNavigate('client'); setMobileMenuOpen(false); }}
              className={`w-full text-left px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                currentView === 'client' ? 'bg-amber-50 text-amber-900 font-bold border-l-4 border-amber-500' : 'text-neutral-700 hover:bg-neutral-100'
              }`}
            >
              Mis Alquileres & Facturas
            </button>
          )}

          {(user?.rol === 'PROPIETARIO' || user?.rol === 'ADMIN') && (
            <button
              onClick={() => { onNavigate('owner'); setMobileMenuOpen(false); }}
              className={`w-full text-left px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                currentView === 'owner' ? 'bg-amber-50 text-amber-900 font-bold border-l-4 border-amber-500' : 'text-neutral-700 hover:bg-neutral-100'
              }`}
            >
              Gestión Propietario
            </button>
          )}

          {user?.rol === 'ADMIN' && (
            <button
              onClick={() => { onNavigate('admin'); setMobileMenuOpen(false); }}
              className={`w-full text-left px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                currentView === 'admin' ? 'bg-amber-50 text-amber-900 font-bold border-l-4 border-amber-500' : 'text-neutral-700 hover:bg-neutral-100'
              }`}
            >
              Panel Administrador
            </button>
          )}

          <button
            onClick={() => { onOpenSwagger(); setMobileMenuOpen(false); }}
            className="w-full text-left px-3 py-2 rounded-lg text-xs font-semibold text-neutral-700 hover:bg-neutral-100 flex items-center gap-2 cursor-pointer"
          >
            <FileCode className="w-4 h-4 text-amber-600" />
            <span>OpenAPI & Arquitectura</span>
          </button>

          {user ? (
            <div className="pt-3 border-t border-neutral-200 flex items-center justify-between">
              <div className="text-xs">
                <span className="font-bold text-neutral-900 block truncate max-w-[180px]">{user.nombre}</span>
                <span className="text-neutral-500 font-mono text-[10px]">{user.rol}</span>
              </div>
              <button
                onClick={() => { logout(); setMobileMenuOpen(false); }}
                className="px-3 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-lg flex items-center gap-1 cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Cerrar Sesión</span>
              </button>
            </div>
          ) : (
            <div className="pt-3 border-t border-neutral-200">
              <button
                onClick={() => { onOpenAuth(); setMobileMenuOpen(false); }}
                className="w-full py-2 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                <UserIcon className="w-3.5 h-3.5" />
                <span>Iniciar Sesión / Registro</span>
              </button>
            </div>
          )}
        </nav>
      )}
    </header>
  );
};
