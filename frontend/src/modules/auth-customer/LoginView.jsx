import React, { useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconAlertTriangle } from '../../components/Icons.jsx';
import './LoginView.css';

export const LoginView = ({ onNavigateToRegister }) => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  const validate = () => {
    const errs = {};
    if (!username.trim()) {
      errs.username = 'Username is required';
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
      await login(username, password, 'CUSTOMER');
    } catch (err) {
      if (err.statusCode === 401) {
        setServerError('Invalid username or password.');
      } else if (err.statusCode === 423) {
        setServerError('Account locked due to 5 failed attempts. Please wait 15 minutes.');
      } else if (err.statusCode === 403) {
        setServerError('Account is disabled. Please contact customer support.');
      } else {
        setServerError(err.message || 'Authentication failed. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-brand">
            <span className="auth-brand__badge">NEXTGEN DIGITAL BANKING</span>
          </div>
          <h1 className="auth-title">Customer Banking</h1>
          <p className="auth-subtitle">
            Sign in to access your personal digital banking account
          </p>
        </div>

        {serverError && (
          <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconAlertTriangle size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <FormField
            id="login-username"
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Enter username"
            error={errors.username}
            required
          />

          <FormField
            id="login-password"
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
              variant="primary"
              disabled={isSubmitting}
              className="w-full"
            >
              {isSubmitting ? 'Authenticating...' : 'Sign In'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Don't have an account?{' '}
          <button
            type="button"
            className="auth-link"
            onClick={onNavigateToRegister}
          >
            Create Account
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginView;
