import React, { useState, useEffect } from 'react';
import { motion, useReducedMotion, type Variants } from 'motion/react';
import { Category, Tool } from '../types';
import { api } from '../services/api';
import {
  defaultExcavator,
  defaultJackhammer,
  defaultScaffolding,
  defaultMixer,
  resolveToolImage
} from '../utils/imageResolver';
import {
  HardHat,
  ArrowRight,
  Plus,
  ShieldCheck,
  FileText,
  Truck,
  CheckCircle2,
  Layers,
  Building2,
  Clock,
  ArrowUpRight,
  UserCheck,
  CreditCard,
  FileCheck2,
  Wrench,
  Boxes
} from 'lucide-react';

interface LandingViewProps {
  onNavigateCatalog: (categoryId?: string | number) => void;
  onPublishEquipment: () => void;
  onOpenAuth: () => void;
}

interface DefaultCategoryInfo {
  id: string;
  nombre: string;
  descripcion: string;
  imagen: string;
}

const DEFAULT_CATEGORIES: DefaultCategoryInfo[] = [
  {
    id: 'maquinaria-pesada',
    nombre: 'Maquinaria Pesada & Movimiento',
    descripcion: 'Excavadoras, retroexcavadoras y compactadores para preparación de terrenos y obra civil pesada.',
    imagen: defaultExcavator
  },
  {
    id: 'demolicion-perforacion',
    nombre: 'Demolición & Perforación',
    descripcion: 'Martillos demoledores, rotomartillos y sierras de alta potencia para apertura de zanjas y estructuras.',
    imagen: defaultJackhammer
  },
  {
    id: 'concreto-pavimentacion',
    nombre: 'Concreto & Pavimentación',
    descripcion: 'Mezcladoras tipo trompo, vibradores de inmersión y allanadoras para vaciados de alta resistencia.',
    imagen: defaultMixer
  },
  {
    id: 'andamios-estructuras',
    nombre: 'Andamios & Trabajo en Alturas',
    descripcion: 'Cuerpos de andamio tubular, plataformas antideslizantes y torres de soporte certificadas.',
    imagen: defaultScaffolding
  }
];

