import React, { useState, useEffect, useMemo } from 'react';
import { Tool, Category } from '../types';
import { resolveToolImage, defaultExcavator } from '../utils/imageResolver';
import { api } from '../services/api';
import heroExcavator from '../assets/images/heavy_excavator_construction_1792091729475.jpg';
import {
  Search,
  SlidersHorizontal,
  Calendar,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  RotateCcw,
  Truck,
  FileText,
  ShieldCheck,
  Layers,
  ArrowUpDown,
  Tag,
  Building2,
  Clock
} from 'lucide-react';

const IVA_RATE = 0.19;

type SortOption = 'destacados' | 'precio_asc' | 'precio_desc' | 'nombre_asc' | 'stock_desc';

interface CatalogViewProps {
  onSelectTool: (tool: Tool) => void;
  onBookTool: (tool: Tool) => void;
}

export const CatalogView: React.FC<CatalogViewProps> = ({ onSelectTool, onBookTool }) => {
  const [tools, setTools] = useState<Tool[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | number | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [maxPrice, setMaxPrice] = useState<number>(2000000);
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [sortBy, setSortBy] = useState<SortOption>('destacados');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Estimador rápido de periodo
  const [quickStartDate, setQuickStartDate] = useState('');
  const [quickEndDate, setQuickEndDate] = useState('');

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

  const calculateDays = (): number => {
    if (!quickStartDate || !quickEndDate) return 1;
    const s = new Date(quickStartDate);
    const e = new Date(quickEndDate);
    if (e < s) return 1;
    const diff = Math.ceil(Math.abs(e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
    return diff === 0 ? 1 : diff;
  };

  const currentDays = calculateDays();

  // Filtrado y ordenamiento combinados
  const filteredTools = useMemo(() => {
    const list = tools.filter(tool => {
      if (selectedCategory && tool.categoriaId !== selectedCategory) return false;
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

  const hasActiveFilters = Boolean(
    selectedCategory !== null ||
    searchQuery.trim() !== '' ||
    onlyAvailable ||
    maxPrice < 2000000 ||
    sortBy !== 'destacados' ||
    quickStartDate !== '' ||
    quickEndDate !== ''
  );

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setSearchQuery('');
    setMaxPrice(2000000);
    setOnlyAvailable(false);
    setSortBy('destacados');
    setQuickStartDate('');
    setQuickEndDate('');
  };

  const scrollToCatalog = () => {
    const element = document.getElementById('catalogo-equipos');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const scrollToHowItWorks = () => {
    const element = document.getElementById('como-funciona');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Cálculo de métricas reales para el Hero
  const totalEquiposDisponibles = useMemo(() => {
    return tools.reduce((acc, t) => {
      const stock = t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1);
      return acc + (t.estadoOperativo === 'DISPONIBLE' ? stock : 0);
    }, 0);
  }, [tools]);

  return (
    <div className="w-full">
      {/* =========================================================================
          1. HERO PRINCIPAL OBRIXEN (Editorial Industrial & Paleta Carbón / Ámbar)
         ========================================================================= */}
      <section className="bg-[#17191C] text-[#F4F5F6] border-b border-[#292D32] relative overflow-hidden">
        {/* Patrón sutil técnico de fondo */}
        <div
          className="absolute inset-0 opacity-[0.03] pointer-events-none"
          style={{
            backgroundImage: `radial-gradient(#F5A623 1px, transparent 1px)`,
            backgroundSize: '24px 24px'
          }}
          aria-hidden="true"
        />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-20 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-12 items-center">
            {/* Columna Izquierda: Mensaje Comercial e Identidad */}
            <div className="lg:col-span-7 flex flex-col items-start">
              {/* Etiqueta superior */}
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-[#292D32]/80 border border-[#F5A623]/30 text-[#F5A623] text-xs font-bold tracking-wider uppercase mb-5">
                <span className="w-2 h-2 rounded-full bg-[#F5A623] animate-pulse" aria-hidden="true" />
                <span>Alquiler de equipos y herramientas</span>
              </div>

              {/* Título Principal */}
              <h1 className="text-3xl sm:text-5xl lg:text-5xl font-extrabold tracking-tight text-[#F4F5F6] leading-[1.15] mb-4">
                El equipo que tu obra necesita.
              </h1>

              {/* Texto descriptivo */}
              <p className="text-base sm:text-lg text-neutral-300 max-w-2xl leading-relaxed mb-8">
                Encuentra y alquila herramientas y equipos de construcción para cada etapa de tu proyecto.
                Disponibilidad en tiempo real, facturación oficial con IVA del 19 % y reservas protegidas.
              </p>

              {/* CTAs */}
              <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto mb-10">
                <button
                  onClick={scrollToCatalog}
                  className="w-full sm:w-auto px-6 py-3.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-bold text-sm rounded-lg shadow-sm transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer group"
                >
                  <span>Explorar equipos</span>
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                </button>
                <button
                  onClick={scrollToHowItWorks}
                  className="w-full sm:w-auto px-5 py-3.5 bg-[#292D32] hover:bg-[#343a40] text-[#F4F5F6] font-semibold text-sm rounded-lg border border-neutral-700 transition-colors duration-200 flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Cómo funciona</span>
                </button>
              </div>

              {/* Indicadores con datos reales verificables */}
              <div className="grid grid-cols-3 gap-4 pt-6 border-t border-[#292D32] w-full max-w-lg">
                <div>
                  <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                    {loading ? '...' : tools.length}
                  </span>
                  <span className="text-xs text-neutral-400 font-medium">Equipos en catálogo</span>
                </div>
                <div>
                  <span className="text-2xl font-extrabold text-[#F5A623] font-mono tabular-nums block">
                    {loading ? '...' : totalEquiposDisponibles}
                  </span>
                  <span className="text-xs text-neutral-400 font-medium">Unidades en stock</span>
                </div>
                <div>
                  <span className="text-2xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                    {loading ? '...' : categories.length}
                  </span>
                  <span className="text-xs text-neutral-400 font-medium">Categorías activas</span>
                </div>
              </div>
            </div>

            {/* Columna Derecha: Tarjeta Fotográfica Industrial */}
            <div className="lg:col-span-5">
              <div className="relative rounded-2xl overflow-hidden border border-[#292D32] bg-[#292D32]/40 shadow-2xl group">
                <div className="relative h-72 sm:h-96 w-full overflow-hidden">
                  <img
                    src={heroExcavator}
                    alt="Maquinaria pesada para construcción en obra"
                    className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#17191C] via-[#17191C]/30 to-transparent" />
                </div>

                {/* Micro-tarjeta flotante con información del sistema */}
                <div className="p-5 bg-[#17191C]/90 backdrop-blur-md border-t border-[#292D32]">
                  <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#F5A623]/10 border border-[#F5A623]/30 flex items-center justify-center shrink-0">
                        <Truck className="w-5 h-5 text-[#F5A623]" />
                      </div>
                      <div>
                        <span className="text-xs font-bold text-[#F4F5F6] block">
                          Logística y Entrega en Obra
                        </span>
                        <span className="text-[11px] text-neutral-400 block">
                          Acta de recepción y facturación oficial COP
                        </span>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold font-mono uppercase px-2 py-1 rounded bg-[#292D32] text-[#F5A623] border border-[#292D32]">
                      IVA 19%
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          2. SECCIÓN "CÓMO FUNCIONA" (3 Pasos Comerciales Transparentes)
         ========================================================================= */}
      <section id="como-funciona" className="bg-white border-b border-neutral-200 py-12 scroll-mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-10">
            <span className="text-xs font-bold tracking-wider uppercase text-[#F5A623] block mb-1">
              Proceso de Alquiler
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-[#17191C]">
              Cómo funciona en OBRIXEN
            </h2>
            <p className="text-sm text-neutral-600 mt-2">
              Un flujo simple, seguro y transparente para contratistas, ingenieros y maestros de obra.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Paso 1 */}
            <div className="p-6 rounded-xl border border-neutral-200 bg-neutral-50/60 hover:border-neutral-300 transition-colors">
              <div className="w-10 h-10 rounded-lg bg-[#17191C] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-4 text-sm">
                01
              </div>
              <h3 className="text-base font-bold text-[#17191C] mb-2">
                Explora y Selecciona
              </h3>
              <p className="text-xs text-neutral-600 leading-relaxed">
                Consulta maquinaria pesada, herramientas y equipos con tarifas claras por día en COP y disponibilidad de stock en tiempo real.
              </p>
            </div>

            {/* Paso 2 */}
            <div className="p-6 rounded-xl border border-neutral-200 bg-neutral-50/60 hover:border-neutral-300 transition-colors">
              <div className="w-10 h-10 rounded-lg bg-[#17191C] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-4 text-sm">
                02
              </div>
              <h3 className="text-base font-bold text-[#17191C] mb-2">
                Reserva y Pago Protegido
              </h3>
              <p className="text-xs text-neutral-600 leading-relaxed">
                Define las fechas de tu obra. El sistema valida el inventario y procesa el pago a través de pasarela segura protegida con clave de idempotencia.
              </p>
            </div>

            {/* Paso 3 */}
            <div className="p-6 rounded-xl border border-neutral-200 bg-neutral-50/60 hover:border-neutral-300 transition-colors">
              <div className="w-10 h-10 rounded-lg bg-[#17191C] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-4 text-sm">
                03
              </div>
              <h3 className="text-base font-bold text-[#17191C] mb-2">
                Entrega en Obra y Factura
              </h3>
              <p className="text-xs text-neutral-600 leading-relaxed">
                El propietario despacha la máquina con inspección técnica. Recibes tu factura legal con IVA del 19 % descargable en PDF.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          3. CATÁLOGO PRINCIPAL, FILTROS Y TARJETAS
         ========================================================================= */}
      <section id="catalogo-equipos" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 scroll-mt-16">
        {/* Encabezado del Catálogo */}
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-6 border-b border-neutral-200 mb-8">
          <div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-[#17191C] tracking-tight">
              Encuentra el equipo adecuado
            </h2>
            <p className="mt-1 text-sm text-neutral-600 max-w-2xl">
              Explora maquinaria, herramientas y andamiajes con especificaciones operativas y tarifas por día.
            </p>
          </div>

          {/* Quick Date Estimator Box */}
          <div className="bg-neutral-100 p-3 rounded-xl border border-neutral-200 flex flex-wrap items-center gap-2.5 shrink-0 text-xs">
            <div className="flex items-center gap-1.5 font-semibold text-neutral-700">
              <Calendar className="w-4 h-4 text-[#F5A623]" />
              <span>Estimar periodo:</span>
            </div>
            <input
              type="date"
              value={quickStartDate}
              onChange={(e) => setQuickStartDate(e.target.value)}
              aria-label="Fecha inicio estimación"
              className="bg-white border border-neutral-300 rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-[#F5A623] text-neutral-800"
            />
            <span className="text-neutral-400 font-medium">a</span>
            <input
              type="date"
              value={quickEndDate}
              onChange={(e) => setQuickEndDate(e.target.value)}
              aria-label="Fecha fin estimación"
              className="bg-white border border-neutral-300 rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-[#F5A623] text-neutral-800"
            />
            {quickStartDate && quickEndDate && (
              <span className="font-bold text-[#17191C] bg-white border border-neutral-300 px-2 py-1 rounded-md font-mono tabular-nums">
                {currentDays} {currentDays === 1 ? 'día' : 'días'}
              </span>
            )}
          </div>
        </div>

        {/* Barra de Filtros Superior: Búsqueda, Rango de Precio, Toggle y Ordenamiento */}
        <div className="bg-white rounded-xl border border-neutral-200 p-4 mb-6 shadow-xs">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
            {/* Buscador */}
            <div className="md:col-span-4 relative">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por equipo, marca o modelo..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 text-xs sm:text-sm bg-neutral-50 border border-neutral-200 rounded-lg focus:outline-none focus:border-[#F5A623] focus:bg-white focus:ring-1 focus:ring-[#F5A623] text-neutral-900 transition-colors"
              />
            </div>

            {/* Slider de Precio Máximo Diario */}
            <div className="md:col-span-3 flex items-center justify-between gap-2 px-3 py-1.5 bg-neutral-50 border border-neutral-200 rounded-lg text-xs">
              <span className="text-neutral-500 whitespace-nowrap font-medium">Máx/día:</span>
              <span className="font-mono font-bold text-neutral-900 tabular-nums whitespace-nowrap">
                ${(maxPrice / 1000).toLocaleString('es-CO')}k
              </span>
              <input
                type="range"
                min="50000"
                max="2000000"
                step="50000"
                value={maxPrice}
                onChange={(e) => setMaxPrice(Number(e.target.value))}
                aria-label="Filtro de precio diario máximo"
                className="w-20 sm:w-24 accent-[#F5A623] cursor-pointer"
              />
            </div>

            {/* Toggle de Disponibilidad */}
            <div className="md:col-span-2 flex items-center">
              <label className="flex items-center gap-2 text-xs font-semibold text-neutral-700 bg-neutral-50 border border-neutral-200 rounded-lg px-3 py-2 cursor-pointer w-full hover:bg-neutral-100 transition-colors">
                <input
                  type="checkbox"
                  checked={onlyAvailable}
                  onChange={(e) => setOnlyAvailable(e.target.checked)}
                  className="rounded text-[#F5A623] focus:ring-[#F5A623] accent-[#F5A623]"
                />
                <span className="truncate">Solo disponibles</span>
              </label>
            </div>

            {/* Selector de Ordenamiento */}
            <div className="md:col-span-3 flex items-center gap-2">
              <div className="relative w-full">
                <ArrowUpDown className="w-3.5 h-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as SortOption)}
                  aria-label="Ordenar equipos"
                  className="w-full pl-8 pr-3 py-2 text-xs bg-neutral-50 border border-neutral-200 rounded-lg text-neutral-700 font-medium focus:outline-none focus:border-[#F5A623] focus:bg-white cursor-pointer"
                >
                  <option value="destacados">Orden: Destacados</option>
                  <option value="precio_asc">Precio: Menor a Mayor</option>
                  <option value="precio_desc">Precio: Mayor a Menor</option>
                  <option value="nombre_asc">Nombre: A - Z</option>
                  <option value="stock_desc">Disponibilidad de Stock</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Chips de Categorías y Contador de Resultados */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          {/* Chips horizontales */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-2 scrollbar-thin flex-1">
            <button
              onClick={() => setSelectedCategory(null)}
              className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap cursor-pointer shrink-0 ${
                selectedCategory === null
                  ? 'bg-[#17191C] text-white shadow-xs'
                  : 'bg-white text-neutral-600 border border-neutral-200 hover:text-neutral-900 hover:bg-neutral-50'
              }`}
            >
              Todas ({tools.length})
            </button>
            {categories.map((cat) => {
              const count = tools.filter(t => t.categoriaId === cat.id).length;
              const isSelected = selectedCategory === cat.id;
              return (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap cursor-pointer shrink-0 ${
                    isSelected
                      ? 'bg-[#17191C] text-[#F5A623] shadow-xs border border-[#17191C]'
                      : 'bg-white text-neutral-600 border border-neutral-200 hover:text-neutral-900 hover:bg-neutral-50'
                  }`}
                >
                  {cat.nombre} ({count})
                </button>
              );
            })}
          </div>

          {/* Contador y Limpieza de Filtros */}
          <div className="flex items-center gap-3 shrink-0 self-end sm:self-auto text-xs">
            <span className="text-neutral-500 font-medium">
              Mostrando <strong className="text-[#17191C] font-bold">{filteredTools.length}</strong> de {tools.length} equipos
            </span>
            {hasActiveFilters && (
              <button
                onClick={handleResetFilters}
                className="inline-flex items-center gap-1 text-xs font-bold text-[#F5A623] hover:text-[#e09419] bg-amber-50 px-2.5 py-1 rounded-md border border-[#F5A623]/30 transition-colors cursor-pointer"
                title="Limpiar todos los filtros aplicados"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Limpiar</span>
              </button>
            )}
          </div>
        </div>

        {/* Estado de Error de API */}
        {error && (
          <div className="p-6 mb-8 rounded-xl border border-rose-200 bg-rose-50 text-rose-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <AlertCircle className="w-6 h-6 text-rose-600 shrink-0" />
              <div>
                <h3 className="text-sm font-bold">Error al consultar el catálogo</h3>
                <p className="text-xs text-rose-700 mt-0.5">{error}</p>
              </div>
            </div>
            <button
              onClick={loadData}
              className="px-4 py-2 bg-rose-600 text-white text-xs font-bold rounded-lg hover:bg-rose-700 transition-colors cursor-pointer shrink-0"
            >
              Reintentar conexión
            </button>
          </div>
        )}

        {/* Grilla de Equipos / Maquinaria */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
              <div key={n} className="bg-white rounded-xl border border-neutral-200 p-4 animate-pulse">
                <div className="h-48 bg-neutral-200 rounded-lg mb-3" />
                <div className="h-4 bg-neutral-200 rounded w-3/4 mb-2" />
                <div className="h-3 bg-neutral-100 rounded w-1/2 mb-4" />
                <div className="h-8 bg-neutral-200 rounded w-1/3" />
              </div>
            ))}
          </div>
        ) : filteredTools.length === 0 ? (
          <div className="text-center py-16 px-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
            <div className="w-12 h-12 rounded-xl bg-neutral-100 flex items-center justify-center mx-auto mb-3 text-neutral-400">
              <Search className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#17191C]">No se encontraron equipos</h3>
            <p className="text-xs text-neutral-500 max-w-md mx-auto mt-1 mb-4 leading-relaxed">
              No hay maquinaria o herramientas que coincidan con los filtros seleccionados. Intenta ampliar el rango de precio o limpiar la búsqueda.
            </p>
            <button
              onClick={handleResetFilters}
              className="inline-flex items-center gap-2 px-4 py-2 bg-[#17191C] hover:bg-neutral-800 text-white text-xs font-bold rounded-lg transition-colors cursor-pointer shadow-xs"
            >
              <RotateCcw className="w-3.5 h-3.5 text-[#F5A623]" />
              <span>Restablecer filtros</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredTools.map((tool) => {
              const stockDisp = tool.cantidadDisponible !== undefined ? tool.cantidadDisponible : (tool.cantidadTotal || 1);
              const isAvailable = tool.estadoOperativo === 'DISPONIBLE' && stockDisp > 0;
              const subtotalEstimado = tool.precioPorDia * currentDays;
              const ivaEstimado = Math.round(subtotalEstimado * IVA_RATE);
              const totalEstimado = subtotalEstimado + ivaEstimado;

              return (
                <div
                  key={tool.id}
                  className="bg-white rounded-xl border border-neutral-200 hover:border-neutral-300 overflow-hidden flex flex-col justify-between transition-all duration-200 hover:shadow-sm group"
                >
                  <div>
                    {/* Fotografía de la Maquinaria */}
                    <div className="relative h-48 bg-neutral-100 overflow-hidden">
                      <img
                        src={resolveToolImage(tool)}
                        alt={tool.nombre}
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-103"
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = defaultExcavator;
                        }}
                      />

                      {/* Badge de disponibilidad de stock real */}
                      <div className="absolute top-2.5 right-2.5 bg-[#17191C]/85 backdrop-blur-xs text-white text-[11px] font-semibold px-2.5 py-1 rounded-md flex items-center gap-1.5 shadow-xs">
                        {isAvailable ? (
                          <>
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                            <span>Disponible · {stockDisp} en stock</span>
                          </>
                        ) : (
                          <>
                            <AlertCircle className="w-3.5 h-3.5 text-rose-400" />
                            <span>Sin stock temporal</span>
                          </>
                        )}
                      </div>
                    </div>

                    {/* Contenido de la Tarjeta */}
                    <div className="p-4">
                      {/* Metadatos técnicos (marca, modelo, stock) */}
                      <div className="flex items-center gap-1.5 text-[11px] font-medium text-neutral-500 mb-1.5 truncate">
                        <span className="font-semibold text-neutral-700">{tool.marca || 'Estándar'}</span>
                        <span aria-hidden="true">·</span>
                        <span>Mod. {tool.modelo || 'N/A'}</span>
                        <span aria-hidden="true">·</span>
                        <span className="text-neutral-500">
                          Total: {tool.cantidadTotal || 1}
                        </span>
                      </div>

                      {/* Título del Equipo */}
                      <h3
                        onClick={() => onSelectTool(tool)}
                        className="text-sm font-bold text-[#17191C] line-clamp-2 hover:text-[#F5A623] transition-colors cursor-pointer mb-1.5"
                        title={tool.nombre}
                      >
                        {tool.nombre}
                      </h3>

                      {/* Descripción corta */}
                      <p className="text-xs text-neutral-600 line-clamp-2 mb-2 leading-relaxed">
                        {tool.descripcion || 'Equipo de alto rendimiento para obras de construcción y remodelación.'}
                      </p>
                    </div>
                  </div>

                  {/* Pie de Tarjeta: Precios e IVA transparente + Acciones */}
                  <div className="p-4 pt-0 border-t border-neutral-100 mt-2">
                    <div className="pt-3 mb-3">
                      <div className="flex items-baseline justify-between">
                        <div>
                          <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-semibold">
                            Tarifa diaria
                          </span>
                          <span className="text-lg font-extrabold text-[#17191C] font-mono tabular-nums">
                            ${tool.precioPorDia.toLocaleString('es-CO')}
                          </span>
                          <span className="text-[11px] text-neutral-500 font-medium ml-1">COP</span>
                        </div>
                        <span className="text-[10px] font-bold text-neutral-500 bg-neutral-100 px-1.5 py-0.5 rounded">
                          + IVA del 19 %
                        </span>
                      </div>

                      {/* Desglose si el estimador rápido tiene más de 1 día */}
                      {currentDays > 1 && (
                        <div className="mt-2 pt-2 border-t border-dashed border-neutral-200 text-[11px] text-neutral-600 flex justify-between items-center">
                          <span>Estimado ({currentDays} días + IVA):</span>
                          <span className="font-mono font-bold text-[#17191C] tabular-nums">
                            ${totalEstimado.toLocaleString('es-CO')} COP
                          </span>
                        </div>
                      )}
                    </div>

                    {/* Botones de Acción */}
                    <div className="grid grid-cols-2 gap-2">
                      <button
                        onClick={() => onSelectTool(tool)}
                        className="w-full py-2 px-2 text-xs font-semibold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer text-center whitespace-nowrap"
                      >
                        Ver Ficha
                      </button>
                      <button
                        onClick={() => onBookTool(tool)}
                        disabled={!isAvailable}
                        className={`w-full py-2 px-2 text-xs font-bold rounded-lg transition-colors whitespace-nowrap cursor-pointer text-center ${
                          isAvailable
                            ? 'bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] shadow-xs'
                            : 'bg-neutral-200 text-neutral-400 cursor-not-allowed'
                        }`}
                      >
                        {isAvailable ? 'Reservar' : 'Sin Stock'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};
