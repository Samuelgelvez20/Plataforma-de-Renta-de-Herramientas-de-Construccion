import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BASE_URL = process.env.BASE_URL || 'http://localhost:5174';
const API_URL = process.env.API_URL || 'http://localhost:8080/api/v1';
const EVIDENCIAS_DIR = path.resolve(__dirname, 'evidencias');

if (!fs.existsSync(EVIDENCIAS_DIR)) {
  fs.mkdirSync(EVIDENCIAS_DIR, { recursive: true });
}

const timestamp = Date.now();
const testClientEmail = `cliente.e2e.${timestamp}@obrixen.test`;
const testClientPass = 'Password123!';
const testClientNombre = 'Cliente';
const testClientApellido = `E2E ${timestamp.toString().slice(-4)}`;
const testClientTelefono = '3109876543';
const testClientDireccion = 'Carrera 15 # 93-60 Torre B Obra 1';

const ownerEmail = 'propietario1@obrixen.com.co';
const ownerPass = 'Propietario1Demo2026*!';

const adminEmail = 'admin@obrixen.com.co';
const adminPass = 'AdminDemo2026*!';

const toolNameTarget = 'Vibrador de Concreto Eléctrico 2 HP';
const damageDesc = 'Prueba E2E: carcasa fisurada y mandril dañado';
const repairCostVal = 85000;

const log = (phase, msg) => {
  const time = new Date().toISOString().substring(11, 19);
  console.log(`[${time}] [${phase}] ${msg}`);
};