export const LandingView: React.FC<LandingViewProps> = ({
  onNavigateCatalog,
  onPublishEquipment,
  onOpenAuth
}) => {
  const shouldReduceMotion = useReducedMotion();

  const [categories, setCategories] = useState<Category[]>([]);
  const [tools, setTools] = useState<Tool[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeAudienceTab, setActiveAudienceTab] = useState<'contratistas' | 'propietarios'>('contratistas');

  useEffect(() => {
    let isMounted = true;
    async function loadLandingData() {
      try {
        setLoading(true);
        const [catsData, toolsData] = await Promise.all([
          api.getCategories().catch(() => []),
          api.getTools().catch(() => [])
        ]);
        if (isMounted) {
          setCategories(catsData);
          setTools(toolsData);
        }
      } catch (err) {
        console.warn('Carga parcial de datos de catálogo para landing:', err);
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    loadLandingData();
    return () => {
      isMounted = false;
    };
  }, []);

  // Animaciones configurables según preferencia de movimiento
  const fadeIn: Variants = {
    hidden: { opacity: shouldReduceMotion ? 1 : 0, y: shouldReduceMotion ? 0 : 16 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: shouldReduceMotion ? 0 : 0.45, ease: 'easeOut' }
    }
  };

  const stagger: Variants = {
    hidden: { opacity: 1 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: shouldReduceMotion ? 0 : 0.08
      }
    }
  };

  // Asigna imagen contextual para cada categoría real
  const resolveCategoryImage = (cat: Category, index: number): string => {
    const name = cat.nombre.toLowerCase();
    const desc = cat.descripcion.toLowerCase();

    if (name.includes('andamio') || name.includes('torre') || desc.includes('andamio') || desc.includes('altura')) {
      return defaultScaffolding;
    }
    if (name.includes('concreto') || name.includes('mezcladora') || desc.includes('concreto') || desc.includes('cemento')) {
      return defaultMixer;
    }
    if (name.includes('demol') || name.includes('taladro') || name.includes('martillo') || desc.includes('perforac')) {
      return defaultJackhammer;
    }
    if (name.includes('pesad') || name.includes('excav') || desc.includes('movimiento') || desc.includes('tierra')) {
      return defaultExcavator;
    }
    const defaultImages = [defaultExcavator, defaultJackhammer, defaultMixer, defaultScaffolding];
    return defaultImages[index % defaultImages.length];
  };

  // Muestra categorías cargadas de la API o los defaults del sistema
  const displayCategories = categories.length > 0 ? categories : DEFAULT_CATEGORIES;

  // Conteo de inventario operativo verificable
  const totalEquiposDisponibles = tools.reduce((acc, t) => {
    const stock = t.cantidadDisponible !== undefined ? t.cantidadDisponible : (t.cantidadTotal || 1);
    return acc + (t.estadoOperativo === 'DISPONIBLE' ? stock : 0);
  }, 0);

  return (
    <div className="w-full bg-[#17191C] text-[#F4F5F6] selection:bg-[#F5A623] selection:text-[#17191C]">
      {/* =========================================================================
          1. HERO PRINCIPAL (Editorial Industrial de Alto Rendimiento)
         ========================================================================= */}
      <section className="relative overflow-hidden border-b border-[#292D32]">
        {/* Fondo técnico con trama reticular industrial sutil */}
        <div
          className="absolute inset-0 opacity-[0.035] pointer-events-none"
          style={{
            backgroundImage: `radial-gradient(#F5A623 1px, transparent 1px)`,
            backgroundSize: '24px 24px'
          }}
          aria-hidden="true"
        />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-20 lg:py-24 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">
            {/* Columna Izquierda: Mensaje Central */}
            <motion.div
              variants={stagger}
              initial="hidden"
              animate="visible"
              className="lg:col-span-7 flex flex-col items-start"
            >
              {/* Badge de Plataforma */}
              <motion.div variants={fadeIn} className="mb-5">
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-[#292D32]/90 border border-[#F5A623]/30 text-[#F5A623] text-xs font-bold tracking-wider uppercase">
                  <span className="w-2 h-2 rounded-full bg-[#F5A623] animate-pulse" aria-hidden="true" />
                  <span>Plataforma de Renta de Maquinaria y Equipos</span>
                </div>
              </motion.div>

              {/* Eslogan Oficial */}
              <motion.h1
                variants={fadeIn}
                className="text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#F4F5F6] leading-[1.12] mb-5 font-sans"
              >
                El equipo que tu obra necesita.
              </motion.h1>

              {/* Propuesta de Valor */}
              <motion.p
                variants={fadeIn}
                className="text-base sm:text-lg text-neutral-300 max-w-2xl leading-relaxed mb-8"
              >
                Alquila maquinaria pesada, herramientas técnicas y andamiajes con disponibilidad verificada en tiempo real, contratos respaldados por depósito en garantía y facturación legal con IVA del 19 % desglosado.
              </motion.p>

              {/* Acciones Principales (CTAs) */}
              <motion.div
                variants={fadeIn}
                className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full sm:w-auto mb-10"
              >
                <button
                  type="button"
                  onClick={() => onNavigateCatalog()}
                  className="px-6 py-3.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-extrabold text-sm rounded-lg shadow-sm transition-all duration-200 flex items-center justify-center gap-2.5 cursor-pointer group"
                >
                  <span>Explorar Catálogo</span>
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                </button>

                <button
                  type="button"
                  onClick={onPublishEquipment}
                  className="px-5 py-3.5 bg-[#292D32] hover:bg-[#343a40] text-[#F4F5F6] font-bold text-sm rounded-lg border border-neutral-700 transition-colors duration-200 flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Plus className="w-4 h-4 text-[#F5A623]" />
                  <span>Publicar Equipos</span>
                </button>
              </motion.div>

              {/* Indicadores de Operación Verificables */}
              <motion.div
                variants={fadeIn}
                className="grid grid-cols-3 gap-4 pt-6 border-t border-[#292D32] w-full max-w-lg"
              >
                <div>
                  <span className="text-2xl sm:text-3xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                    {loading ? '—' : tools.length}
                  </span>
                  <span className="text-[11px] sm:text-xs text-neutral-400 font-medium block mt-0.5">
                    Equipos en catálogo
                  </span>
                </div>
                <div>
                  <span className="text-2xl sm:text-3xl font-extrabold text-[#F5A623] font-mono tabular-nums block">
                    {loading ? '—' : totalEquiposDisponibles}
                  </span>
                  <span className="text-[11px] sm:text-xs text-neutral-400 font-medium block mt-0.5">
                    Unidades en stock
                  </span>
                </div>
                <div>
                  <span className="text-2xl sm:text-3xl font-extrabold text-[#F4F5F6] font-mono tabular-nums block">
                    {loading ? '—' : (categories.length || DEFAULT_CATEGORIES.length)}
                  </span>
                  <span className="text-[11px] sm:text-xs text-neutral-400 font-medium block mt-0.5">
                    Categorías activas
                  </span>
                </div>
              </motion.div>
            </motion.div>

            {/* Columna Derecha: Tarjeta Fotográfica de Obra */}
            <motion.div
              initial={{ opacity: shouldReduceMotion ? 1 : 0, scale: shouldReduceMotion ? 1 : 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: shouldReduceMotion ? 0 : 0.5, ease: 'easeOut' }}
              className="lg:col-span-5"
            >
              <div className="relative rounded-2xl overflow-hidden border border-[#292D32] bg-[#292D32]/40 shadow-2xl group">
                <div className="relative h-72 sm:h-96 w-full overflow-hidden bg-neutral-900">
                  <img
                    src={defaultExcavator}
                    alt="Maquinaria pesada para construcción en obra civil"
                    className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-104"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#17191C] via-[#17191C]/35 to-transparent" />
                </div>

                {/* Micro-panel de Garantías Técnicas */}
                <div className="p-5 bg-[#17191C]/95 backdrop-blur-sm border-t border-[#292D32]">
                  <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#F5A623]/15 border border-[#F5A623]/30 flex items-center justify-center shrink-0">
                        <Truck className="w-5 h-5 text-[#F5A623]" />
                      </div>
                      <div>
                        <span className="text-xs font-bold text-[#F4F5F6] block">
                          Logística y Entrega en Obra
                        </span>
                        <span className="text-[11px] text-neutral-400 block">
                          Inspección técnica de recepción y facturación oficial
                        </span>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold font-mono uppercase px-2 py-1 rounded bg-[#292D32] text-[#F5A623] border border-[#292D32]">
                      IVA 19%
                    </span>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          2. VITRINA DE CATEGORÍAS TÉCNICAS (Datos Reales)
         ========================================================================= */}
      <section className="bg-[#1F2226] border-b border-[#292D32] py-14 sm:py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
            <div>
              <span className="text-xs font-bold tracking-wider uppercase text-[#F5A623] block mb-1">
                Especialidades de Maquinaria
              </span>
              <h2 className="text-2xl sm:text-3xl font-extrabold text-[#F4F5F6] tracking-tight">
                Categorías de Equipos en OBRIXEN
              </h2>
            </div>
            <button
              type="button"
              onClick={() => onNavigateCatalog()}
              className="text-xs font-bold text-[#F5A623] hover:text-[#e09419] flex items-center gap-1.5 transition-colors self-start md:self-auto cursor-pointer"
            >
              <span>Ver todas en el catálogo</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {displayCategories.map((cat, idx) => {
              const isDefault = !('activo' in cat);
              const imgSrc = isDefault
                ? (cat as DefaultCategoryInfo).imagen
                : resolveCategoryImage(cat, idx);

              // Conteo de herramientas en la categoría
              const count = isDefault
                ? tools.filter(t => t.categoriaId === cat.id).length
                : tools.filter(t => t.categoriaId === cat.id).length;

              return (
                <div
                  key={String(cat.id)}
                  onClick={() => onNavigateCatalog(cat.id)}
                  className="bg-[#17191C] rounded-xl border border-[#292D32] hover:border-[#F5A623]/50 overflow-hidden flex flex-col justify-between transition-all duration-200 hover:-translate-y-1 group cursor-pointer shadow-sm"
                >
                  <div className="relative h-44 w-full bg-neutral-900 overflow-hidden">
                    <img
                      src={imgSrc}
                      alt={cat.nombre}
                      className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-106"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-[#17191C] via-transparent to-transparent opacity-80" />
                    {count > 0 && (
                      <span className="absolute top-2.5 right-2.5 bg-[#17191C]/85 backdrop-blur-xs text-[11px] font-mono text-[#F5A623] font-bold px-2 py-0.5 rounded border border-[#292D32]">
                        {count} {count === 1 ? 'equipo' : 'equipos'}
                      </span>
                    )}
                  </div>

                  <div className="p-5 flex-1 flex flex-col justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-[#F4F5F6] group-hover:text-[#F5A623] transition-colors mb-2">
                        {cat.nombre}
                      </h3>
                      <p className="text-xs text-neutral-400 line-clamp-3 leading-relaxed">
                        {cat.descripcion || 'Equipos seleccionados para tareas exigentes de construcción y remodelación.'}
                      </p>
                    </div>

                    <div className="pt-4 mt-3 border-t border-[#292D32] flex items-center justify-between text-xs font-semibold text-neutral-400 group-hover:text-[#F5A623] transition-colors">
                      <span>Explorar categoría</span>
                      <ArrowUpRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* =========================================================================
          3. CÓMO FUNCIONA EL PROCESO DE RENTA (3 Pasos Transparentes)
         ========================================================================= */}
      <section className="bg-[#17191C] border-b border-[#292D32] py-14 sm:py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="text-xs font-bold tracking-wider uppercase text-[#F5A623] block mb-1">
              Flujo Transparente
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-[#F4F5F6] tracking-tight">
              Cómo rentar en OBRIXEN
            </h2>
            <p className="text-sm text-neutral-400 mt-2">
              Un proceso formal diseñado para contratistas, ingenieros y maestros de obra.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-8">
            {/* Paso 01 */}
            <div className="p-6 rounded-xl border border-[#292D32] bg-[#1F2226]/70 relative">
              <div className="w-10 h-10 rounded-lg bg-[#292D32] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-5 text-sm border border-neutral-700">
                01
              </div>
              <h3 className="text-base font-bold text-[#F4F5F6] mb-2 flex items-center gap-2">
                <span>Explora y Cotiza</span>
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                Consulta maquinaria pesada, herramientas y andamiajes con especificaciones operativas, stock verificado y tarifas por día en COP.
              </p>
              <div className="flex items-center gap-2 text-[11px] font-mono text-[#F5A623]">
                <Boxes className="w-3.5 h-3.5" />
                <span>Disponibilidad inmediata</span>
              </div>
            </div>

            {/* Paso 02 */}
            <div className="p-6 rounded-xl border border-[#292D32] bg-[#1F2226]/70 relative">
              <div className="w-10 h-10 rounded-lg bg-[#292D32] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-5 text-sm border border-neutral-700">
                02
              </div>
              <h3 className="text-base font-bold text-[#F4F5F6] mb-2 flex items-center gap-2">
                <span>Reserva y Pago Protegido</span>
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                Selecciona las fechas de tu obra. El sistema bloquea el inventario y procesa el pago seguro con clave de idempotencia y depósito en garantía.
              </p>
              <div className="flex items-center gap-2 text-[11px] font-mono text-[#F5A623]">
                <CreditCard className="w-3.5 h-3.5" />
                <span>Idempotencia y seguridad</span>
              </div>
            </div>

            {/* Paso 03 */}
            <div className="p-6 rounded-xl border border-[#292D32] bg-[#1F2226]/70 relative">
              <div className="w-10 h-10 rounded-lg bg-[#292D32] text-[#F5A623] font-mono font-bold flex items-center justify-center mb-5 text-sm border border-neutral-700">
                03
              </div>
              <h3 className="text-base font-bold text-[#F4F5F6] mb-2 flex items-center gap-2">
                <span>Recepción en Obra y Factura</span>
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                El propietario despacha la máquina con acta técnica de entrega. Al completar el servicio, liquidas el depósito y descargas tu factura oficial en PDF.
              </p>
              <div className="flex items-center gap-2 text-[11px] font-mono text-[#F5A623]">
                <FileCheck2 className="w-3.5 h-3.5" />
                <span>Facturación legal con IVA</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          4. BENEFICIOS OBRIXEN: CONTRATISTAS Y PROPIETARIOS
         ========================================================================= */}
      <section className="bg-[#1F2226] border-b border-[#292D32] py-14 sm:py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-10">
            <span className="text-xs font-bold tracking-wider uppercase text-[#F5A623] block mb-1">
              Valor Operativo
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-[#F4F5F6] tracking-tight">
              Diseñado para los actores de la construcción
            </h2>
            <p className="text-sm text-neutral-400 mt-2">
              Soluciones estructuradas para quienes ejecutan obra y para quienes administran flotas de maquinaria.
            </p>

            {/* Selector de audiencia */}
            <div className="inline-flex p-1 bg-[#17191C] rounded-lg border border-[#292D32] mt-6">
              <button
                type="button"
                onClick={() => setActiveAudienceTab('contratistas')}
                className={`px-4 py-2 text-xs font-bold rounded-md transition-colors cursor-pointer ${
                  activeAudienceTab === 'contratistas'
                    ? 'bg-[#F5A623] text-[#17191C] shadow-xs'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Para Contratistas & Obras
              </button>
              <button
                type="button"
                onClick={() => setActiveAudienceTab('propietarios')}
                className={`px-4 py-2 text-xs font-bold rounded-md transition-colors cursor-pointer ${
                  activeAudienceTab === 'propietarios'
                    ? 'bg-[#F5A623] text-[#17191C] shadow-xs'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Para Propietarios de Equipos
              </button>
            </div>
          </div>

          {/* Contenido según pestaña activa */}
          {activeAudienceTab === 'contratistas' ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Stock en Tiempo Real
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Conoce las unidades exactas disponibles antes de comprometer el cronograma de tu cuadrilla en obra.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <FileText className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Facturación Oficial COP
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Tarifas transparentes por día con desglose del 19 % de IVA y comprobante formal descargable en PDF.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Depósito Protegido
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Garantía custodiada formalmente y liquidada contra acta técnica de devolución sin cargos ocultos.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <Clock className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Flexibilidad de Periodo
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Renta desde un día de trabajo hasta contratos mensuales continuos con opción de renovación en línea.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <Building2 className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Gestión Integral de Flota
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Controla equipos publicados, inventario total, unidades alquiladas y fichas técnicas desde un panel dedicado.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <Wrench className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Inspección y Devoluciones
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Registra actas de retorno, evalúa el estado operativo y tramita deducciones justificadas por reparación.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <CreditCard className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Cobros Idempotentes
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Transacciones protegidas contra duplicidad con trazabilidad de pagos y depósitos en custodia.
                </p>
              </div>

              <div className="p-5 rounded-xl border border-[#292D32] bg-[#17191C]">
                <div className="w-9 h-9 rounded-lg bg-[#292D32] flex items-center justify-center text-[#F5A623] mb-3">
                  <FileCheck2 className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-bold text-[#F4F5F6] mb-1.5">
                  Validación DIAN de NIT
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Registro empresarial estricto con comprobación de dígito de verificación para proveedores certificados.
                </p>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* =========================================================================
          5. LLAMADA A LA ACCIÓN FINAL (CTA de Conversión)
         ========================================================================= */}
      <section className="bg-[#17191C] py-16 sm:py-20 relative overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="rounded-2xl border border-[#292D32] bg-gradient-to-r from-[#1F2226] via-[#292D32]/80 to-[#1F2226] p-8 sm:p-12 text-center max-w-4xl mx-auto shadow-2xl">
            <div className="w-12 h-12 rounded-xl bg-[#F5A623] text-[#17191C] flex items-center justify-center mx-auto mb-5 shadow-xs">
              <HardHat className="w-6 h-6" />
            </div>

            <h2 className="text-2xl sm:text-4xl font-extrabold text-[#F4F5F6] tracking-tight mb-3">
              ¿Listo para equipar tu próxima faena?
            </h2>

            <p className="text-sm sm:text-base text-neutral-300 max-w-xl mx-auto mb-8 leading-relaxed">
              Explora la maquinaria disponible de inmediato o crea tu cuenta empresarial para gestionar alquileres y facturación oficial.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => onNavigateCatalog()}
                className="w-full sm:w-auto px-6 py-3.5 bg-[#F5A623] hover:bg-[#e09419] text-[#17191C] font-extrabold text-xs sm:text-sm rounded-lg shadow-sm transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer"
              >
                <span>Explorar Catálogo</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={onOpenAuth}
                className="w-full sm:w-auto px-6 py-3.5 bg-[#17191C] hover:bg-neutral-900 text-[#F4F5F6] font-bold text-xs sm:text-sm rounded-lg border border-neutral-700 transition-colors duration-200 flex items-center justify-center gap-2 cursor-pointer"
              >
                <UserCheck className="w-4 h-4 text-[#F5A623]" />
                <span>Iniciar Sesión / Registro</span>
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
