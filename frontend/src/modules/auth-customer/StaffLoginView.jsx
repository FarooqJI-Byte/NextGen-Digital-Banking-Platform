import React, { useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconLock, IconAlertTriangle } from '../../components/Icons.jsx';
import './LoginView.css';

export const StaffLoginView = ({ onNavigateToActivate, onNavigateToCustomerLogin }) => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  const validate = () => {
    const errs = {};
    if (!username.trim()) {
      errs.username = 'Employee username is required';
    }
    if (!password) {
      errs.password = 'Password is required';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await login(username, password, 'STAFF');
    } catch (err) {
      if (err.statusCode === 401) {
        setServerError('Invalid employee username or password.');
      } else if (err.statusCode === 423) {
        setServerError('Account locked due to 5 failed attempts. Please wait 15 minutes.');
      } else if (err.statusCode === 403) {
        setServerError('Account is disabled or unactivated. Please complete activation or contact administrator.');
      } else {
        setServerError(err.message || 'Authentication failed. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container auth-container--staff">
      <div className="auth-card portal--staff">
        <div className="auth-header">
          <div className="auth-brand">
            <span className="auth-brand__badge badge--staff">
              INTERNAL STAFF & ADMIN PORTAL
            </span>
          </div>
          <h1 className="auth-title">Staff & Admin Portal</h1>
          <p className="auth-subtitle">
            Authorized bank staff, auditor, and system administration access
          </p>
        </div>

        <div className="auth-staff-notice" style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-2)' }}>
          <IconLock size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <strong>Restricted Access:</strong> This internal portal is restricted to authorized personnel. Customer accounts cannot authenticate here.
          </div>
        </div>

        {serverError && (
          <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconAlertTriangle size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <FormField
            id="staff-login-username"
            label="Employee Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Enter staff username"
            error={errors.username}
            required
          />

          <FormField
            id="staff-login-password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter password"
            error={errors.password}
            required
          />

          <div className="auth-actions">
            <Button
              type="submit"
              variant="warning"
              disabled={isSubmitting}
              className="w-full"
            >
              {isSubmitting ? 'Authenticating...' : 'Sign In to Staff Portal'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Have an employee activation token?{' '}
          <button
            type="button"
            className="auth-link"
            onClick={onNavigateToActivate}
          >
            Activate Staff Account
          </button>
        </div>
      </div>
    </div>
  );
};

export default StaffLoginView;
