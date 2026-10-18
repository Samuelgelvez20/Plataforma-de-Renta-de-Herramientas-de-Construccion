import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { CatalogView } from './components/CatalogView';
import { ToolDetailModal } from './components/ToolDetailModal';
import { RentalBookingModal } from './components/RentalBookingModal';
import { CheckoutPaymentModal } from './components/CheckoutPaymentModal';
import { ClientDashboard } from './components/ClientDashboard';
import { OwnerDashboard } from './components/OwnerDashboard';
import { AdminDashboard } from './components/AdminDashboard';
import { SwaggerViewerModal } from './components/SwaggerViewerModal';
import { AuthModal } from './components/AuthModal';
import { Tool, Reservation, Invoice } from './types';
import { ShieldCheck, HardHat, FileText, CheckCircle2 } from 'lucide-react';

function AppContent() {
  const { user } = useAuth();
  const [currentView, setCurrentView] = useState<'catalog' | 'client' | 'owner' | 'admin'>('catalog');

  // Modales
  const [selectedToolForDetail, setSelectedToolForDetail] = useState<Tool | null>(null);
  const [selectedToolForBooking, setSelectedToolForBooking] = useState<Tool | null>(null);
  const [paymentReservation, setPaymentReservation] = useState<Reservation | null>(null);
  const [showSwagger, setShowSwagger] = useState<boolean>(false);
  const [showAuth, setShowAuth] = useState<boolean>(false);

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

  return (
    <div className="min-h-screen flex flex-col bg-neutral-50 text-neutral-900">
      {/* Strict Top Bar Navigation */}
      <Navbar
        currentView={currentView}
        onNavigate={setCurrentView}
        onOpenSwagger={() => setShowSwagger(true)}
        onOpenAuth={() => setShowAuth(true)}
      />

      {/* Main Viewport Content */}
      <main className="flex-1">
        {currentView === 'catalog' && (
          <CatalogView
            onSelectTool={(tool) => setSelectedToolForDetail(tool)}
            onBookTool={handleBookTool}
          />
        )}

        {currentView === 'client' && (
          <ClientDashboard
            onPayReservation={(resv) => setPaymentReservation(resv)}
            onViewInvoice={(invoiceId) => {}}
          />
        )}

        {currentView === 'owner' && (
          <OwnerDashboard />
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
        onClose={() => setPaymentReservation(null)}
        onPaymentSuccess={handlePaymentSuccess}
      />

      {showSwagger && (
        <SwaggerViewerModal onClose={() => setShowSwagger(false)} />
      )}

      {showAuth && (
        <AuthModal onClose={() => setShowAuth(false)} />
      )}

      {/* Quiet Professional Footer */}
      <footer className="bg-white border-t border-neutral-200 mt-16 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-neutral-500">
          <div className="flex items-center gap-2">
            <HardHat className="w-4 h-4 text-amber-500" />
            <span className="font-semibold text-neutral-800">Plataforma de Renta de Equipos de Construcción</span>
            <span>·</span>
            <span>Arquitectura Hexagonal & DDD</span>
          </div>

          <div className="flex items-center gap-4 text-[11px] font-mono">
            <span>Spring Boot 3.2.4</span>
            <span>·</span>
            <span>Java 21</span>
            <span>·</span>
            <span>PostgreSQL 16</span>
            <span>·</span>
            <span>Flyway Migrations</span>
            <span>·</span>
            <span>React 19 + TypeScript</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

export function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
