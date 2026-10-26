import React, { useState, useEffect, useRef } from 'react';
import { ToastProvider } from './context/ToastContext';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { LandingView } from './components/LandingView';
import { CatalogView } from './components/CatalogView';
import { ToolDetailModal } from './components/ToolDetailModal';
import { RentalBookingModal } from './components/RentalBookingModal';
import { CheckoutPaymentModal } from './components/CheckoutPaymentModal';
import { ClientDashboard } from './components/ClientDashboard';
import { OwnerDashboard } from './components/OwnerDashboard';
import { AdminDashboard } from './components/AdminDashboard';
import { SwaggerViewerModal } from './components/SwaggerViewerModal';
import { AuthModal } from './components/AuthModal';
import { Footer } from './components/Footer';
import { Tool, Reservation, Invoice } from './types';
import { ShieldCheck, HardHat, FileText, CheckCircle2 } from 'lucide-react';

type View = 'landing' | 'catalog' | 'client' | 'owner' | 'admin';

function AppContent() {
  const { user } = useAuth();
  const [currentView, setCurrentView] = useState<View>('landing');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string | number | null>(null);
  const prevUserRef = useRef(user);

  // Gestión de transiciones de sesión entre visitante y usuario autenticado
  useEffect(() => {
    const prevUser = prevUserRef.current;
    prevUserRef.current = user;

    // Si el usuario acaba de iniciar sesión mientras estaba en la landing, dirigir al catálogo
    if (!prevUser && user && currentView === 'landing') {
      setCurrentView('catalog');
    }
    // Si el usuario cierra sesión y estaba en una vista protegida, dirigir a la landing
    if (prevUser && !user && (currentView === 'client' || currentView === 'owner' || currentView === 'admin')) {
      setCurrentView('landing');
    }
  }, [user?.id, user?.email]);

  // Modales
  const [selectedToolForDetail, setSelectedToolForDetail] = useState<Tool | null>(null);
  const [selectedToolForBooking, setSelectedToolForBooking] = useState<Tool | null>(null);
  const [paymentReservation, setPaymentReservation] = useState<Reservation | null>(null);
  const [showSwagger, setShowSwagger] = useState<boolean>(false);
  const [showAuth, setShowAuth] = useState<boolean>(false);
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState<number>(0);

  const handleBookTool = (tool: Tool) => {
    if (!user) {
      setShowAuth(true);
      return;
    }
    setSelectedToolForBooking(tool);
  };

  const handleBookingSuccess = () => {
    setSelectedToolForBooking(null);
    setCurrentView('client');
  };

  const handlePaymentSuccess = (invoice: Invoice) => {
    // Keep modal open to show success and PDF download
  };

  const handlePublishEquipment = () => {
    if (!user) {
      setShowAuth(true);
      return;
    }
    setCurrentView('owner');
  };

  const handleNavigateCatalogFromLanding = (categoryId?: string | number) => {
    setSelectedCategoryFilter(categoryId || null);
    setCurrentView('catalog');
  };

  const handleGlobalNavigate = (view: View) => {
    if (view === 'catalog') {
      setSelectedCategoryFilter(null);
    }
    setCurrentView(view);
  };

  return (
    <div className="min-h-screen flex flex-col bg-neutral-50 text-neutral-900">
      {/* Strict Top Bar Navigation */}
      <Navbar
        currentView={currentView}
        onNavigate={handleGlobalNavigate}
        onOpenSwagger={() => setShowSwagger(true)}
        onOpenAuth={() => setShowAuth(true)}
      />

      {/* Main Viewport Content */}
      <main className="flex-1">
        {currentView === 'landing' && (
          <LandingView
            onNavigateCatalog={handleNavigateCatalogFromLanding}
            onPublishEquipment={handlePublishEquipment}
            onOpenAuth={() => setShowAuth(true)}
          />
        )}

        {currentView === 'catalog' && (
          <CatalogView
            initialCategory={selectedCategoryFilter}
            onClearInitialCategory={() => setSelectedCategoryFilter(null)}
            onSelectTool={(tool) => setSelectedToolForDetail(tool)}
            onBookTool={handleBookTool}
          />
        )}

        {currentView === 'client' && (
          <ClientDashboard
            key={dashboardRefreshKey}
            onPayReservation={(resv) => setPaymentReservation(resv)}
            onViewInvoice={(invoiceId) => {}}
            onNavigateCatalog={() => handleGlobalNavigate('catalog')}
          />
        )}

        {currentView === 'owner' && (
          user?.rol === 'PROPIETARIO' || user?.rol === 'ADMIN' ? (
            <OwnerDashboard key={dashboardRefreshKey} onNavigateCatalog={() => handleGlobalNavigate('catalog')} />
          ) : (
            <div className="max-w-3xl mx-auto px-4 py-16 text-center">
              <div className="p-8 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                <ShieldCheck className="w-12 h-12 text-amber-500 mx-auto mb-3" />
                <h2 className="text-base font-bold text-neutral-900">Acceso Exclusivo para Propietarios</h2>
                <p className="text-xs text-neutral-600 mt-2 max-w-md mx-auto leading-relaxed">
                  Su cuenta actual ({user?.email}) tiene rol de {user?.rol}. Para registrar maquinaria, despachos u operar devoluciones técnicas, inicie sesión con una cuenta de Propietario o use el selector rápido 'Demo dev' en la barra superior.
                </p>
                <button
                  onClick={() => handleGlobalNavigate('catalog')}
                  className="mt-5 px-4 py-2 text-xs font-bold bg-neutral-900 text-white rounded-lg hover:bg-neutral-800 transition-colors cursor-pointer"
                >
                  Volver al Catálogo
                </button>
              </div>
            </div>
          )
        )}

        {currentView === 'admin' && (
          <AdminDashboard />
        )}
      </main>

      {/* Global Modals */}
      <ToolDetailModal
        tool={selectedToolForDetail}
        onClose={() => setSelectedToolForDetail(null)}
        onBook={handleBookTool}
      />

      <RentalBookingModal
        tool={selectedToolForBooking}
        onClose={() => setSelectedToolForBooking(null)}
        onSuccess={handleBookingSuccess}
        onOpenAuth={() => setShowAuth(true)}
      />

      <CheckoutPaymentModal
        reservation={paymentReservation}
        onClose={() => {
          setPaymentReservation(null);
          setDashboardRefreshKey(k => k + 1);
        }}
        onPaymentSuccess={handlePaymentSuccess}
      />

      {showSwagger && (
        <SwaggerViewerModal onClose={() => setShowSwagger(false)} />
      )}

      {showAuth && (
        <AuthModal onClose={() => setShowAuth(false)} />
      )}

      {/* Unified Industrial Footer */}
      <Footer
        onNavigate={handleGlobalNavigate}
        onOpenSwagger={() => setShowSwagger(true)}
      />
    </div>

  );
}

export function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ToastProvider>
  );
}

export default App;
