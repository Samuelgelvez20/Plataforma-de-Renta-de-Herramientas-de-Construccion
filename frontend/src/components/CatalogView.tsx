import React, { useState, useEffect, useMemo } from 'react';
import { Tool, Category } from '../types';
import { resolveToolImage, defaultExcavator } from '../utils/imageResolver';
import { api } from '../services/api';
import {
  Search,
  SlidersHorizontal,
  Calendar,
  CheckCircle2,
  AlertCircle,
  RotateCcw,
  Tag,
  ArrowUpDown,
  X,
  Boxes,
  ShieldCheck,
  HardHat,
  Filter
} from 'lucide-react';

const IVA_RATE = 0.19;

type SortOption = 'destacados' | 'precio_asc' | 'precio_desc' | 'nombre_asc' | 'stock_desc';

interface CatalogViewProps {
  onSelectTool: (tool: Tool) => void;
  onBookTool: (tool: Tool) => void;
  initialCategory?: string | number | null;
  onClearInitialCategory?: () => void;
}

export const CatalogView: React.FC<CatalogViewProps> = ({
  onSelectTool,
  onBookTool,
  initialCategory,
  onClearInitialCategory
}) => {
  const [tools, setTools] = useState<Tool[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | number | null>(initialCategory || null);
  const [searchQuery, setSearchQuery] = useState('');
  const [maxPrice, setMaxPrice] = useState<number>(2000000);
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [sortBy, setSortBy] = useState<SortOption>('destacados');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Estimador de periodo para cálculo de alquiler
  const [quickStartDate, setQuickStartDate] = useState('');
  const [quickEndDate, setQuickEndDate] = useState('');

  // Mobile Drawer State
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  // Sincronizar initialCategory cuando cambie (ej: navegación directa desde la landing)
  useEffect(() => {
    if (initialCategory !== undefined) {
      setSelectedCategory(initialCategory);
    }
  }, [initialCategory]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [catsData, toolsData] = await Promise.all([
        api.getCategories(),
        api.getTools()
      ]);
      setCategories(catsData);
      setTools(toolsData);
    } catch (err: unknown) {
      console.error('Error cargando catálogo:', err);
      const message = err instanceof Error ? err.message : 'No se pudo conectar con el servidor de catálogo.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Cerrar drawer móvil con tecla Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileFiltersOpen) {
        setMobileFiltersOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileFiltersOpen]);

  const calculateDays = (): number => {
    if (!quickStartDate || !quickEndDate) return 1;
    const s = new Date(quickStartDate);
    const e = new Date(quickEndDate);
    if (e < s) return 1;
    const diff = Math.ceil(Math.abs(e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
    return diff === 0 ? 1 : diff;
  };

  const currentDays = calculateDays();

  // Filtrado y ordenamiento de herramientas
  const filteredTools = useMemo(() => {
    const list = tools.filter(tool => {
      if (selectedCategory && String(tool.categoriaId) !== String(selectedCategory)) return false;
      const stockDisp = tool.cantidadDisponible !== undefined ? tool.cantidadDisponible : (tool.cantidadTotal || 1);
      if (onlyAvailable && (tool.estadoOperativo !== 'DISPONIBLE' || stockDisp <= 0)) return false;
      if (tool.precioPorDia > maxPrice) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const match =
          tool.nombre.toLowerCase().includes(q) ||
          tool.descripcion.toLowerCase().includes(q) ||
          tool.marca.toLowerCase().includes(q) ||
          tool.modelo.toLowerCase().includes(q) ||
          tool.ubicacion.toLowerCase().includes(q);
        if (!match) return false;
      }
      return true;
    });

    switch (sortBy) {
      case 'precio_asc':
        return [...list].sort((a, b) => a.precioPorDia - b.precioPorDia);
      case 'precio_desc':
        return [...list].sort((a, b) => b.precioPorDia - a.precioPorDia);
      case 'nombre_asc':
        return [...list].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
      case 'stock_desc':
        return [...list].sort((a, b) => {
          const stockA = a.cantidadDisponible !== undefined ? a.cantidadDisponible : (a.cantidadTotal || 1);
          const stockB = b.cantidadDisponible !== undefined ? b.cantidadDisponible : (b.cantidadTotal || 1);
          return stockB - stockA;
        });
      case 'destacados':
      default:
        return list;
    }
  }, [tools, selectedCategory, onlyAvailable, maxPrice, searchQuery, sortBy]);

  const activeCategoryObj = useMemo(() => {
    if (!selectedCategory) return null;
    return categories.find(c => String(c.id) === String(selectedCategory)) || null;
  }, [categories, selectedCategory]);

  const hasActiveFilters = Boolean(
    selectedCategory !== null ||
    searchQuery.trim() !== '' ||
    onlyAvailable ||
    maxPrice < 2000000 ||
    sortBy !== 'destacados' ||
    quickStartDate !== '' ||
    quickEndDate !== ''
  );

  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (selectedCategory !== null) count++;
    if (searchQuery.trim() !== '') count++;
    if (onlyAvailable) count++;
    if (maxPrice < 2000000) count++;
    if (quickStartDate && quickEndDate) count++;
    return count;
  }, [selectedCategory, searchQuery, onlyAvailable, maxPrice, quickStartDate, quickEndDate]);

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setSearchQuery('');
    setMaxPrice(2000000);
    setOnlyAvailable(false);
    setSortBy('destacados');
    setQuickStartDate('');
    setQuickEndDate('');
    onClearInitialCategory?.();
  };

  const handleSelectCategory = (catId: string | number | null) => {
    setSelectedCategory(catId);
    if (catId === null) {
      onClearInitialCategory?.();
    }
  };

  const totalStockUnits = useMemo(() => {
    return tools.reduce((acc, t) => {
      const stock = t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1);
      return acc + (t.estadoOperativo === 'DISPONIBLE' ? stock : 0);
    }, 0);
  }, [tools]);

  // Controles de filtros compartidos entre sidebar desktop y drawer móvil
  const renderFilterControls = () => (
    <div className="space-y-6">
      {/* 1. Búsqueda de Texto */}
      <div className="space-y-2">
        <label htmlFor="search-input" className="text-xs font-bold uppercase tracking-wider text-neutral-700 block">
          Búsqueda de Equipo
        </label>
        <div className="relative">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            id="search-input"
            type="text"
            placeholder="Nombre, marca, modelo..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-9 py-2.5 text-xs bg-white border border-neutral-300 rounded-lg focus:outline-none focus:border-[#F5A623] focus:ring-1 focus:ring-[#F5A623] text-neutral-900 transition-colors shadow-xs"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-700 p-0.5 cursor-pointer"
              title="Borrar búsqueda"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* 2. Categorías */}
      <div className="space-y-2.5">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold uppercase tracking-wider text-neutral-700 block">
            Especialidad / Categoría
          </label>
          {selectedCategory && (
            <button
              type="button"
              onClick={() => handleSelectCategory(null)}
              className="text-[11px] font-semibold text-[#F5A623] hover:text-[#e09419] cursor-pointer"
            >
              Ver todas
            </button>
          )}
        </div>

        <div className="space-y-1 max-h-56 overflow-y-auto pr-1">
          <button
            type="button"
            onClick={() => handleSelectCategory(null)}
            className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium flex items-center justify-between transition-colors cursor-pointer ${
              selectedCategory === null
                ? 'bg-[#17191C] text-white font-bold shadow-xs'
                : 'text-neutral-700 hover:bg-neutral-100'
            }`}
          >
            <span>Todas las categorías</span>
            <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
              selectedCategory === null ? 'bg-[#292D32] text-[#F5A623]' : 'bg-neutral-200 text-neutral-600'
            }`}>
              {tools.length}
            </span>
          </button>

          {categories.map((cat) => {
            const isSelected = String(selectedCategory) === String(cat.id);
            const count = tools.filter(t => String(t.categoriaId) === String(cat.id)).length;
            return (
              <button
                key={String(cat.id)}
                type="button"
                onClick={() => handleSelectCategory(cat.id)}
                className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium flex items-center justify-between transition-colors cursor-pointer ${
                  isSelected
                    ? 'bg-[#17191C] text-[#F5A623] font-bold shadow-xs border-l-4 border-[#F5A623]'
                    : 'text-neutral-700 hover:bg-neutral-100'
                }`}
              >
                <span className="truncate pr-2">{cat.nombre}</span>
                <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded shrink-0 ${
                  isSelected ? 'bg-[#292D32] text-[#F5A623]' : 'bg-neutral-200 text-neutral-600'
                }`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. Rango de Tarifa Diaria en COP */}
      <div className="space-y-2 bg-neutral-50 p-3.5 rounded-xl border border-neutral-200">
        <div className="flex items-center justify-between text-xs">
          <span className="font-bold text-neutral-700 uppercase tracking-wider text-[11px]">
            Tarifa Máx / Día
          </span>
          <span className="font-mono font-bold text-[#17191C] bg-white px-2 py-0.5 rounded border border-neutral-200 text-xs">
            ${(maxPrice).toLocaleString('es-CO')} COP
          </span>
        </div>
        <input
          type="range"
          min="50000"
          max="2000000"
          step="50000"
          value={maxPrice}
          onChange={(e) => setMaxPrice(Number(e.target.value))}
          aria-label="Filtro de tarifa máxima por día"
          className="w-full accent-[#F5A623] cursor-pointer"
        />
        <div className="flex justify-between text-[10px] text-neutral-400 font-mono">
          <span>$50k</span>
          <span>$1.000k</span>
          <span>$2.000k COP</span>
        </div>
      </div>

      {/* 4. Filtro de Disponibilidad de Stock */}
      <div className="space-y-2">
        <label className="flex items-center gap-2.5 p-3 bg-neutral-50 hover:bg-neutral-100 rounded-xl border border-neutral-200 cursor-pointer transition-colors">
          <input
            type="checkbox"
            checked={onlyAvailable}
            onChange={(e) => setOnlyAvailable(e.target.checked)}
            className="w-4 h-4 rounded text-[#F5A623] focus:ring-[#F5A623] accent-[#F5A623] cursor-pointer"
          />
          <div className="text-xs">
            <span className="font-semibold text-neutral-800 block">Solo unidades en stock</span>
            <span className="text-[11px] text-neutral-500 block">Excluye maquinaria temporalmente comprometida</span>
          </div>
        </label>
      </div>

      {/* 5. Estimador Rápido de Periodo de Obra */}
      <div className="space-y-2.5 bg-neutral-50 p-3.5 rounded-xl border border-neutral-200">
        <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-700 uppercase tracking-wider text-[11px]">
          <Calendar className="w-3.5 h-3.5 text-[#F5A623]" />
          <span>Calcular por Periodo</span>
        </div>
        <div className="grid grid-cols-2 gap-2 text-xs">
          <div>
            <label className="text-[10px] font-medium text-neutral-500 block mb-1">Inicio:</label>
            <input
              type="date"
              value={quickStartDate}
              onChange={(e) => setQuickStartDate(e.target.value)}
              className="w-full bg-white border border-neutral-300 rounded-md px-2 py-1.5 text-xs focus:ring-1 focus:ring-[#F5A623] focus:outline-none"
            />
          </div>
          <div>
            <label className="text-[10px] font-medium text-neutral-500 block mb-1">Fin:</label>
            <input
              type="date"
              value={quickEndDate}
              onChange={(e) => setQuickEndDate(e.target.value)}
              className="w-full bg-white border border-neutral-300 rounded-md px-2 py-1.5 text-xs focus:ring-1 focus:ring-[#F5A623] focus:outline-none"
            />
          </div>
        </div>

        {quickStartDate && quickEndDate && (
          <div className="pt-2 border-t border-neutral-200 text-xs flex items-center justify-between font-mono">
            <span className="text-neutral-500 text-[11px]">Duración obra:</span>
            <span className="font-bold text-neutral-900 bg-white px-2 py-0.5 rounded border border-neutral-200">
              {currentDays} {currentDays === 1 ? 'día' : 'días'}
            </span>
          </div>
        )}
      </div>

      {/* 6. Acción de Reset */}
      {hasActiveFilters && (
        <button
          type="button"
          onClick={handleResetFilters}
          className="w-full py-2.5 px-3 bg-white hover:bg-neutral-100 text-neutral-700 text-xs font-bold rounded-lg border border-neutral-300 flex items-center justify-center gap-2 transition-colors cursor-pointer shadow-xs"
        >
          <RotateCcw className="w-3.5 h-3.5 text-[#F5A623]" />
          <span>Limpiar todos los filtros</span>
        </button>
      )}
    </div>
  );

  return (
    <div className="w-full bg-neutral-50 min-h-screen">
      {/* =========================================================================
          1. ENCABEZADO INDUSTRIAL DEL CATÁLOGO (Enfocado, sin duplicar hero)
         ========================================================================= */}
      <section className="bg-[#17191C] text-[#F4F5F6] border-b border-[#292D32] py-8 sm:py-10 relative overflow-hidden">
        {/* Patrón reticular sutil */}
        <div
          className="absolute inset-0 opacity-[0.035] pointer-events-none"
          style={{
            backgroundImage: `radial-gradient(#F5A623 1px, transparent 1px)`,
            backgroundSize: '24px 24px'
          }}
          aria-hidden="true"
        />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div>
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded bg-[#292D32] text-[#F5A623] text-xs font-bold uppercase tracking-wider mb-2.5 border border-[#F5A623]/25">
                <HardHat className="w-3.5 h-3.5" />
                <span>Catálogo Técnico de Equipos</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F5F6]">
                Inventario Operativo en Tiempo Real
              </h1>
              <p className="mt-1.5 text-xs sm:text-sm text-neutral-300 max-w-2xl leading-relaxed">
                Consulta disponibilidad verificada, especificaciones técnicas y tarifas diarias en pesos colombianos con facturación oficial e IVA del 19 %.
              </p>
            </div>

            {/* Micro-contadores informativos */}
            <div className="flex items-center gap-4 bg-[#1F2226] p-3 rounded-xl border border-[#292D32] shrink-0">
              <div className="px-3 border-r border-[#292D32] text-center">
                <span className="block text-xl font-mono font-bold text-[#F4F5F6]">
                  {loading ? '—' : tools.length}
                </span>
                <span className="text-[10px] text-neutral-400 font-medium">Equipos</span>
              </div>
              <div className="px-3 border-r border-[#292D32] text-center">
                <span className="block text-xl font-mono font-bold text-[#F5A623]">
                  {loading ? '—' : totalStockUnits}
                </span>
                <span className="text-[10px] text-neutral-400 font-medium">En Stock</span>
              </div>
              <div className="px-3 text-center">
                <span className="block text-xl font-mono font-bold text-[#F4F5F6]">
                  {loading ? '—' : categories.length}
                </span>
                <span className="text-[10px] text-neutral-400 font-medium">Categorías</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          2. CONTENIDO PRINCIPAL: SIDEBAR + GRILLA DE RESULTADOS
         ========================================================================= */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Sidebar Desktop (visible en pantallas grandes) */}
          <aside className="hidden lg:block lg:col-span-3 sticky top-20 bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-neutral-200">
              <div className="flex items-center gap-2">
                <Filter className="w-4 h-4 text-[#F5A623]" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-neutral-900">
                  Filtros del Catálogo
                </h3>
              </div>
              {activeFiltersCount > 0 && (
                <span className="text-[11px] font-mono font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-900">
                  {activeFiltersCount} activo{activeFiltersCount > 1 ? 's' : ''}
                </span>
              )}
            </div>

            {renderFilterControls()}
          </aside>

          {/* Área Principal de Resultados (lg:col-span-9) */}
          <main className="lg:col-span-9 space-y-6">
            {/* Barra Superior de Control: Disparador Móvil, Contador de Resultados y Orden */}
            <div className="bg-white rounded-xl border border-neutral-200 p-4 shadow-xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
              {/* Botón Disparador de Filtros en Móvil */}
              <div className="lg:hidden flex items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={() => setMobileFiltersOpen(true)}
                  className="inline-flex items-center gap-2 px-3.5 py-2 bg-[#17191C] text-white text-xs font-bold rounded-lg cursor-pointer hover:bg-neutral-800 transition-colors shadow-xs"
                >
                  <SlidersHorizontal className="w-4 h-4 text-[#F5A623]" />
                  <span>Filtros y Búsqueda</span>
                  {activeFiltersCount > 0 && (
                    <span className="ml-1 w-5 h-5 rounded-full bg-[#F5A623] text-[#17191C] font-mono text-[10px] font-bold flex items-center justify-center">
                      {activeFiltersCount}
                    </span>
                  )}
                </button>

                <span className="text-xs font-semibold text-neutral-600 font-mono">
                  {filteredTools.length} equipo{filteredTools.length === 1 ? '' : 's'}
                </span>
              </div>

              {/* Conteo Desktop */}
              <div className="hidden lg:flex items-center gap-2 text-xs text-neutral-600">
                <span className="font-medium">
                  Mostrando <strong className="text-neutral-900 font-bold font-mono">{filteredTools.length}</strong> de {tools.length} equipos disponibles
                </span>
              </div>

              {/* Selector de Ordenamiento */}
              <div className="flex items-center gap-2.5 self-end sm:self-auto w-full sm:w-auto">
                <label htmlFor="sort-select" className="text-xs font-semibold text-neutral-600 whitespace-nowrap hidden sm:inline">
                  Ordenar:
                </label>
                <div className="relative w-full sm:w-48">
                  <ArrowUpDown className="w-3.5 h-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <select
                    id="sort-select"
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value as SortOption)}
                    aria-label="Criterio de ordenamiento de maquinaria"
                    className="w-full pl-8 pr-3 py-2 text-xs bg-neutral-50 hover:bg-white border border-neutral-300 rounded-lg text-neutral-800 font-medium focus:outline-none focus:border-[#F5A623] cursor-pointer"
                  >
                    <option value="destacados">Destacados (Original)</option>
                    <option value="precio_asc">Tarifa: Menor a Mayor</option>
                    <option value="precio_desc">Tarifa: Mayor a Menor</option>
                    <option value="nombre_asc">Nombre: A - Z</option>
                    <option value="stock_desc">Mayor Stock Disponible</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Chips de Filtros Activos con Botón 'X' */}
            {hasActiveFilters && (
              <div className="flex flex-wrap items-center gap-2 pt-1 pb-1">
                <span className="text-[11px] font-semibold text-neutral-500 uppercase tracking-wider">
                  Filtros activos:
                </span>

                {/* Chip Categoría */}
                {selectedCategory !== null && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-900 text-[#F5A623] text-xs font-semibold">
                    <span>Categoría: {activeCategoryObj?.nombre || selectedCategory}</span>
                    <button
                      type="button"
                      onClick={() => handleSelectCategory(null)}
                      className="hover:text-white cursor-pointer"
                      title="Quitar filtro de categoría"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                )}

                {/* Chip Búsqueda */}
                {searchQuery.trim() !== '' && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-200 text-neutral-800 text-xs font-semibold">
                    <span>Búsqueda: "{searchQuery}"</span>
                    <button
                      type="button"
                      onClick={() => setSearchQuery('')}
                      className="hover:text-neutral-900 cursor-pointer"
                      title="Quitar búsqueda"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                )}

                {/* Chip Precio Máximo */}
                {maxPrice < 2000000 && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-200 text-neutral-800 text-xs font-semibold">
                    <span>Máx: ${(maxPrice).toLocaleString('es-CO')}</span>
                    <button
                      type="button"
                      onClick={() => setMaxPrice(2000000)}
                      className="hover:text-neutral-900 cursor-pointer"
                      title="Quitar límite de precio"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                )}

                {/* Chip Solo Disponibles */}
                {onlyAvailable && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-emerald-100 text-emerald-900 text-xs font-semibold">
                    <span>Solo en stock</span>
                    <button
                      type="button"
                      onClick={() => setOnlyAvailable(false)}
                      className="hover:text-emerald-950 cursor-pointer"
                      title="Quitar filtro de stock"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                )}

                {/* Chip Periodo */}
                {quickStartDate && quickEndDate && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-200 text-neutral-800 text-xs font-semibold">
                    <span>Periodo: {currentDays} días</span>
                    <button
                      type="button"
                      onClick={() => { setQuickStartDate(''); setQuickEndDate(''); }}
                      className="hover:text-neutral-900 cursor-pointer"
                      title="Quitar periodo"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                )}

                {/* Limpiar todo */}
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="text-xs font-bold text-rose-600 hover:text-rose-800 underline ml-2 cursor-pointer"
                >
                  Restablecer todo
                </button>
              </div>
            )}

            {/* Estado de Error de Servidor */}
            {error && (
              <div className="p-6 rounded-2xl border border-rose-200 bg-rose-50 text-rose-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <AlertCircle className="w-6 h-6 text-rose-600 shrink-0" />
                  <div>
                    <h3 className="text-sm font-bold">Error de conexión con el catálogo</h3>
                    <p className="text-xs text-rose-700 mt-0.5">{error}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={loadData}
                  className="px-4 py-2 bg-rose-600 text-white text-xs font-bold rounded-lg hover:bg-rose-700 transition-colors cursor-pointer shrink-0"
                >
                  Reintentar conexión
                </button>
              </div>
            )}

            {/* Grilla de Herramientas o Estados Vacíos */}
            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {[1, 2, 3, 4, 5, 6].map((n) => (
                  <div key={n} className="bg-white rounded-2xl border border-neutral-200 p-4 animate-pulse space-y-3">
                    <div className="h-44 bg-neutral-200 rounded-xl" />
                    <div className="h-4 bg-neutral-200 rounded w-3/4" />
                    <div className="h-3 bg-neutral-100 rounded w-1/2" />
                    <div className="h-8 bg-neutral-200 rounded w-1/3 pt-2" />
                  </div>
                ))}
              </div>
            ) : filteredTools.length === 0 ? (
              <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <div className="w-14 h-14 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto mb-4 border border-amber-200">
                  <Search className="w-7 h-7" />
                </div>
                <h3 className="text-base font-bold text-neutral-900">
                  No se encontraron equipos coincidentes
                </h3>
                <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1.5 mb-6 leading-relaxed">
                  No hay maquinaria que cumpla los criterios actuales de búsqueda o rango de precio. Intenta ajustar los filtros aplicados.
                </p>
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="inline-flex items-center gap-2 px-4 py-2.5 bg-[#17191C] hover:bg-neutral-800 text-white text-xs font-bold rounded-lg transition-colors cursor-pointer shadow-xs"
                >
                  <RotateCcw className="w-3.5 h-3.5 text-[#F5A623]" />
                  <span>Restablecer todos los filtros</span>
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {filteredTools.map((tool) => {
                  const stockDisp = tool.cantidadDisponible !== undefined ? tool.cantidadDisponible : (tool.cantidadTotal || 1);
                  const isAvailable = tool.estadoOperativo === 'DISPONIBLE' && stockDisp > 0;
                  const subtotalEstimado = tool.precioPorDia * currentDays;
                  const ivaEstimado = Math.round(subtotalEstimado * IVA_RATE);
                  const totalEstimado = subtotalEstimado + ivaEstimado;

                  // Nombre de categoría
                  const catName = categories.find(c => String(c.id) === String(tool.categoriaId))?.nombre || 'Construcción';

                  return (
                    <article
                      key={String(tool.id)}
                      className="bg-white rounded-2xl border border-neutral-200 hover:border-neutral-300 overflow-hidden flex flex-col justify-between transition-all duration-200 hover:shadow-md group"
                    >
                      <div>
                        {/* Fotografía de la Maquinaria */}
                        <div className="relative h-48 bg-neutral-100 overflow-hidden">
                          <img
                            src={resolveToolImage({ ...tool, categoriaNombre: catName })}
                            alt={tool.nombre}
                            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-104"
                            onError={(e) => {
                              (e.target as HTMLImageElement).src = defaultExcavator;
                            }}
                          />
                          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60" />

                          {/* Tag de Categoría en esquina superior izquierda */}
                          <span className="absolute top-2.5 left-2.5 bg-[#17191C]/90 backdrop-blur-xs text-[10px] font-bold uppercase tracking-wider text-[#F5A623] px-2 py-0.5 rounded border border-[#292D32]">
                            {catName}
                          </span>

                          {/* Badge de disponibilidad de stock real en esquina superior derecha */}
                          <div className="absolute top-2.5 right-2.5 bg-[#17191C]/90 backdrop-blur-xs text-white text-[11px] font-semibold px-2.5 py-1 rounded-md flex items-center gap-1.5 shadow-xs border border-[#292D32]">
                            {isAvailable ? (
                              <>
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                                <span>{stockDisp} en stock</span>
                              </>
                            ) : (
                              <>
                                <AlertCircle className="w-3.5 h-3.5 text-rose-400" />
                                <span>Sin stock</span>
                              </>
                            )}
                          </div>
                        </div>

                        {/* Metadatos y Descripción */}
                        <div className="p-4">
                          <div className="flex items-center gap-1.5 text-[11px] font-medium text-neutral-500 mb-1 truncate">
                            <span className="font-semibold text-neutral-700">{tool.marca || 'Estándar'}</span>
                            <span aria-hidden="true">·</span>
                            <span>Mod. {tool.modelo || 'N/A'}</span>
                            <span aria-hidden="true">·</span>
                            <span>Total: {tool.cantidadTotal || 1}</span>
                          </div>

                          <h3
                            onClick={() => onSelectTool(tool)}
                            className="text-sm font-bold text-neutral-900 line-clamp-2 hover:text-[#F5A623] transition-colors cursor-pointer mb-1.5"
                            title={tool.nombre}
                          >
                            {tool.nombre}
                          </h3>

                          <p className="text-xs text-neutral-600 line-clamp-2 mb-2 leading-relaxed">
                            {tool.descripcion || 'Maquinaria de alto rendimiento para obras civiles y construcción en general.'}
                          </p>
                        </div>
                      </div>

                      {/* Pie de Tarjeta: Tarifas y Acciones */}
                      <div className="p-4 pt-0 border-t border-neutral-100 mt-2">
                        <div className="pt-3 mb-3">
                          <div className="flex items-baseline justify-between">
                            <div>
                              <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-semibold">
                                Tarifa Diaria
                              </span>
                              <span className="text-lg font-extrabold text-neutral-900 font-mono tabular-nums">
                                ${tool.precioPorDia.toLocaleString('es-CO')}
                              </span>
                              <span className="text-[11px] text-neutral-500 font-medium ml-1">COP</span>
                            </div>
                            <span className="text-[10px] font-bold text-neutral-600 bg-neutral-100 px-1.5 py-0.5 rounded border border-neutral-200">
                              + IVA 19%
                            </span>
                          </div>

                          {/* Desglose de cálculo de periodo */}
                          {currentDays > 1 && (
                            <div className="mt-2 pt-2 border-t border-dashed border-neutral-200 text-[11px] text-neutral-600 flex justify-between items-center">
                              <span>Total ({currentDays} días + IVA):</span>
                              <span className="font-mono font-bold text-neutral-900 tabular-nums">
                                ${totalEstimado.toLocaleString('es-CO')} COP
                              </span>
                            </div>
                          )}
                        </div>

                        {/* Botones de Acción */}
                        <div className="grid grid-cols-2 gap-2">
                          <button
                            type="button"
                            onClick={() => onSelectTool(tool)}
                            className="w-full py-2.5 px-2 text-xs font-semibold text-neutral-800 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer text-center"
                          >
                            Ver Ficha
                          </button>
                          <button
                            type="button"
                            onClick={() => onBookTool(tool)}
                            disabled={!isAvailable}
                            className={`w-full py-2.5 px-2 text-xs font-bold rounded-lg transition-colors cursor-pointer text-center ${
                              isAvailable
                                ? 'bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] shadow-xs'
                                : 'bg-neutral-200 text-neutral-400 cursor-not-allowed'
                            }`}
                          >
                            {isAvailable ? 'Reservar' : 'Sin Stock'}
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </main>
        </div>
      </div>

      {/* =========================================================================
          3. DRAWER MÓVIL DE FILTROS ACCESIBLE
         ========================================================================= */}
      {mobileFiltersOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="mobile-filter-title"
          className="fixed inset-0 z-50 flex lg:hidden"
        >
          {/* Backdrop con desenfoque */}
          <div
            onClick={() => setMobileFiltersOpen(false)}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity"
            aria-hidden="true"
          />

          {/* Panel deslizante */}
          <div className="relative ml-auto w-full max-w-xs sm:max-w-sm bg-white h-full shadow-2xl flex flex-col justify-between z-10 animate-in slide-in-from-right duration-200">
            {/* Header del Drawer */}
            <div className="px-5 py-4 border-b border-neutral-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-[#F5A623]" />
                <h2 id="mobile-filter-title" className="text-sm font-bold text-neutral-900">
                  Filtros del Catálogo
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setMobileFiltersOpen(false)}
                className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg cursor-pointer"
                aria-label="Cerrar filtros"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Contenido con scroll */}
            <div className="p-5 overflow-y-auto flex-1">
              {renderFilterControls()}
            </div>

            {/* Footer de acción del Drawer */}
            <div className="p-4 border-t border-neutral-200 bg-neutral-50 flex items-center gap-2">
              <button
                type="button"
                onClick={handleResetFilters}
                className="w-1/3 py-2.5 text-xs font-bold text-neutral-700 bg-white border border-neutral-300 rounded-lg hover:bg-neutral-100 cursor-pointer text-center"
              >
                Limpiar
              </button>
              <button
                type="button"
                onClick={() => setMobileFiltersOpen(false)}
                className="w-2/3 py-2.5 text-xs font-bold text-[#17191C] bg-[#F5A623] hover:bg-[#e09419] rounded-lg cursor-pointer text-center shadow-xs"
              >
                Ver {filteredTools.length} equipo{filteredTools.length === 1 ? '' : 's'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
