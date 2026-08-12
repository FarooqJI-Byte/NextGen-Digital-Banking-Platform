import React, { useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import './LoginView.css';

export const RegisterView = ({ onNavigateToLogin, onRegisterSuccess }) => {
  const { register } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errs = {};
    if (!username.trim()) {
      errs.username = 'Username is required';
    } else if (username.length < 3 || username.length > 50) {
      errs.username = 'Username must be between 3 and 50 characters';
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      errs.email = 'Email is required';
    } else if (!emailPattern.test(email.trim())) {
      errs.email = 'Please enter a valid email address';
    }

    // BR-AUTH-001 Complexity Rule
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!password) {
      errs.password = 'Password is required';
    } else if (!passwordPattern.test(password)) {
      errs.password = 'Password must be at least 8 characters with 1 uppercase, 1 lowercase, 1 digit, and 1 special character (@$!%*?&)';
    }

    if (!confirmPassword) {
      errs.confirmPassword = 'Confirm password is required';
    } else if (password !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccessMessage('');

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await register({
        username: username.trim(),
        email: email.trim(),
        password,
        role: 'CUSTOMER',
      });

      setSuccessMessage('Account registered successfully! Redirecting to sign in...');
      setTimeout(() => {
        if (typeof onRegisterSuccess === 'function') {
          onRegisterSuccess(response);
        } else if (typeof onNavigateToLogin === 'function') {
          onNavigateToLogin();
        }
      }, 1500);
    } catch (err) {
      if (err.errorCode === 'USERNAME_ALREADY_EXISTS') {
        setServerError('Username is already taken. Please choose another username.');
      } else if (err.errorCode === 'EMAIL_ALREADY_EXISTS') {
        setServerError('Email is already registered. Please sign in or use another email address.');
      } else {
        setServerError(err.message || 'Registration failed. Please review your details and try again.');
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
            <span className="auth-brand__badge">NEXTGEN BANKING</span>
          </div>
          <h1 className="auth-title">Create Account</h1>
          <p className="auth-subtitle">Register for modern, secure digital banking</p>
        </div>

        {serverError && (
          <div className="auth-error-banner" role="alert">
            <span>⚠️</span>
            <span>{serverError}</span>
          </div>
        )}

        {successMessage && (
          <div className="auth-success-banner" role="status">
            <span>✅</span>
            <span>{successMessage}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <FormField
            id="register-username"
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Choose a username"
            error={errors.username}
            required
          />

          <FormField
            id="register-email"
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@example.com"
            error={errors.email}
            required
          />

          <FormField
            id="register-password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Min. 8 chars (upper, lower, digit, special)"
            error={errors.password}
            required
          />

          <FormField
            id="register-confirm-password"
            label="Confirm Password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Re-enter your password"
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
              {isSubmitting ? 'Creating Account...' : 'Create Account'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Already have an account?{' '}
          <button
            type="button"
            className="auth-link"
            onClick={onNavigateToLogin}
          >
            Sign In
          </button>
        </div>
      </div>
    </div>
  );
};

export default RegisterView;
