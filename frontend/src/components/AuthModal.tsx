import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Lock, Mail, User as UserIcon, AlertCircle, Building2, Phone, MapPin, CheckCircle2 } from 'lucide-react';

interface AuthModalProps {
  onClose: () => void;
}

/**
 * Validador oficial del Dígito de Verificación (DV) de NIT según algoritmo módulo 11 de la DIAN.
 * Coincide 100% con la regla del Value Object Nit.java del backend.
 */
export function validarNitDian(nit: string): boolean {
  if (!nit || typeof nit !== 'string') return false;
  const trimmed = nit.trim().toUpperCase();
  if (!/^\d{1,15}-\d$/.test(trimmed)) return false;

  const [numero, dvStr] = trimmed.split('-');
  const dvIngresado = parseInt(dvStr, 10);
  const pesos = [3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71];
  let suma = 0;
  for (let i = 0; i < numero.length; i++) {
    const digito = parseInt(numero.charAt(numero.length - 1 - i), 10);
    suma += digito * pesos[i];
  }
  const residuo = suma % 11;
  const dvCalculado = residuo > 1 ? 11 - residuo : residuo;
  return dvCalculado === dvIngresado;
}

export const AuthModal: React.FC<AuthModalProps> = ({ onClose }) => {
  const { login, register, isDemoEnabled } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [telefono, setTelefono] = useState('');
  const [razonSocial, setRazonSocial] = useState('');
  const [nit, setNit] = useState('');
  const [direccionFiscal, setDireccionFiscal] = useState('');
  const [direccionEntregaDefault, setDireccionEntregaDefault] = useState('');
  const [rol, setRol] = useState<'CLIENTE' | 'PROPIETARIO'>('CLIENTE');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const isNitValid = rol === 'PROPIETARIO' && nit.trim().length > 0 ? validarNitDian(nit) : true;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    if (isRegister) {
      if (!nombre.trim()) {
        setErrorMsg('El nombre es obligatorio');
        return;
      }
      if (!apellido.trim()) {
        setErrorMsg('El apellido es obligatorio');
        return;
      }
      if (rol === 'PROPIETARIO') {
        if (!razonSocial.trim()) {
          setErrorMsg('La razón social es obligatoria para cuentas de propietario');
          return;
        }
        if (!nit.trim()) {
          setErrorMsg('El NIT es obligatorio para cuentas de propietario');
          return;
        }
        if (!validarNitDian(nit)) {
          setErrorMsg('Formato de NIT inválido o dígito de verificación incorrecto según DIAN (formato esperado: NÚMERO-DV, ej: 901500001-1)');
          return;
        }
      }
      if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
        setErrorMsg('Ingrese un correo electrónico válido');
        return;
      }
      if (telefono.trim() && !/^\d{10}$/.test(telefono.trim())) {
        setErrorMsg('El teléfono debe contener exactamente 10 dígitos numéricos (ej. 3052654240)');
        return;
      }
      if (password.length < 6) {
        setErrorMsg('La contraseña debe tener al menos 6 caracteres');
        return;
      }
    } else {
      if (!email.trim()) {
        setErrorMsg('El correo electrónico es obligatorio');
        return;
      }
      if (!password) {
        setErrorMsg('La contraseña es obligatoria');
        return;
      }
    }

    setLoading(true);

    try {
      if (isRegister) {
        await register({
          nombre: nombre.trim(),
          apellido: apellido.trim(),
          email: email.trim().toLowerCase(),
          password,
          rol,
          telefono: telefono.trim(),
          razonSocial: rol === 'PROPIETARIO' ? (razonSocial.trim() || `${nombre.trim()} ${apellido.trim()}`) : undefined,
          nit: rol === 'PROPIETARIO' ? nit.trim() : undefined,
          direccionFiscal: rol === 'PROPIETARIO' ? (direccionFiscal.trim() || 'Dirección fiscal principal') : undefined,
          direccionEntregaDefault: rol === 'CLIENTE' ? (direccionEntregaDefault.trim() || 'Dirección de obra principal') : undefined
        });
      } else {
        await login(email.trim().toLowerCase(), password);
      }
      onClose();
    } catch (err: any) {
      setErrorMsg(err.message || 'Error en la autenticación');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (emailDemo: string) => {
    setEmail(emailDemo);
    setPassword('Password123!');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150 my-8">
        <div className="flex items-center justify-between pb-4 border-b border-neutral-200 mb-5">
          <h2 className="text-base font-bold text-neutral-900">
            {isRegister ? 'Registro de Usuario en la Plataforma' : 'Iniciar Sesión en la Plataforma'}
          </h2>
          <button
            onClick={onClose}
            className="p-1 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer"
            aria-label="Cerrar modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form noValidate onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          {isRegister && (
            <>
              {/* Rol en la Plataforma */}
              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Tipo de Cuenta (Rol) *</label>
                <select
                  value={rol}
                  onChange={(e) => {
                    setRol(e.target.value as any);
                    setErrorMsg('');
                  }}
                  className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg font-medium text-neutral-900 focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                >
                  <option value="CLIENTE">Cliente / Empresa Contratista (Renta de Equipos)</option>
                  <option value="PROPIETARIO">Propietario / Proveedor de Maquinaria</option>
                </select>
                <p className="mt-1 text-[11px] text-neutral-500">
                  {rol === 'CLIENTE'
                    ? 'Permite consultar disponibilidad, solicitar reservas, pagar en línea y descargar facturas.'
                    : 'Permite publicar maquinaria en catálogo, gestionar inventarios y recibir devoluciones.'}
                </p>
              </div>

              {/* Nombre y Apellido separados */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Nombres *</label>
                  <input
                    type="text"
                    required
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    placeholder="Ej. Carlos"
                    className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Apellidos *</label>
                  <input
                    type="text"
                    required
                    value={apellido}
                    onChange={(e) => setApellido(e.target.value)}
                    placeholder="Ej. Gómez Pérez"
                    className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                  />
                </div>
              </div>

              {/* Campos específicos de PROPIETARIO */}
              {rol === 'PROPIETARIO' && (
                <div className="p-3 bg-amber-50/60 rounded-xl border border-amber-200/80 space-y-3">
                  <div className="flex items-center gap-1.5 font-semibold text-amber-900">
                    <Building2 className="w-4 h-4 text-amber-600" />
                    <span>Datos Tributarios del Propietario</span>
                  </div>

                  <div>
                    <label className="block font-semibold text-neutral-700 mb-1">Razón Social o Nombre Comercial *</label>
                    <input
                      type="text"
                      required
                      value={razonSocial}
                      onChange={(e) => setRazonSocial(e.target.value)}
                      placeholder="Ej. Maquinaria y Equipos del Valle S.A.S."
                      className="w-full p-2 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                    />
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="block font-semibold text-neutral-700">NIT con Dígito de Verificación (DIAN) *</label>
                      {nit.trim().length > 0 && (
                        <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${isNitValid ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}`}>
                          {isNitValid ? '✓ DV Válido' : '✗ DV Inválido'}
                        </span>
                      )}
                    </div>
                    <input
                      type="text"
                      required
                      value={nit}
                      onChange={(e) => setNit(e.target.value)}
                      placeholder="Ej. 901500001-1"
                      className="w-full p-2 bg-white border border-neutral-300 rounded-lg font-mono focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                    />
                    <p className="mt-1 text-[10px] text-neutral-500">
                      Formato obligatorio NÚMERO-DV con verificación oficial módulo 11 (Ejemplo de prueba: <span className="font-mono font-semibold">901500001-1</span>).
                    </p>
                  </div>

                  <div>
                    <label className="block font-semibold text-neutral-700 mb-1">Dirección Fiscal Principal</label>
                    <input
                      type="text"
                      value={direccionFiscal}
                      onChange={(e) => setDireccionFiscal(e.target.value)}
                      placeholder="Ej. Calle 26 # 68-35, Bogotá"
                      className="w-full p-2 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                    />
                  </div>
                </div>
              )}

              {/* Campos específicos de CLIENTE */}
              {rol === 'CLIENTE' && (
                <div>
                  <label className="block font-semibold text-neutral-700 mb-1">Dirección de Entrega / Obra Principal</label>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={direccionEntregaDefault}
                      onChange={(e) => setDireccionEntregaDefault(e.target.value)}
                      placeholder="Ej. Carrera 7 # 100-20, Obra Torre Norte"
                      className="w-full pl-9 pr-3 py-2 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                    />
                  </div>
                </div>
              )}

              {/* Teléfono */}
              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Teléfono de Contacto</label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="tel"
                    inputMode="numeric"
                    maxLength={10}
                    value={telefono}
                    onChange={(e) => setTelefono(e.target.value.replace(/\D/g, "").slice(0, 10))}
                    placeholder="Ej. 3052654240"
                    className="w-full pl-9 pr-3 py-2 bg-white border border-neutral-300 rounded-lg font-mono focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                  />
                </div>
              </div>
            </>
          )}

          {/* Correo Electrónico */}
          <div>
            <label className="block font-semibold text-neutral-700 mb-1">Correo Electrónico *</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="correo@ejemplo.com"
                className="w-full pl-9 pr-3 py-2.5 bg-white border border-neutral-300 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          {/* Contraseña */}
          <div>
            <label className="block font-semibold text-neutral-700 mb-1">Contraseña *</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Mínimo 6 caracteres"
                className="w-full pl-9 pr-3 py-2.5 bg-white border border-neutral-300 rounded-lg font-mono focus:outline-hidden focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors shadow-xs mt-2 cursor-pointer disabled:opacity-50"
          >
            {loading ? 'Procesando...' : isRegister ? 'Registrar Cuenta' : 'Iniciar Sesión'}
          </button>
        </form>

        {/* Cuentas Demo de Prueba para Evaluación (Sin ADMIN) */}
        {!isRegister && isDemoEnabled && (
          <div className="mt-5 pt-4 border-t border-neutral-200">
            <span className="text-[11px] font-semibold text-neutral-500 uppercase tracking-wider block mb-2 text-center">
              Cuentas Demo de Prueba (Dev)
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('compras@colpatriaconstrucciones.com')}
                className="p-1.5 text-[11px] font-medium bg-neutral-100 hover:bg-neutral-200 text-neutral-800 rounded-lg text-center cursor-pointer"
              >
                Cliente Demo
              </button>
              <button
                type="button"
                onClick={() => handleQuickLogin('contacto@maquinariavalle.com')}
                className="p-1.5 text-[11px] font-medium bg-neutral-100 hover:bg-neutral-200 text-neutral-800 rounded-lg text-center cursor-pointer"
              >
                Propietario Demo
              </button>
            </div>
          </div>
        )}

        <div className="mt-4 text-center">
          <button
            type="button"
            onClick={() => { setIsRegister(!isRegister); setErrorMsg(''); }}
            className="text-xs font-semibold text-amber-600 hover:text-amber-700 underline cursor-pointer"
          >
            {isRegister ? '¿Ya tiene cuenta? Iniciar Sesión' : '¿No tiene cuenta? Regístrese aquí'}
          </button>
        </div>
      </div>
    </div>
  );
};
