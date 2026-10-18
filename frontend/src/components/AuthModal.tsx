import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Lock, Mail, User as UserIcon, AlertCircle } from 'lucide-react';

interface AuthModalProps {
  onClose: () => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ onClose }) => {
  const { login, register, isDemoEnabled } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nombre, setNombre] = useState('');
  const [documento, setDocumento] = useState('');
  const [telefono, setTelefono] = useState('');
  const [direccion, setDireccion] = useState('');
  const [rol, setRol] = useState<'CLIENTE' | 'PROPIETARIO'>('CLIENTE');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setLoading(true);

    try {
      if (isRegister) {
        await register({
          nombre,
          email,
          password,
          rol,
          documentoIdentidad: documento,
          telefono,
          direccion
        });
      } else {
        await login(email, password);
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
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-neutral-200 animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-4 border-b border-neutral-200 mb-5">
          <h2 className="text-base font-bold text-neutral-900">
            {isRegister ? 'Registro de Usuario' : 'Iniciar Sesión en la Plataforma'}
          </h2>
          <button
            onClick={onClose}
            className="p-1 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100"
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

        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          {isRegister && (
            <>
              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Nombre Completo / Razón Social *</label>
                <input
                  type="text"
                  required
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-semibold text-neutral-700 mb-1">Rol en la Plataforma *</label>
                <select
                  value={rol}
                  onChange={(e) => setRol(e.target.value as any)}
                  className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg"
                >
                  <option value="CLIENTE">Cliente / Empresa Contratista</option>
                  <option value="PROPIETARIO">Propietario / Proveedor de Equipos</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-neutral-700 mb-1">NIT / Cédula de Ciudadanía *</label>
                <input
                  type="text"
                  required
                  value={documento}
                  onChange={(e) => setDocumento(e.target.value)}
                  className="w-full p-2.5 bg-white border border-neutral-300 rounded-lg font-mono"
                />
              </div>
            </>
          )}

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
                className="w-full pl-9 pr-3 py-2.5 bg-white border border-neutral-300 rounded-lg"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-neutral-700 mb-1">Contraseña *</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 bg-white border border-neutral-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 text-xs font-bold text-slate-950 bg-amber-500 hover:bg-amber-600 rounded-lg transition-colors shadow-xs mt-2"
          >
            {loading ? 'Procesando...' : isRegister ? 'Registrar Cuenta' : 'Iniciar Sesión'}
          </button>
        </form>

        {/* Quick Demo Credentials (Solo visible si VITE_ENABLE_DEMO_SWITCHER=true en desarrollo, sin cuentas admin) */}
        {!isRegister && isDemoEnabled && (
          <div className="mt-5 pt-4 border-t border-neutral-200">
            <span className="text-[11px] font-semibold text-neutral-500 uppercase tracking-wider block mb-2 text-center">
              Cuentas Demo de Prueba (Dev)
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('compras@colpatriaconstrucciones.com')}
                className="p-1.5 text-[11px] font-medium bg-neutral-100 hover:bg-neutral-200 text-neutral-800 rounded-lg text-center"
              >
                Cliente Demo
              </button>
              <button
                type="button"
                onClick={() => handleQuickLogin('contacto@maquinariavalle.com')}
                className="p-1.5 text-[11px] font-medium bg-neutral-100 hover:bg-neutral-200 text-neutral-800 rounded-lg text-center"
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
            className="text-xs font-semibold text-amber-600 hover:text-amber-700 underline"
          >
            {isRegister ? '¿Ya tiene cuenta? Iniciar Sesión' : '¿No tiene cuenta? Regístrese aquí'}
          </button>
        </div>
      </div>
    </div>
  );
};
