import React, { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

interface ToastContextType {
  toast: {
    success: (message: string, title?: string) => void;
    error: (message: string, title?: string) => void;
    info: (message: string, title?: string) => void;
    warning: (message: string, title?: string) => void;
  };
  showToast: (type: ToastType, message: string, title?: string, duration?: number) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((type: ToastType, message: string, title?: string, duration = 4500) => {
    const id = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const newToast: ToastMessage = { id, type, title, message, duration };
    setToasts((prev) => [...prev, newToast]);

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }
  }, [removeToast]);

  const toast = {
    success: (message: string, title = 'Operación Exitosa') => showToast('success', message, title),
    error: (message: string, title = 'Error') => showToast('error', message, title, 5500),
    info: (message: string, title = 'Información') => showToast('info', message, title),
    warning: (message: string, title = 'Advertencia') => showToast('warning', message, title),
  };

  return (
    <ToastContext.Provider value={{ toast, showToast }}>
      {children}
      {/* Lateral Toast Notification Container (Top Right Lateral) */}
      <div
        aria-live="polite"
        className="fixed top-6 right-6 z-[9999] flex flex-col gap-3 max-w-sm w-full pointer-events-none"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            role="alert"
            className={`pointer-events-auto flex items-start gap-3.5 p-4 rounded-xl shadow-2xl border transition-all duration-300 transform translate-x-0 ${
              t.type === 'success'
                ? 'bg-emerald-50 border-emerald-300 text-emerald-950 shadow-emerald-900/15'
                : t.type === 'error'
                ? 'bg-rose-50 border-rose-300 text-rose-950 shadow-rose-900/15'
                : t.type === 'warning'
                ? 'bg-amber-50 border-amber-300 text-amber-950 shadow-amber-900/15'
                : 'bg-blue-50 border-blue-300 text-blue-950 shadow-blue-900/15'
            }`}
          >
            <div className="shrink-0 mt-0.5">
              {t.type === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-600" />}
              {t.type === 'error' && <AlertCircle className="w-5 h-5 text-rose-600" />}
              {t.type === 'warning' && <AlertTriangle className="w-5 h-5 text-amber-600" />}
              {t.type === 'info' && <Info className="w-5 h-5 text-blue-600" />}
            </div>

            <div className="flex-1 min-w-0">
              {t.title && (
                <h4 className="text-sm font-bold tracking-tight mb-0.5">
                  {t.title}
                </h4>
              )}
              <p className="text-xs font-medium leading-relaxed opacity-90 break-words">
                {t.message}
              </p>
            </div>

            <button
              type="button"
              onClick={() => removeToast(t.id)}
              className="shrink-0 p-1 rounded-lg opacity-60 hover:opacity-100 hover:bg-black/5 transition-colors"
              aria-label="Cerrar notificación"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};
