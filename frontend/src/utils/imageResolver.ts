import defaultExcavator from '../assets/images/heavy_excavator_construction_1792091729475.jpg';
import defaultJackhammer from '../assets/images/demolition_jackhammer_tool_1792091731118.jpg';
import defaultScaffolding from '../assets/images/scaffolding_tower_construction_1792091730950.jpg';
import defaultMixer from '../assets/images/concrete_mixer_industrial_1792091730159.jpg';

export function resolveToolImage(tool?: { imagenUrl?: string; nombre?: string; descripcion?: string }): string {
  if (tool?.imagenUrl && tool.imagenUrl.trim() !== '') {
    return tool.imagenUrl;
  }
  const name = (tool?.nombre || '').toLowerCase();
  const desc = (tool?.descripcion || '').toLowerCase();

  if (name.includes('andamio') || name.includes('torre') || desc.includes('andamio') || desc.includes('estructura')) {
    return defaultScaffolding;
  }
  if (name.includes('mezcladora') || name.includes('concreto') || desc.includes('concreto') || desc.includes('cemento') || desc.includes('allanadora')) {
    return defaultMixer;
  }
  if (name.includes('taladro') || name.includes('martillo') || name.includes('rotomartillo') || name.includes('sierra') || name.includes('pulidora') || desc.includes('electrica') || desc.includes('menor')) {
    return defaultJackhammer;
  }
  return defaultExcavator;
}

export { defaultExcavator, defaultJackhammer, defaultScaffolding, defaultMixer };
