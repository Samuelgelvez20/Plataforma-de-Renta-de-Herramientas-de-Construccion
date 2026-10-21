import defaultExcavator from '../assets/images/heavy_excavator_construction_1792091729475.jpg';
import defaultJackhammer from '../assets/images/demolition_jackhammer_tool_1792091731118.jpg';
import defaultScaffolding from '../assets/images/scaffolding_tower_construction_1792091730950.jpg';
import defaultMixer from '../assets/images/concrete_mixer_industrial_1792091730159.jpg';

export function resolveToolImage(tool?: { imagenUrl?: string; nombre?: string; descripcion?: string; categoriaNombre?: string }): string {
  if (tool?.imagenUrl && tool.imagenUrl.trim() !== '') {
    return tool.imagenUrl;
  }
  const name = (tool?.nombre || '').toLowerCase();
  const desc = (tool?.descripcion || '').toLowerCase();
  const cat = (tool?.categoriaNombre || '').toLowerCase();

  // Andamios & Estructuras
  if (name.includes('andamio') || name.includes('torre') || desc.includes('andamio') || desc.includes('estructura') || cat.includes('andamio')) {
    return defaultScaffolding;
  }
  // Concreto & Pavimentación
  if (name.includes('mezcladora') || name.includes('concreto') || desc.includes('concreto') || desc.includes('cemento') || desc.includes('allanadora') || name.includes('trompo') || cat.includes('concreto')) {
    return defaultMixer;
  }
  // Demolición, Perforación, Herramienta Eléctrica y Menor
  if (name.includes('taladro') || name.includes('martillo') || name.includes('rotomartillo') || name.includes('sierra') || name.includes('pulidora') || desc.includes('electrica') || desc.includes('perforac') || cat.includes('eléctrica') || cat.includes('electrica')) {
    return defaultJackhammer;
  }
  // Generación y Bombeo
  if (name.includes('generador') || name.includes('planta') || name.includes('bomba') || name.includes('motobomba') || name.includes('iluminac') || cat.includes('bombeo') || cat.includes('generac')) {
    return defaultJackhammer;
  }
  // Herramienta Menor y Compactación
  if (name.includes('rana') || name.includes('bailarina') || name.includes('compactador') || cat.includes('menor')) {
    return defaultJackhammer;
  }
  // Maquinaria Pesada & Movimiento por defecto
  return defaultExcavator;
}

export { defaultExcavator, defaultJackhammer, defaultScaffolding, defaultMixer };

