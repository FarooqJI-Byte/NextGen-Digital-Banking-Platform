import React, { useState, useEffect } from 'react';
import { authApi } from './authApi.js';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconShield, IconAlertTriangle, IconCheckCircle, IconInfo, IconLock } from '../../components/Icons.jsx';
import './LoginView.css';
import './StaffActivationView.css';

export const StaffActivationView = ({ onNavigateToLogin, defaultToken = '' }) => {
  // Query parameter extraction fallback
  const initialToken = defaultToken || new URLSearchParams(window.location.search).get('token') || '';
  const [token, setToken] = useState(initialToken);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const [isActivated, setIsActivated] = useState(false);
  const [activatedUsername, setActivatedUsername] = useState('');

  useEffect(() => {
    if (!token && initialToken) {
      setToken(initialToken);
    }
  }, [initialToken, token]);

  const validate = () => {
    const errs = {};
    if (!token.trim()) {
      errs.token = 'Activation token is required';
    } else if (token.trim().length < 32) {
      errs.token = 'Please enter a valid activation token';
    }

    if (!password) {
      errs.password = 'Password is required';
    } else if (password.length < 8) {
      errs.password = 'Password must be at least 8 characters';
    } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(password)) {
      errs.password = 'Password must include uppercase, lowercase, digit, and special char (@$!%*?&)';
    }

    if (!confirmPassword) {
      errs.confirmPassword = 'Confirmation password is required';
    } else if (password !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleActivate = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const response = await authApi.activateStaff({
        token: token.trim(),
        password,
        confirmPassword,
      });

      setActivatedUsername(response?.username || '');
      setIsActivated(true);
    } catch (err) {
      if (err.errorCode === 'INVALID_ACTIVATION_TOKEN' || err.statusCode === 400) {
        setServerError(err.message || 'Invalid, expired, or already used activation token. Please request a new activation link from your administrator.');
      } else {
        setServerError(err.message || 'Staff activation failed. Please check your credentials and try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isActivated) {
    return (
      <div className="auth-container auth-container--staff">
        <div className="auth-card portal--staff">
          <div className="auth-header">
            <div className="auth-brand">
              <span className="auth-brand__badge badge--staff">
                NEXTGEN BANKING &bull; INTERNAL
              </span>
            </div>
            <div style={{ margin: 'var(--space-4) auto var(--space-2)', display: 'flex', justifyContent: 'center' }}>
              <IconCheckCircle size={48} color="var(--color-pine-700)" />
            </div>
            <h1 className="auth-title">Staff Account Activated</h1>
            <p className="auth-subtitle">
              {activatedUsername ? `Welcome, ${activatedUsername}. ` : ''}Your employee identity is now active.
            </p>
          </div>

          <div className="auth-success-banner" role="status" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconCheckCircle size={18} />
            <span>Password successfully established. You may now log in to the Internal Portal.</span>
          </div>

          <div className="auth-actions" style={{ marginTop: 'var(--space-5)' }}>
            <Button
              type="button"
              variant="primary"
              onClick={onNavigateToLogin}
              className="w-full"
            >
              Proceed to Staff Sign In
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container auth-container--staff">
      <div className="auth-card portal--staff">
        <div className="auth-header">
          <div className="auth-brand">
            <span className="auth-brand__badge badge--staff">
              NEXTGEN BANKING
            </span>
          </div>
          <h1 className="auth-title">Staff Account Activation</h1>
          <p className="auth-subtitle">
            Establish your employee credentials to activate your corporate account
          </p>
        </div>

        <div className="auth-staff-notice">
          <IconLock size={16} style={{ flexShrink: 0, marginTop: '2px', color: '#854d0e' }} />
          <div>
            <strong>Self-Service Onboarding:</strong> Single-use activation token expires after 24 hours. Your password is never shared with administrators.
          </div>
        </div>

        {serverError && (
          <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconAlertTriangle size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleActivate} noValidate>
          <div>
            <FormField
              id="activation-token"
              label="Activation Token"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Paste 64-character activation token"
              error={errors.token}
              required
            />
          </div>

          <div>
            <FormField
              id="activation-password"
              label="New Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Create password (min. 8 characters)"
              error={errors.password}
              required
            />
            <div className="password-rules" style={{ marginTop: '-4px', fontSize: '0.78rem', color: '#64748B' }}>
              Min 8 chars: 1 uppercase, 1 lowercase, 1 digit, 1 special char (@$!%*?&)
            </div>
          </div>

          <FormField
            id="activation-confirm-password"
            label="Confirm Password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Re-enter password"
            error={errors.confirmPassword}
            required
          />

          <div className="auth-actions">
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="w-full"
            >
              {isSubmitting ? 'Activating Staff Account...' : 'Activate Staff Account'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Already activated?{' '}
          <button
            type="button"
            className="auth-link"
            onClick={onNavigateToLogin}
          >
            Staff Sign In
          </button>
        </div>
      </div>
    </div>
  );
};

export default StaffActivationView;
