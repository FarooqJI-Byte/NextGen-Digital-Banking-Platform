import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './modules/auth-customer/AuthContext.jsx';
import { LoginView } from './modules/auth-customer/LoginView.jsx';
import { StaffLoginView } from './modules/auth-customer/StaffLoginView.jsx';
import { RegisterView } from './modules/auth-customer/RegisterView.jsx';
import { StaffActivationView } from './modules/auth-customer/StaffActivationView.jsx';
import { CustomerProfileView } from './modules/auth-customer/CustomerProfileView.jsx';
import { StaffKycView } from './modules/auth-customer/StaffKycView.jsx';
import { AdminStaffManagementView } from './modules/auth-customer/AdminStaffManagementView.jsx';
import { ComponentShowcase } from './components/ComponentShowcase.jsx';
import { Button } from './components/Button.jsx';
import { StatusBadge } from './components/StatusBadge.jsx';
import { IconUser, IconShield, IconUsers, IconPalette } from './components/Icons.jsx';

function MainApp() {
  const { isAuthenticated, user, role, logout } = useAuth();
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [activeTab, setActiveTab] = useState('profile');

  const isAdmin = role === 'ADMIN';
  const isStaff = role === 'BANK_STAFF';
  const isCustomer = role === 'CUSTOMER';
  const isStaffOrAdmin = isStaff || isAdmin;

  // Listen to browser navigation (back/forward)
  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname);
    };

    window.addEventListener('popstate', handleLocationChange);
    return () => window.removeEventListener('popstate', handleLocationChange);
  }, []);

  const navigateTo = (path) => {
    window.history.pushState(null, '', path);
    setCurrentPath(path);
  };

  // Set default tab according to role
  useEffect(() => {
    if (isAdmin) {
      setActiveTab('admin-staff');
    } else if (isStaff) {
      setActiveTab('staff-kyc');
    } else if (isCustomer) {
      setActiveTab('profile');
    }
  }, [role, isAdmin, isStaff, isCustomer]);

  // Unauthenticated Navigation Logic
  if (!isAuthenticated) {
    // 1. Staff Activation (/staff/activate or /activate-staff)
    const isStaffActivationRoute =
      currentPath === '/staff/activate' ||
      currentPath.startsWith('/staff/activate') ||
      currentPath === '/activate-staff' ||
      currentPath.startsWith('/activate-staff');

    if (isStaffActivationRoute) {
      const searchToken = new URLSearchParams(window.location.search).get('token') || '';
      return (
        <StaffActivationView
          defaultToken={searchToken}
          onNavigateToLogin={() => navigateTo('/staff/login')}
        />
      );
    }

    // 2. Internal Staff & Admin Login (/staff/login or /staff)
    if (currentPath === '/staff/login' || currentPath === '/staff') {
      return (
        <StaffLoginView
          onNavigateToActivate={() => navigateTo('/staff/activate')}
          onNavigateToCustomerLogin={() => navigateTo('/login')}
        />
      );
    }

    // 3. Customer Registration (/register)
    if (currentPath === '/register') {
      return (
        <RegisterView
          onNavigateToLogin={() => navigateTo('/login')}
          onRegisterSuccess={() => navigateTo('/login')}
        />
      );
    }

    // 4. Default / Customer Banking Login (/login, /, etc.)
    return (
      <LoginView
        onNavigateToRegister={() => navigateTo('/register')}
      />
    );
  }

  // Authenticated Portal View
  return (
    <div className="app-shell" style={{ minHeight: '100vh', backgroundColor: 'var(--color-cream-50)' }}>
      {/* Top Header */}
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: 'var(--space-4) var(--space-6)',
          backgroundColor: 'var(--color-pine-950)',
          color: 'var(--color-text-inverse)',
          borderBottom: '1px solid var(--color-pine-800)',
          boxShadow: 'var(--shadow-glass)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <span
            style={{
              fontFamily: 'var(--font-display)',
              fontWeight: 700,
              fontSize: '1.15rem',
              color: 'var(--color-gold-500)',
              letterSpacing: '0.5px',
            }}
          >
            NEXTGEN DIGITAL BANK
          </span>
          {role && (
            <StatusBadge
              status={role === 'ADMIN' ? 'danger' : role === 'BANK_STAFF' ? 'warning' : 'success'}
              label={role}
            />
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
          <span style={{ fontSize: '0.9rem', color: 'var(--color-cream-100)' }}>
            Signed in as <strong>{user?.username}</strong>
          </span>
          <Button
            variant="inverse"
            onClick={() => {
              logout();
              navigateTo(isStaffOrAdmin ? '/staff/login' : '/login');
            }}
          >
            Sign Out
          </Button>
        </div>
      </header>

      {/* Role-Aware Navigation Bar */}
      <nav
        style={{
          display: 'flex',
          gap: 'var(--space-2)',
          padding: 'var(--space-3) var(--space-6)',
          backgroundColor: 'var(--glass-fill-solid)',
          borderBottom: '1px solid var(--color-cream-200)',
        }}
      >
        {isCustomer && (
          <button
            type="button"
            onClick={() => setActiveTab('profile')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 'var(--space-2)',
              padding: 'var(--space-2) var(--space-4)',
              borderRadius: 'var(--radius-md)',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.9rem',
              backgroundColor: activeTab === 'profile' ? 'var(--color-pine-950)' : 'transparent',
              color: activeTab === 'profile' ? 'var(--color-gold-500)' : 'var(--color-pine-950)',
              transition: 'all 0.2s ease',
            }}
          >
            <IconUser size={16} /> Customer Profile & KYC
          </button>
        )}

        {isStaffOrAdmin && (
          <button
            type="button"
            onClick={() => setActiveTab('staff-kyc')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 'var(--space-2)',
              padding: 'var(--space-2) var(--space-4)',
              borderRadius: 'var(--radius-md)',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.9rem',
              backgroundColor: activeTab === 'staff-kyc' ? 'var(--color-pine-950)' : 'transparent',
              color: activeTab === 'staff-kyc' ? 'var(--color-gold-500)' : 'var(--color-pine-950)',
              transition: 'all 0.2s ease',
            }}
          >
            <IconShield size={16} /> Staff KYC Verification
          </button>
        )}

        {isAdmin && (
          <button
            type="button"
            onClick={() => setActiveTab('admin-staff')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 'var(--space-2)',
              padding: 'var(--space-2) var(--space-4)',
              borderRadius: 'var(--radius-md)',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.9rem',
              backgroundColor: activeTab === 'admin-staff' ? 'var(--color-pine-950)' : 'transparent',
              color: activeTab === 'admin-staff' ? 'var(--color-gold-500)' : 'var(--color-pine-950)',
              transition: 'all 0.2s ease',
            }}
          >
            <IconUsers size={16} /> Admin Staff Management
          </button>
        )}

        <button
          type="button"
          onClick={() => setActiveTab('showcase')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
            padding: 'var(--space-2) var(--space-4)',
            borderRadius: 'var(--radius-md)',
            border: 'none',
            cursor: 'pointer',
            fontWeight: 600,
            fontSize: '0.9rem',
            backgroundColor: activeTab === 'showcase' ? 'var(--color-pine-950)' : 'transparent',
            color: activeTab === 'showcase' ? 'var(--color-gold-500)' : 'var(--color-pine-950)',
            transition: 'all 0.2s ease',
          }}
        >
          <IconPalette size={16} /> UI Design Showcase
        </button>
      </nav>

      {/* Main Content View */}
      <main style={{ padding: 'var(--space-6)' }}>
        {activeTab === 'profile' && isCustomer && <CustomerProfileView />}
        {activeTab === 'staff-kyc' && isStaffOrAdmin && <StaffKycView />}
        {activeTab === 'admin-staff' && isAdmin && <AdminStaffManagementView />}
        {activeTab === 'showcase' && <ComponentShowcase />}
      </main>
    </div>
  );
}

export function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

export default App;