async function runE2ETest() {
  console.log('='.repeat(80));
  console.log('OBRIXEN — PRUEBA E2E AUTOMATIZADA DE ALQUILER Y REPARACIÓN');
  console.log(`Frontend URL: ${BASE_URL}`);
  console.log(`Backend API:  ${API_URL}`);
  console.log(`Directorio Evidencias: ${EVIDENCIAS_DIR}`);
  console.log('='.repeat(80));

  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,900']
  });

  const context = await browser.newContext({
    viewport: { width: 1400, height: 900 },
    acceptDownloads: true
  });

  const page = await context.newPage();

  // Helper de captura segura
  const capture = async (name) => {
    const filePath = path.join(EVIDENCIAS_DIR, `${name}.png`);
    await page.screenshot({ path: filePath, fullPage: true });
    log('SCREENSHOT', `Captura guardada en: ${filePath}`);
    return filePath;
  };

  const results = {
    faseA: { status: 'PENDING' },
    faseB: { status: 'PENDING' },
    faseC: { status: 'PENDING' },
    faseD: { status: 'PENDING' },
    faseE: { status: 'PENDING' },
    faseF: { status: 'PENDING' },
    faseG: { status: 'PENDING' },
    faseH: { status: 'PENDING' },
    seguridad: { status: 'PENDING' },
    dbIntegridad: { status: 'PENDING' }
  };

  let createdReservationId = null;
  let rentalInvoiceNumber = null;
  let repairInvoiceNumber = null;
  let clientJwtToken = null;

  try {
    // -------------------------------------------------------------------------
    // FASE A — Registrar un cliente nuevo
    // -------------------------------------------------------------------------
    log('FASE A', 'Navegando a la aplicación web...');
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    log('FASE A', 'Abriendo modal de autenticación para registro...');
    const loginNavBtn = page.locator('header button:has-text("Iniciar Sesión")');
    await loginNavBtn.click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');

    log('FASE A', 'Cambiando a formulario de registro...');
    await page.locator('text=¿No tiene cuenta? Regístrese aquí').click();
    await page.waitForSelector('text=Registro de Usuario en la Plataforma');

    await capture('fase_a_1_formulario_registro');

    log('FASE A', `Completando datos para nuevo cliente: ${testClientEmail}`);
    await page.locator('select').first().selectOption('CLIENTE');
    await page.locator('input[placeholder="Ej. Carlos"]').fill(testClientNombre);
    await page.locator('input[placeholder="Ej. Gómez Pérez"]').fill(testClientApellido);
    await page.locator('input[placeholder="Ej. Carrera 7 # 100-20, Obra Torre Norte"]').fill(testClientDireccion);
    await page.locator('input[placeholder="Ej. 3052654240"]').fill(testClientTelefono);
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(testClientEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(testClientPass);

    log('FASE A', 'Enviando formulario de registro...');
    await page.locator('button:has-text("Registrar Cuenta")').click();

    // Esperar cierre del modal y verificación de sesión
    await page.waitForSelector('text=Registro de Usuario en la Plataforma', { state: 'detached', timeout: 10000 });
    await page.waitForTimeout(1000);

    // Verificar en la barra de navegación que el usuario está conectado con rol CLIENTE
    const navText = await page.locator('header').innerText();
    if (!navText.includes('CLIENTE')) {
      throw new Error(`El usuario recién registrado no figura como CLIENTE en la barra superior. Contenido: ${navText}`);
    }
    log('FASE A', `Cliente registrado e identificado exitosamente: ${testClientNombre} (${testClientEmail})`);
    await capture('fase_a_2_cliente_autenticado_navbar');
    results.faseA = { status: 'APROBADO', email: testClientEmail, rol: 'CLIENTE' };

    // -------------------------------------------------------------------------
    // FASE B — Consultar herramientas y crear una reserva
    // -------------------------------------------------------------------------
    log('FASE B', 'Navegando al catálogo de equipos...');
    await page.locator('header nav button:has-text("Catálogo de Equipos")').click();
    await page.waitForSelector(`text=${toolNameTarget}`, { timeout: 10000 });

    log('FASE B', `Herramienta encontrada en catálogo: "${toolNameTarget}"`);
    await capture('fase_b_1_catalogo_equipos');

    // Localizar tarjeta de la herramienta
    const toolCard = page.locator(`article:has-text("${toolNameTarget}")`);
    await toolCard.scrollIntoViewIfNeeded();

    log('FASE B', 'Iniciando solicitud de reserva...');
    await toolCard.locator('button:has-text("Reservar")').click();

    await page.waitForSelector('text=Solicitar Reserva de Equipo', { timeout: 8000 });

    // Configurar fechas permitidas (inicio mañana, fin en 3 días)
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const endRental = new Date(today);
    endRental.setDate(endRental.getDate() + 3);

    const fInicioStr = tomorrow.toISOString().split('T')[0];
    const fFinStr = endRental.toISOString().split('T')[0];

    log('FASE B', `Seleccionando periodo: ${fInicioStr} al ${fFinStr}...`);
    const dateInputs = page.locator('form input[type="date"]');
    await dateInputs.nth(0).fill(fInicioStr);
    await dateInputs.nth(1).fill(fFinStr);

    // Agregar notas de obra
    const notasTextarea = page.locator('form textarea');
    if (await notasTextarea.count() > 0) {
      await notasTextarea.fill('Reserva automatizada E2E - Obra Puente Calle 100');
    }

    await capture('fase_b_2_formulario_reserva');

    log('FASE B', 'Confirmando solicitud de reserva...');
    await page.locator('button:has-text("Confirmar Solicitud de Reserva")').click();

    // Esperar redirección al dashboard del cliente y listado de reservas
    await page.waitForSelector('text=Mis Reservas', { timeout: 12000 });
    await page.waitForTimeout(1500);

    // Localizar la fila creada en la tabla de reservas
    const reservationRow = page.locator(`tr:has-text("${toolNameTarget}")`).first();
    await reservationRow.waitFor({ state: 'visible', timeout: 10000 });

    const rowText = await reservationRow.innerText();
    const idMatch = rowText.match(/#([0-9a-fA-F-]+)/);
    createdReservationId = idMatch ? idMatch[1] : null;

    if (!createdReservationId) {
      throw new Error(`No se pudo extraer el ID de la reserva creada en la fila: ${rowText}`);
    }

    log('FASE B', `Reserva creada exitosamente con ID: ${createdReservationId}`);
    if (!rowText.toUpperCase().includes('PENDIENTE')) {
      throw new Error(`La reserva recién creada no está en estado PENDIENTE. Texto fila: ${rowText}`);
    }

    await capture('fase_b_3_reserva_pendiente_cliente');
    results.faseB = { status: 'APROBADO', reservaId: createdReservationId, estado: 'PENDIENTE' };

    // -------------------------------------------------------------------------
    // FASE C — Validación por el propietario
    // -------------------------------------------------------------------------
    log('FASE C', 'Cerrando sesión del cliente...');
    await page.locator('header button[title="Cerrar sesión"]').click();
    await page.waitForTimeout(500);

    log('FASE C', `Iniciando sesión como propietario titular: ${ownerEmail}`);
    await page.locator('header button:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(ownerEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(ownerPass);
    await page.locator('form button[type="submit"]:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma', { state: 'detached', timeout: 8000 });

    log('FASE C', 'Navegando a Gestión Propietario...');
    await page.locator('header nav button:has-text("Gestión Propietario")').click();
    await page.waitForSelector('text=Flota y Maquinaria', { timeout: 10000 });

    log('FASE C', 'Cambiando a pestaña Solicitudes y Reservas...');
    await page.locator('button:has-text("Solicitudes y Reservas")').click();
    await page.waitForTimeout(1000);

    // Localizar la solicitud pendiente de nuestro cliente por su ID único
    const card = page.locator(`span:has-text("${createdReservationId}")`).locator('xpath=ancestor::div[contains(@class, "p-4")][1]');
    await card.waitFor({ state: 'visible', timeout: 10000 });

    await capture('fase_c_1_solicitud_pendiente_propietario');

    log('FASE C', 'Aceptando reserva como propietario...');
    const acceptBtn = card.locator('button:has-text("Aceptar Reserva")');
    await acceptBtn.click();
    await page.waitForTimeout(2000);

    // Verificar en el historial que la reserva pasó a CONFIRMADA
    const confirmedRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await confirmedRow.waitFor({ state: 'visible', timeout: 8000 });
    const confirmedText = await confirmedRow.innerText();
    log('FASE C', `Estado actualizado en vista de propietario: ${confirmedText.replace(/\n/g, ' ')}`);

    if (!confirmedText.toUpperCase().includes('CONFIRMADA')) {
      throw new Error(`La reserva no figura como CONFIRMADA tras ser aceptada por el propietario.`);
    }

    await capture('fase_c_2_reserva_confirmada_propietario');
    results.faseC = { status: 'APROBADO', reservaId: createdReservationId, estado: 'CONFIRMADA' };

    // -------------------------------------------------------------------------
    // FASE D — Pagar el alquiler
    // -------------------------------------------------------------------------
    log('FASE D', 'Cerrando sesión de propietario...');
    await page.locator('header button[title="Cerrar sesión"]').click();
    await page.waitForTimeout(500);

    log('FASE D', `Iniciando sesión como cliente titular: ${testClientEmail}`);
    await page.locator('header button:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(testClientEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(testClientPass);
    await page.locator('form button[type="submit"]:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma', { state: 'detached', timeout: 8000 });

    log('FASE D', 'Abriendo Mis Alquileres & Facturas...');
    await page.locator('header nav button:has-text("Mis Alquileres & Facturas")').click();
    await page.waitForSelector('text=Mis Reservas', { timeout: 10000 });
    await page.waitForTimeout(1000);

    const clientResRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await clientResRow.waitFor({ state: 'visible', timeout: 10000 });

    log('FASE D', 'Localizando botón Pagar en la reserva confirmada...');
    const payBtn = clientResRow.locator('button:has-text("Pagar")');
    await payBtn.click();

    await page.waitForSelector('text=Confirmar y Pagar Alquiler', { timeout: 8000 });
    await capture('fase_d_1_modal_pago_alquiler');

    log('FASE D', 'Procesando pago de alquiler en pasarela simulada...');
    const submitPayBtn = page.locator('form button[type="submit"]:has-text("Pagar")');
    await submitPayBtn.click();

    await page.waitForSelector('text=¡Transacción Aprobada!', { timeout: 15000 });
    await capture('fase_d_2_pago_alquiler_aprobado');

    // Extraer número de factura emitida
    const factMatch = await page.locator('text=/F-[0-9]+/').innerText().catch(() => '');
    rentalInvoiceNumber = factMatch || 'F-GENERADA';
    log('FASE D', `Factura original de alquiler emitida: ${rentalInvoiceNumber}`);

    // Cerrar modal de pago exitoso (botón Cerrar)
    await page.locator('button:has-text("Cerrar")').click();
    await page.waitForTimeout(1000);

    // Verificar en pestaña Facturas y Pagos
    log('FASE D', 'Verificando factura en la pestaña "Facturas y Pagos"...');
    await page.locator('button:has-text("Facturas y Pagos")').click();
    await page.waitForTimeout(1000);

    const invRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await invRow.waitFor({ state: 'visible', timeout: 10000 });
    const invRowText = await invRow.innerText();
    log('FASE D', `Factura registrada en panel cliente: ${invRowText.replace(/\n/g, ' ')}`);

    if (!invRowText.toUpperCase().includes('PAGADA')) {
      throw new Error(`La factura de alquiler no figura como PAGADA.`);
    }

    const factNumMatch = invRowText.match(/F-[0-9]+/);
    if (factNumMatch) {
      rentalInvoiceNumber = factNumMatch[0];
      log('FASE D', `Número exacto de factura de alquiler confirmado: ${rentalInvoiceNumber}`);
    }

    await capture('fase_d_3_factura_alquiler_pagada');
    results.faseD = { status: 'APROBADO', facturaAlquiler: rentalInvoiceNumber, estado: 'PAGADA' };

    // -------------------------------------------------------------------------
    // FASE E — Continuar hasta la devolución (Despacho / Entrega)
    // -------------------------------------------------------------------------
    log('FASE E', 'Cerrando sesión de cliente...');
    await page.locator('header button[title="Cerrar sesión"]').click();
    await page.waitForTimeout(500);

    log('FASE E', `Iniciando sesión como propietario: ${ownerEmail}`);
    await page.locator('header button:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(ownerEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(ownerPass);
    await page.locator('form button[type="submit"]:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma', { state: 'detached', timeout: 8000 });

    log('FASE E', 'Navegando a Gestión Propietario -> Despachos y Devoluciones...');
    await page.locator('header nav button:has-text("Gestión Propietario")').click();
    await page.locator('button:has-text("Despachos y Devoluciones")').click();
    await page.waitForTimeout(1000);

    const ownerPaidRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await ownerPaidRow.waitFor({ state: 'visible', timeout: 10000 });

    log('FASE E', 'Registrando entrega/despacho en obra del equipo...');
    const dispatchBtn = ownerPaidRow.locator('button:has-text("Registrar Entrega")');
    await dispatchBtn.click();

    await page.waitForSelector('text=Confirmar Despacho y Entrega', { timeout: 8000 });
    await capture('fase_e_1_modal_despacho_entrega');

    await page.locator('button:has-text("Confirmar Entrega")').click();
    await page.waitForTimeout(2000);

    // Verificar que el estado ahora sea EN OBRA o EN_CURSO y aparezca el botón Confirmar Devolución
    const inProgressRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await inProgressRow.waitFor({ state: 'visible', timeout: 8000 });
    const inProgressText = await inProgressRow.innerText();
    log('FASE E', `Estado tras despacho: ${inProgressText.replace(/\n/g, ' ')}`);

    const returnBtn = inProgressRow.locator('button:has-text("Confirmar Devolución")');
    await returnBtn.waitFor({ state: 'visible', timeout: 8000 });

    await capture('fase_e_2_equipo_entregado_listo_devolucion');
    results.faseE = { status: 'APROBADO', reservaId: createdReservationId, estado: 'EN_CURSO' };

    // -------------------------------------------------------------------------
    // FASE F — Registrar la devolución con daños
    // -------------------------------------------------------------------------
    log('FASE F', 'Abriendo formulario de Acta de Inspección y Retorno...');
    await returnBtn.click();
    await page.waitForSelector('text=Acta de Inspección Técnica y Retorno', { timeout: 8000 });

    log('FASE F', 'Marcando casilla de equipo con daños/averías...');
    const damageCheckbox = page.locator('input[type="checkbox"]').first();
    await damageCheckbox.check();
    await page.waitForTimeout(300);

    log('FASE F', `Ingresando descripción de averías: "${damageDesc}"`);
    await page.locator('textarea[placeholder*="Rotura en eje"]').fill(damageDesc);

    log('FASE F', `Ingresando costo de reparación: ${repairCostVal} COP`);
    const repairCostInput = page.locator('input[type="number"]');
    await repairCostInput.fill(repairCostVal.toString());

    await page.locator('textarea[placeholder*="Detalles sobre el estado"]').fill('Inspección técnica visual en taller receptor');

    await capture('fase_f_1_formulario_devolucion_danos');

    log('FASE F', 'Finalizando inspección técnica de devolución con daños...');
    await page.locator('button:has-text("Finalizar Inspección")').click();
    await page.waitForSelector('text=Acta de Inspección Técnica y Retorno', { state: 'detached', timeout: 10000 });
    await page.waitForTimeout(1000);

    // Cambiar a pestaña Solicitudes y Reservas para ver el historial general de reservas
    log('FASE F', 'Verificando estado FINALIZADA en historial de reservas...');
    await page.locator('button:has-text("Solicitudes y Reservas")').click();
    await page.waitForTimeout(1000);

    const finalizedRow = page.locator(`tr:has-text("${createdReservationId}")`).first();
    await finalizedRow.waitFor({ state: 'visible', timeout: 8000 });
    const finalizedText = await finalizedRow.innerText();
    log('FASE F', `Estado de la reserva tras devolución: ${finalizedText.replace(/\n/g, ' ')}`);

    if (!finalizedText.toUpperCase().includes('FINALIZADA') && !finalizedText.toUpperCase().includes('DEVUELTA')) {
      throw new Error(`La reserva no figura como FINALIZADA/DEVUELTA tras confirmar retorno con daños.`);
    }

    await capture('fase_f_2_reserva_finalizada_propietario');
    results.faseF = {
      status: 'APROBADO',
      reservaId: createdReservationId,
      descripcionDano: damageDesc,
      costoReparacion: repairCostVal
    };

    // -------------------------------------------------------------------------
    // FASE G — Consultar y pagar la reparación como cliente
    // -------------------------------------------------------------------------
    log('FASE G', 'Cerrando sesión de propietario...');
    await page.locator('header button[title="Cerrar sesión"]').click();
    await page.waitForTimeout(500);

    log('FASE G', `Iniciando sesión como cliente titular: ${testClientEmail}`);
    await page.locator('header button:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(testClientEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(testClientPass);
    await page.locator('form button[type="submit"]:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma', { state: 'detached', timeout: 8000 });

    log('FASE G', 'Abriendo Mis Alquileres & Facturas -> Facturas y Pagos...');
    await page.locator('header nav button:has-text("Mis Alquileres & Facturas")').click();
    await page.locator('button:has-text("Facturas y Pagos")').click();
    await page.waitForTimeout(1500);

    // Buscar fila de la factura de reparación
    const repairRow = page.locator('tr:has-text("Costos de reparación")').first();
    await repairRow.waitFor({ state: 'visible', timeout: 10000 });

    const repairRowText = await repairRow.innerText();
    log('FASE G', `Factura de reparación localizada en panel cliente:\n${repairRowText.replace(/\n/g, ' ')}`);

    // Validar visualización de descripción de avería, monto y estado pendiente
    if (!repairRowText.includes(damageDesc)) {
      throw new Error(`La descripción del daño no aparece visible en la factura de reparación.`);
    }
    if (!repairRowText.includes('85.000')) {
      throw new Error(`El importe de 85.000 COP no coincide en la fila de reparación.`);
    }
    if (!repairRowText.toUpperCase().includes('PENDIENTE')) {
      throw new Error(`La factura de reparación recién generada no se encuentra en estado PENDIENTE.`);
    }

    const payRepairBtn = repairRow.locator('button:has-text("Pagar")');
    await payRepairBtn.waitFor({ state: 'visible', timeout: 5000 });

    await capture('fase_g_1_factura_reparacion_pendiente');

    log('FASE G', 'Iniciando pago de la factura de reparación...');
    await payRepairBtn.click();

    await page.waitForSelector('text=Pago de Costos de Reparación', { timeout: 8000 });
    await capture('fase_g_2_modal_pago_reparacion');

    log('FASE G', 'Confirmando pago de reparación en pasarela...');
    await page.locator('form button[type="submit"]:has-text("Pagar")').click();
    await page.waitForTimeout(2000);

    // Localizar la fila de la factura de reparación actualizada en la tabla
    const paidRepairRow = page.locator('tr:has-text("Costos de reparación")').first();
    await paidRepairRow.waitFor({ state: 'visible', timeout: 10000 });
    const paidRepairText = await paidRepairRow.innerText();
    log('FASE G', `Fila de reparación tras pago: ${paidRepairText.replace(/\n/g, ' ')}`);

    if (!paidRepairText.toUpperCase().includes('PAGADA')) {
      throw new Error(`La factura de reparación no figura como PAGADA tras procesar el pago.`);
    }

    const repMatch = paidRepairText.match(/REP-[0-9]+/);
    repairInvoiceNumber = repMatch ? repMatch[0] : 'REP-GENERADA';
    log('FASE G', `Factura de reparación pagada confirmada: ${repairInvoiceNumber}`);

    // Probar descarga de PDF de factura de reparación desde la tabla
    log('FASE G', 'Descargando comprobante PDF de reparación...');
    const pdfBtn = paidRepairRow.locator('button:has-text("Descargar PDF")');
    await pdfBtn.waitFor({ state: 'visible', timeout: 8000 });

    const downloadPromise = page.waitForEvent('download', { timeout: 10000 });
    await pdfBtn.click();
    const download = await downloadPromise;
    const downloadPath = path.join(EVIDENCIAS_DIR, download.suggestedFilename());
    await download.saveAs(downloadPath);

    // Validar bytes del PDF (%PDF)
    const pdfBuffer = fs.readFileSync(downloadPath);
    const isRealPdf = pdfBuffer.slice(0, 4).toString() === '%PDF';
    log('FASE G', `PDF descargado en: ${downloadPath} (Válido %PDF: ${isRealPdf}, Tamaño: ${pdfBuffer.length} bytes)`);

    if (!isRealPdf) {
      throw new Error('El archivo descargado no corresponde a un documento PDF válido.');
    }

    // Comprobar persistencia del estado PAGADA en la tabla tras recargar la página
    log('FASE G', 'Verificando persistencia de estado PAGADA tras recargar la página...');
    await page.reload({ waitUntil: 'networkidle' });
    await page.locator('header nav button:has-text("Mis Alquileres & Facturas")').click();
    await page.locator('button:has-text("Facturas y Pagos")').click();
    await page.waitForTimeout(1000);

    const reloadedRepairRow = page.locator('tr:has-text("Costos de reparación")').first();
    await reloadedRepairRow.waitFor({ state: 'visible', timeout: 8000 });
    const reloadedRepairText = await reloadedRepairRow.innerText();

    if (!reloadedRepairText.toUpperCase().includes('PAGADA')) {
      throw new Error(`La factura de reparación no persiste en estado PAGADA tras recargar la página.`);
    }

    // Verificar que el botón Pagar dejó de estar disponible y ahora está "Descargar PDF"
    const hasPayBtn = await reloadedRepairRow.locator('button:has-text("Pagar")').count();
    const hasPdfBtn = await reloadedRepairRow.locator('button:has-text("Descargar PDF")').count();

    if (hasPayBtn > 0 || hasPdfBtn === 0) {
      throw new Error(`Estado visual inconsistente: Botón Pagar presente (${hasPayBtn}) o Botón PDF ausente (${hasPdfBtn}).`);
    }

    await capture('fase_g_3_factura_reparacion_pagada');
    results.faseG = {
      status: 'APROBADO',
      facturaReparacion: repairInvoiceNumber,
      monto: repairCostVal,
      pdfValido: isRealPdf,
      tamanoPdfBytes: pdfBuffer.length
    };

    // Guardar token JWT del cliente para pruebas complementarias
    clientJwtToken = await page.evaluate(() => localStorage.getItem('jwt_token'));

    // -------------------------------------------------------------------------
    // FASE H — Verificación desde la cuenta administrativa
    // -------------------------------------------------------------------------
    log('FASE H', 'Cerrando sesión de cliente...');
    await page.locator('header button[title="Cerrar sesión"]').click();
    await page.waitForTimeout(500);

    log('FASE H', `Iniciando sesión como Administrador: ${adminEmail}`);
    await page.locator('header button:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma');
    await page.locator('input[placeholder="correo@ejemplo.com"]').fill(adminEmail);
    await page.locator('input[placeholder="Mínimo 6 caracteres"]').fill(adminPass);
    await page.locator('form button[type="submit"]:has-text("Iniciar Sesión")').click();
    await page.waitForSelector('text=Iniciar Sesión en la Plataforma', { state: 'detached', timeout: 8000 });

    log('FASE H', 'Navegando a Panel Administrador...');
    await page.locator('header nav button:has-text("Panel Administrador")').click();
    await page.waitForSelector('#admin-tabs', { timeout: 10000 });

    await capture('fase_h_1_panel_administrador');

    // 1. Verificación de pestañas de administración y KPIs
    const adminPanelText = await page.locator('main').innerText();
    const hasIngresos = adminPanelText.includes('Ingresos Totales');
    const hasReservasActivas = adminPanelText.includes('Reservas Activas');
    const hasUserInTable = adminPanelText.includes(testClientEmail);
    log('FASE H', `  - Métricas globales visibles: ${hasIngresos && hasReservasActivas ? 'SÍ' : 'NO'}`);
    log('FASE H', `  - Nuevo cliente en tabla de usuarios: ${hasUserInTable ? 'SÍ' : 'NO'}`);

    // 2. Navegar a pestaña "Historial de Reservas y Devoluciones"
    log('FASE H', 'Navegando a Pestaña: Historial de Reservas y Devoluciones...');
    await page.locator('#tab-btn-reservas').click();
    await page.waitForSelector('#tabla-reservas-admin', { timeout: 8000 });
    await page.waitForTimeout(500);

    await page.locator('#tabla-reservas-admin').scrollIntoViewIfNeeded();
    await capture('fase_h_2_reservas_danos_admin');

    const reservasTableText = await page.locator('#tabla-reservas-admin').innerText();
    const reservaVisible = reservasTableText.includes(createdReservationId) || reservasTableText.includes(targetToolName);
    const danosVisibles = reservasTableText.includes('Devuelto con Daños') && reservasTableText.includes(damageDesc);
    const facturaRepAsociada = reservasTableText.includes(repairInvoiceNumber);

    log('FASE H', `  - Reserva visible en auditoría admin: ${reservaVisible ? 'SÍ' : 'NO'}`);
    log('FASE H', `  - Indicador y detalle de daños visible: ${danosVisibles ? 'SÍ' : 'NO'}`);
    log('FASE H', `  - Factura de reparación asociada visible (${repairInvoiceNumber}): ${facturaRepAsociada ? 'SÍ' : 'NO'}`);

    // 3. Navegar a pestaña "Facturas y Cobros"
    log('FASE H', 'Navegando a Pestaña: Facturas y Cobros...');
    await page.locator('#tab-btn-facturas').click();
    await page.waitForSelector('#tabla-facturas-admin', { timeout: 8000 });
    await page.waitForTimeout(500);
    await capture('fase_h_3_facturas_admin');

    const facturasTableText = await page.locator('#tabla-facturas-admin').innerText();
    const facturaAlquilerVisible = facturasTableText.includes(rentalInvoiceNumber) && facturasTableText.includes('ALQUILER');
    const facturaReparacionVisible = facturasTableText.includes(repairInvoiceNumber) && facturasTableText.includes('REPARACIÓN');

    log('FASE H', `  - Factura de alquiler visible (${rentalInvoiceNumber}): ${facturaAlquilerVisible ? 'SÍ' : 'NO'}`);
    log('FASE H', `  - Factura de reparación visible (${repairInvoiceNumber}): ${facturaReparacionVisible ? 'SÍ' : 'NO'}`);

    // 4. Navegar a pestaña "Transacciones y Pagos"
    log('FASE H', 'Navegando a Pestaña: Transacciones y Pagos...');
    await page.locator('#tab-btn-pagos').click();
    await page.waitForSelector('#tabla-pagos-admin', { timeout: 8000 });
    await page.waitForTimeout(500);
    await capture('fase_h_4_pagos_admin');

    const pagosTableText = await page.locator('#tabla-pagos-admin').innerText();
    const pagoAlquilerVisible = pagosTableText.includes('PAGO ALQUILER') && (pagosTableText.includes('160.650') || pagosTableText.includes(rentalInvoiceNumber || ''));
    const pagoReparacionVisible = pagosTableText.includes('PAGO REPARACIÓN') && (pagosTableText.includes('85.000') || pagosTableText.includes(repairInvoiceNumber || ''));

    log('FASE H', `  - Pago de alquiler individual visible: ${pagoAlquilerVisible ? 'SÍ' : 'NO'}`);
    log('FASE H', `  - Pago de reparación individual visible: ${pagoReparacionVisible ? 'SÍ' : 'NO'}`);

    const faseHOk = reservaVisible && danosVisibles && facturaRepAsociada && facturaAlquilerVisible && facturaReparacionVisible && pagoAlquilerVisible && pagoReparacionVisible;

    results.faseH = {
      status: faseHOk ? 'APROBADO' : 'PARCIALMENTE_APROBADO_CON_HALLAZGOS',
      metricasGlobalesVisibles: hasIngresos && hasReservasActivas,
      clienteVisibleEnUsuarios: hasUserInTable,
      reservaEnAuditoria: reservaVisible,
      danosYReparacionVisible: danosVisibles,
      facturaAlquilerVisible,
      facturaReparacionVisible,
      pagoAlquilerVisible,
      pagoReparacionVisible
    };

    // -------------------------------------------------------------------------
    // VERIFICACIONES NEGATIVAS Y DE SEGURIDAD
    // -------------------------------------------------------------------------
    log('SEGURIDAD', 'Ejecutando verificaciones de seguridad y control de acceso...');

    // 1. Cliente intentando acceder a endpoints de administración -> 403 Forbidden
    const adminCheckRes = await fetch(`${API_URL}/admin/usuarios`, {
      headers: { 'Authorization': `Bearer ${clientJwtToken}` }
    });
    const adminReservasRes = await fetch(`${API_URL}/admin/reservas`, {
      headers: { 'Authorization': `Bearer ${clientJwtToken}` }
    });
    const adminFacturasRes = await fetch(`${API_URL}/admin/facturas`, {
      headers: { 'Authorization': `Bearer ${clientJwtToken}` }
    });
    const adminPagosRes = await fetch(`${API_URL}/admin/pagos`, {
      headers: { 'Authorization': `Bearer ${clientJwtToken}` }
    });
    log('SEGURIDAD', `1. Cliente -> GET /api/v1/admin/usuarios: HTTP ${adminCheckRes.status} (Esperado: 403)`);
    log('SEGURIDAD', `   Cliente -> GET /api/v1/admin/reservas: HTTP ${adminReservasRes.status} (Esperado: 403)`);
    log('SEGURIDAD', `   Cliente -> GET /api/v1/admin/facturas: HTTP ${adminFacturasRes.status} (Esperado: 403)`);
    log('SEGURIDAD', `   Cliente -> GET /api/v1/admin/pagos: HTTP ${adminPagosRes.status} (Esperado: 403)`);

    // 2. Cliente intentando resolver reembolso -> 403 Forbidden
    const fakeUuid = '00000000-0000-0000-0000-000000000001';
    const reembCheckRes = await fetch(`${API_URL}/reembolsos/${fakeUuid}/resolver`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${clientJwtToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ aprobar: true })
    });
    log('SEGURIDAD', `2. Cliente -> PUT /api/v1/reembolsos/{id}/resolver: HTTP ${reembCheckRes.status} (Esperado: 403)`);

    // 3. Petición no autenticada -> 401 Unauthorized
    const anonRes = await fetch(`${API_URL}/admin/reservas`, {
      headers: { 'Content-Type': 'application/json' }
    });
    log('SEGURIDAD', `3. Anónimo -> GET /api/v1/admin/reservas: HTTP ${anonRes.status} (Esperado: 401)`);

    // 4. Token inválido -> 401 Unauthorized
    const invalidTokenRes = await fetch(`${API_URL}/admin/reservas`, {
      headers: {
        'Authorization': 'Bearer token-falso-invalido-12345',
        'Content-Type': 'application/json'
      }
    });
    log('SEGURIDAD', `4. Token inválido -> GET /api/v1/admin/reservas: HTTP ${invalidTokenRes.status} (Esperado: 401)`);

    // 5. Intento de doble cobro de factura ya pagada (idempotencia) -> 200 idempotente sin duplicar
    const doublePayRes = await fetch(`${API_URL}/pagos/procesar`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${clientJwtToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        reservaId: createdReservationId,
        metodo: 'SIMULADO',
        claveIdempotencia: `REINTENTO-${timestamp}`
      })
    });
    const doublePayJson = await doublePayRes.json();
    log('SEGURIDAD', `5. Reintento de pago sobre reserva pagada: HTTP ${doublePayRes.status} Mensaje: "${doublePayJson.mensaje}"`);

    const segOk = adminCheckRes.status === 403 &&
                  adminReservasRes.status === 403 &&
                  adminFacturasRes.status === 403 &&
                  adminPagosRes.status === 403 &&
                  reembCheckRes.status === 403 &&
                  anonRes.status === 401 &&
                  invalidTokenRes.status === 401;

    results.seguridad = {
      status: segOk ? 'APROBADO' : 'FALLIDO',
      clienteEndpointAdminUsuarios: adminCheckRes.status,
      clienteEndpointAdminReservas: adminReservasRes.status,
      clienteEndpointAdminFacturas: adminFacturasRes.status,
      clienteEndpointAdminPagos: adminPagosRes.status,
      clienteResolverReembolso: reembCheckRes.status,
      anonimoSinToken: anonRes.status,
      tokenInvalido: invalidTokenRes.status,
      doblePagoIdempotente: doublePayJson.mensaje
    };

    // -------------------------------------------------------------------------
    // VERIFICACIÓN DE INTEGRIDAD EN BASE DE DATOS (CONSULTAS INDEPENDIENTES)
    // -------------------------------------------------------------------------
    log('DATABASE', 'Consultando cardinalidad e integridad real en PostgreSQL...');

    // 1. Consulta de reserva
    const resSql = `SELECT id, estado, solicitud_devolucion, estado_devolucion, costo_reparacion, descripcion_dano FROM reserva WHERE id = '${createdReservationId}';`;
    const resOutput = execSync(`docker exec obrixen_postgres psql -U postgres -d plataforma_renta -c "${resSql}"`, { encoding: 'utf-8' });

    // 2. Consulta de facturas asociadas
    const facSql = `SELECT id, numero_factura, tipo, total, estado, pago_id, descripcion_dano, creado_en FROM factura WHERE reserva_id = '${createdReservationId}' ORDER BY creado_en ASC;`;
    const facOutput = execSync(`docker exec obrixen_postgres psql -U postgres -d plataforma_renta -c "${facSql}"`, { encoding: 'utf-8' });

    // 3. Consulta de pagos asociados
    const pagSql = `SELECT id, reserva_id, factura_id, monto, estado, proveedor_pago, transaccion_id, creado_en FROM pago WHERE reserva_id = '${createdReservationId}' ORDER BY creado_en ASC;`;
    const pagOutput = execSync(`docker exec obrixen_postgres psql -U postgres -d plataforma_renta -c "${pagSql}"`, { encoding: 'utf-8' });

    console.log('\n--- 1. RESERVA EN POSTGRESQL ---');
    console.log(resOutput);
    console.log('\n--- 2. FACTURAS ASOCIADAS (INDIVIDUAL) ---');
    console.log(facOutput);
    console.log('\n--- 3. PAGOS ASOCIADOS (INDIVIDUAL) ---');
    console.log(pagOutput);

    // Verificación de conteos y cardinalidad exacta en JSON
    const jsonCountSql = `
      SELECT
        (SELECT count(*) FROM factura WHERE reserva_id = '${createdReservationId}') AS total_facturas,
        (SELECT count(*) FROM pago WHERE reserva_id = '${createdReservationId}') AS total_pagos,
        (SELECT total FROM factura WHERE reserva_id = '${createdReservationId}' AND tipo = 'FACTURA') AS total_alquiler_factura,
        (SELECT monto FROM pago WHERE reserva_id = '${createdReservationId}' AND factura_id IS NULL) AS total_alquiler_pago,
        (SELECT total FROM factura WHERE reserva_id = '${createdReservationId}' AND tipo = 'REPARACION') AS total_reparacion_factura,
        (SELECT monto FROM pago WHERE reserva_id = '${createdReservationId}' AND factura_id IS NOT NULL) AS total_reparacion_pago;
    `;
    const countOutput = execSync(`docker exec obrixen_postgres psql -U postgres -d plataforma_renta -t -A -F "|" -c "${jsonCountSql.replace(/\\n/g, ' ')}"`, { encoding: 'utf-8' }).trim();
    const [totFac, totPag, facAlq, pagAlq, facRep, pagRep] = countOutput.split('|');

    log('DATABASE', `Cardinalidad verificada:`);
    log('DATABASE', `  - Total facturas: ${totFac} (Esperado: 2)`);
    log('DATABASE', `  - Total pagos: ${totPag} (Esperado: 2)`);
    log('DATABASE', `  - Monto factura alquiler (${facAlq}) == Monto pago alquiler (${pagAlq}): ${parseFloat(facAlq) === parseFloat(pagAlq) ? 'SÍ' : 'NO'}`);
    log('DATABASE', `  - Monto factura reparación (${facRep}) == Monto pago reparación (${pagRep}): ${parseFloat(facRep) === parseFloat(pagRep) ? 'SÍ' : 'NO'}`);

    const dbOk = parseInt(totFac) === 2 &&
                 parseInt(totPag) === 2 &&
                 parseFloat(facAlq) === parseFloat(pagAlq) &&
                 parseFloat(facRep) === parseFloat(pagRep);

    results.dbIntegridad = {
      status: dbOk ? 'APROBADO' : 'FALLIDO',
      reservaId: createdReservationId,
      totalFacturas: parseInt(totFac),
      totalPagos: parseInt(totPag),
      facturaAlquilerMonto: parseFloat(facAlq),
      pagoAlquilerMonto: parseFloat(pagAlq),
      facturaReparacionMonto: parseFloat(facRep),
      pagoReparacionMonto: parseFloat(pagRep),
      reservaRaw: resOutput,
      facturasRaw: facOutput,
      pagosRaw: pagOutput
    };

  } catch (err) {
    log('ERROR', `Fallo durante la ejecución: ${err.message}`);
    await capture('error_fase_e2e');
    throw err;
  } finally {
    await browser.close();
  }

  console.log('\n' + '='.repeat(80));
  console.log('RESUMEN DE RESULTADOS DE LA PRUEBA E2E:');
  console.log(JSON.stringify(results, null, 2));
  console.log('='.repeat(80));

  return results;
}

runE2ETest().catch((err) => {
  console.error('La prueba E2E falló:', err);
  process.exit(1);
});
