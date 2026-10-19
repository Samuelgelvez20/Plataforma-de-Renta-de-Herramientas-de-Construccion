import React, { useState, useEffect } from 'react';
import { Tool, Category } from '../types';
import { resolveToolImage, defaultExcavator } from '../utils/imageResolver';
import { api } from '../services/api';
import { Search, SlidersHorizontal, Calendar, MapPin, CheckCircle2, AlertCircle } from 'lucide-react';

const IVA_RATE = 0.19;

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
  const [loading, setLoading] = useState(true);

  // Quick live calculator dates
  const [quickStartDate, setQuickStartDate] = useState('');
  const [quickEndDate, setQuickEndDate] = useState('');

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const [catsData, toolsData] = await Promise.all([
          api.getCategories(),
          api.getTools()
        ]);
        setCategories(catsData);
        setTools(toolsData);
      } catch (err) {
        console.error('Error cargando catálogo:', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const filteredTools = tools.filter(tool => {
    if (selectedCategory && tool.categoriaId !== selectedCategory) return false;
    if (onlyAvailable && tool.estadoOperativo !== 'DISPONIBLE') return false;
    if (tool.precioPorDia > maxPrice) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const match =
        tool.nombre.toLowerCase().includes(q) ||
        tool.descripcion.toLowerCase().includes(q) ||
        tool.marca.toLowerCase().includes(q) ||
        tool.ubicacion.toLowerCase().includes(q);
      if (!match) return false;
    }
    return true;
  });

  const calculateDays = () => {
    if (!quickStartDate || !quickEndDate) return 1;
    const s = new Date(quickStartDate);
    const e = new Date(quickEndDate);
    if (e < s) return 1;
    const diff = Math.ceil(Math.abs(e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
    return diff === 0 ? 1 : diff;
  };

  const currentDays = calculateDays();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Hero Header: Natural title, clear proposition */}
      <div className="border-b border-neutral-200 pb-8 mb-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <span className="text-xs font-semibold tracking-wider uppercase text-amber-600 block mb-1">
              Inventario de Equipos Certificados
            </span>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-neutral-900 leading-tight">
              Renta de Maquinaria y Herramientas para Obras
            </h1>
            <p className="mt-2 text-sm text-neutral-600 max-w-2xl">
              Alquiler por días con validación estricta de disponibilidad, seguros por depósito, facturación oficial con IVA 19% en COP y pasarela de pago simulada con protección de idempotencia.
            </p>
          </div>

          {/* Quick Date Estimator Box */}
          <div className="bg-neutral-100 p-3.5 rounded-xl border border-neutral-200 flex flex-wrap items-center gap-3 shrink-0">
            <div className="flex items-center gap-1.5 text-xs font-medium text-neutral-700">
              <Calendar className="w-4 h-4 text-amber-600" />
              <span>Calcular periodo:</span>
            </div>
            <input
              type="date"
              value={quickStartDate}
              onChange={(e) => setQuickStartDate(e.target.value)}
              className="text-xs bg-white border border-neutral-300 rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            <span className="text-xs text-neutral-400">a</span>
            <input
              type="date"
              value={quickEndDate}
              onChange={(e) => setQuickEndDate(e.target.value)}
              className="text-xs bg-white border border-neutral-300 rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            {quickStartDate && quickEndDate && (
              <span className="text-xs font-semibold text-neutral-900 bg-white border border-neutral-300 px-2 py-1 rounded-md font-mono tabular-nums">
                {currentDays} {currentDays === 1 ? 'día' : 'días'}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row gap-4 items-center justify-between mb-8">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por equipo, marca, modelo o ciudad..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 text-sm bg-white border border-neutral-200 rounded-lg focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 shadow-xs"
          />
        </div>

        {/* Price Slider & Availability Toggle */}
        <div className="flex flex-wrap items-center gap-4 w-full md:w-auto">
          <div className="flex items-center gap-2 text-xs font-medium text-neutral-600 bg-white border border-neutral-200 rounded-lg px-3 py-1.5 shadow-xs">
            <SlidersHorizontal className="w-3.5 h-3.5 text-neutral-500" />
            <span>Precio máx/día:</span>
            <span className="font-mono font-bold text-neutral-900 tabular-nums">
              ${(maxPrice / 1000).toLocaleString('es-CO')}k
            </span>
            <input
              type="range"
              min="50000"
              max="2000000"
              step="50000"
              value={maxPrice}
              onChange={(e) => setMaxPrice(Number(e.target.value))}
              className="w-24 accent-amber-500 cursor-pointer"
            />
          </div>

          <label className="flex items-center gap-2 text-xs font-medium text-neutral-700 bg-white border border-neutral-200 rounded-lg px-3 py-2 cursor-pointer shadow-xs">
            <input
              type="checkbox"
              checked={onlyAvailable}
              onChange={(e) => setOnlyAvailable(e.target.checked)}
              className="rounded text-amber-500 focus:ring-amber-400 accent-amber-500"
            />
            <span>Solo disponibles</span>
          </label>
        </div>
      </div>

      {/* Category Segmented Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-3 mb-8 scrollbar-thin">
        <button
          onClick={() => setSelectedCategory(null)}
          className={`px-3.5 py-1.5 text-xs font-medium rounded-lg transition-colors whitespace-nowrap cursor-pointer ${
            selectedCategory === null
              ? 'bg-neutral-900 text-white shadow-xs'
              : 'bg-white text-neutral-600 border border-neutral-200 hover:text-neutral-900 hover:bg-neutral-50'
          }`}
        >
          Todas las Categorías ({tools.length})
        </button>
        {categories.map((cat) => {
          const count = tools.filter(t => t.categoriaId === cat.id).length;
          return (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3.5 py-1.5 text-xs font-medium rounded-lg transition-colors whitespace-nowrap cursor-pointer ${
                selectedCategory === cat.id
                  ? 'bg-neutral-900 text-white shadow-xs'
                  : 'bg-white text-neutral-600 border border-neutral-200 hover:text-neutral-900 hover:bg-neutral-50'
              }`}
            >
              {cat.nombre} ({count})
            </button>
          );
        })}
      </div>

      {/* Grid of Equipment Cards */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
            <div key={n} className="bg-white rounded-xl border border-neutral-200 p-4 animate-pulse">
              <div className="h-44 bg-neutral-200 rounded-lg mb-3"></div>
              <div className="h-4 bg-neutral-200 rounded w-3/4 mb-2"></div>
              <div className="h-3 bg-neutral-100 rounded w-1/2 mb-4"></div>
              <div className="h-6 bg-neutral-200 rounded w-1/3"></div>
            </div>
          ))}
        </div>
      ) : filteredTools.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-neutral-200">
          <p className="text-sm font-medium text-neutral-500 mb-2">No se encontraron equipos con los filtros seleccionados.</p>
          <button
            onClick={() => { setSelectedCategory(null); setSearchQuery(''); setMaxPrice(2000000); setOnlyAvailable(false); }}
            className="text-xs font-semibold text-amber-600 hover:text-amber-700 underline"
          >
            Limpiar filtros
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredTools.map((tool) => {
            const stockDisp = tool.cantidadDisponible !== undefined ? tool.cantidadDisponible : (tool.cantidadTotal || 1);
            const isAvailable = tool.estadoOperativo === 'DISPONIBLE' && stockDisp > 0;
            const calculatedSubtotal = tool.precioPorDia * currentDays;
            const calculatedTotal = calculatedSubtotal * (1 + IVA_RATE);

            return (
              <div
                key={tool.id}
                className="bg-white rounded-xl border border-neutral-200 overflow-hidden flex flex-col justify-between hover:border-neutral-300 transition-all hover:shadow-xs group"
              >
                <div>
                  {/* Equipment Photography */}
                  <div className="relative h-48 bg-neutral-100 overflow-hidden">
                    <img
                      src={resolveToolImage(tool)}
                      alt={tool.nombre}
                      className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-300"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = defaultExcavator;
                      }}
                    />
                    {/* Operational state badge top right */}
                    <div className="absolute top-2.5 right-2.5 bg-neutral-900/80 backdrop-blur-xs text-white text-[11px] font-medium px-2 py-0.5 rounded-md flex items-center gap-1">
                      {isAvailable ? (
                        <>
                          <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                          <span>Disponible ({stockDisp} unid.)</span>
                        </>
                      ) : (
                        <>
                          <AlertCircle className="w-3 h-3 text-rose-400" />
                          <span>Sin Stock</span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Card Content */}
                  <div className="p-4">
                    {/* Unboxed Metadata with Typographic Separator (Zero-Pill Rule) */}
                    <div className="flex items-center gap-1.5 text-[11px] font-medium text-neutral-500 mb-1.5">
                      <span>{tool.marca}</span>
                      <span aria-hidden="true">·</span>
                      <span>Mod. {tool.modelo}</span>
                      <span aria-hidden="true">·</span>
                      <span className={stockDisp > 0 ? "font-semibold text-emerald-700" : "font-semibold text-rose-600"}>
                        Stock: {stockDisp} de {tool.cantidadTotal || 1}
                      </span>
                    </div>

                    {/* Primary Title */}
                    <h3
                      onClick={() => onSelectTool(tool)}
                      className="text-sm font-bold text-neutral-900 line-clamp-2 hover:text-amber-600 transition-colors cursor-pointer mb-2"
                      title={tool.nombre}
                    >
                      {tool.nombre}
                    </h3>

                    {/* Brief description */}
                    <p className="text-xs text-neutral-600 line-clamp-2 mb-3">
                      {tool.descripcion}
                    </p>
                  </div>
                </div>

                {/* Footer Pricing & Actions */}
                <div className="p-4 pt-0 border-t border-neutral-100 mt-2">
                  <div className="flex items-baseline justify-between mb-3 pt-3">
                    <div>
                      <span className="text-[10px] text-neutral-500 uppercase block font-medium">Tarifa por día</span>
                      <span className="text-base font-extrabold text-neutral-900 font-mono tabular-nums">
                        ${tool.precioPorDia.toLocaleString('es-CO')}
                      </span>
                      <span className="text-[11px] text-neutral-500 ml-1">COP</span>
                    </div>

                    {currentDays > 1 && (
                      <div className="text-right">
                        <span className="text-[10px] text-neutral-500 block font-medium">Total ({currentDays}d + IVA)</span>
                        <span className="text-xs font-bold text-neutral-800 font-mono tabular-nums">
                          ${Math.round(calculatedTotal).toLocaleString('es-CO')}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => onSelectTool(tool)}
                      className="w-full py-1.5 px-2 text-xs font-medium text-neutral-700 bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-colors cursor-pointer text-center whitespace-nowrap"
                    >
                      Ver Ficha
                    </button>
                    <button
                      onClick={() => onBookTool(tool)}
                      disabled={!isAvailable}
                      className={`w-full py-1.5 px-2 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap cursor-pointer text-center ${
                        isAvailable
                          ? 'bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold shadow-xs'
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
    </div>
  );
};
