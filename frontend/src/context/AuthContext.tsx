import React, { createContext, useContext, useState, useEffect } from 'react';
import { User } from '../types';
import { api } from '../services/api';

const IS_DEMO_ENABLED = import.meta.env.VITE_ENABLE_DEMO_SWITCHER === 'true';

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: any) => Promise<void>;
  logout: () => void;
  switchDemoRole: (role: 'PROPIETARIO' | 'CLIENTE') => Promise<void>;
  isDemoEnabled: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwt_token'));
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function loadUser() {
      if (token) {
        try {
          const profile = await api.getMe();
          setUser(profile);
        } catch (err) {
          console.error('Sesión expirada o token inválido:', err);
          logout();
        }
      }
      setLoading(false);
    }
    loadUser();
  }, []);

  const login = async (email: string, password: string) => {
    const data = await api.login(email, password);
    localStorage.setItem('jwt_token', data.token);
    setToken(data.token);
    setUser(data.usuario);
  };

  const register = async (payload: any) => {
    const data = await api.register(payload);
    localStorage.setItem('jwt_token', data.token);
    setToken(data.token);
    setUser(data.usuario);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    setToken(null);
    setUser(null);
  };

  const switchDemoRole = async (role: 'PROPIETARIO' | 'CLIENTE') => {
    if (!IS_DEMO_ENABLED) {
      console.warn('El conmutador demo está deshabilitado en este entorno');
      return;
    }
    // Solo roles no privilegiados para desarrollo
    let email = 'compras@colpatriaconstrucciones.com';
    if (role === 'PROPIETARIO') email = 'contacto@maquinariavalle.com';

    try {
      const data = await api.login(email, 'Password123!');
      localStorage.setItem('jwt_token', data.token);
      setToken(data.token);
      setUser(data.usuario);
    } catch (e) {
      console.error('Error switching demo role:', e);
    }
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, switchDemoRole, isDemoEnabled: IS_DEMO_ENABLED }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};
