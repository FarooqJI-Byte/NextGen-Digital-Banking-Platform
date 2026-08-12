import React, { useState } from 'react';
import { AuthProvider, useAuth } from './modules/auth-customer/AuthContext.jsx';
import { LoginView } from './modules/auth-customer/LoginView.jsx';
import { RegisterView } from './modules/auth-customer/RegisterView.jsx';
import { ComponentShowcase } from './components/ComponentShowcase.jsx';
import { Button } from './components/Button.jsx';
import { StatusBadge } from './components/StatusBadge.jsx';

function MainApp() {
  const { isAuthenticated, user, role, logout } = useAuth();
  const [authView, setAuthView] = useState('login'); // 'login' | 'register'

  if (!isAuthenticated) {
    return authView === 'register' ? (
      <RegisterView
        onNavigateToLogin={() => setAuthView('login')}
        onRegisterSuccess={() => setAuthView('login')}
      />
    ) : (
      <LoginView
        onNavigateToRegister={() => setAuthView('register')}
      />
    );
  }

  return (
    <div className="app-shell">
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: 'var(--space-4) var(--space-6)',
          backgroundColor: 'var(--color-pine-950)',
          color: 'var(--color-text-inverse)',
          borderBottom: '1px solid var(--color-pine-800)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <span
            style={{
              fontFamily: 'var(--font-display)',
              fontWeight: 700,
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
          <Button variant="secondary" onClick={logout}>
            Sign Out
          </Button>
        </div>
      </header>

      <main style={{ padding: 'var(--space-6)' }}>
        <ComponentShowcase />
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
