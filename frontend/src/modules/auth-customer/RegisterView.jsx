import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from './AuthContext.jsx';
import { authApi } from './authApi.js';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconAlertTriangle, IconCheckCircle, IconShield, IconInfo, IconRefresh } from '../../components/Icons.jsx';
import './LoginView.css';

export const RegisterView = ({ onNavigateToLogin }) => {
  const { register } = useAuth();

  // Registration Form State
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // OTP Verification State
  // Steps: 'REGISTER' | 'VERIFY_OTP' | 'VERIFIED'
  const [step, setStep] = useState('REGISTER');
  const [otp, setOtp] = useState('');
  const [otpError, setOtpError] = useState('');
  const [otpSuccessMessage, setOtpSuccessMessage] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  // Countdown timer ref
  const timerRef = useRef(null);

  useEffect(() => {
    if (resendCooldown > 0) {
      timerRef.current = setTimeout(() => {
        setResendCooldown((prev) => prev - 1);
      }, 1000);
    }
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [resendCooldown]);

  const validateRegistration = () => {
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

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validateRegistration()) {
      return;
    }

    setIsSubmitting(true);
    try {
      // 1. Create customer account per 08-api-contracts.md
      await register({
        username: username.trim(),
        email: email.trim().toLowerCase(),
        password,
        role: 'CUSTOMER',
      });

      // 2. Transition directly to OTP verification view
      setStep('VERIFY_OTP');
      setResendCooldown(60);
      setOtp('');
      setOtpError('');
      setOtpSuccessMessage('Account created. A 6-digit verification code has been dispatched to your email.');
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

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setOtpError('');
    setOtpSuccessMessage('');

    const cleanOtp = otp.trim();
    if (!cleanOtp) {
      setOtpError('Please enter the 6-digit verification code');
      return;
    }

    if (!/^\d{6}$/.test(cleanOtp)) {
      setOtpError('Verification code must be exactly 6 numeric digits');
      return;
    }

    setIsVerifying(true);
    try {
      await authApi.verifyOtp({
        identifier: email.trim().toLowerCase(),
        purpose: 'CUSTOMER_REGISTRATION',
        otp: cleanOtp,
      });

      setStep('VERIFIED');
    } catch (err) {
      if (err.errorCode === 'NO_ACTIVE_OTP') {
        setOtpError('No active OTP found. Please click "Resend Code" below to receive a new one.');
      } else if (err.errorCode === 'OTP_EXPIRED') {
        setOtpError('Verification code has expired. Please request a fresh code.');
      } else if (err.errorCode === 'OTP_MAX_ATTEMPTS_EXCEEDED') {
        setOtpError('Maximum verification attempts exceeded. Please request a fresh code.');
      } else {
        setOtpError(err.message || 'Invalid verification code. Please check and try again.');
      }
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResendOtp = async () => {
    if (resendCooldown > 0 || isResending) return;

    setOtpError('');
    setOtpSuccessMessage('');
    setIsResending(true);

    try {
      await authApi.resendOtp({
        identifier: email.trim().toLowerCase(),
        purpose: 'CUSTOMER_REGISTRATION',
      });

      setResendCooldown(60);
      setOtp('');
      setOtpSuccessMessage('A fresh 6-digit verification code has been dispatched.');
    } catch (err) {
      setOtpError(err.message || 'Failed to resend verification code. Please try again.');
    } finally {
      setIsResending(false);
    }
  };

  // -------------------------------------------------------------
  // STEP 3: SUCCESS STATE
  // -------------------------------------------------------------
  if (step === 'VERIFIED') {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <div className="auth-brand">
              <span className="auth-brand__badge">NEXTGEN BANKING</span>
            </div>
            <div style={{ margin: 'var(--space-4) auto var(--space-2)', display: 'flex', justifyContent: 'center' }}>
              <IconCheckCircle size={48} color="var(--color-pine-700)" />
            </div>
            <h1 className="auth-title">Email Verified</h1>
            <p className="auth-subtitle">
              Your customer identity is verified and ready.
            </p>
          </div>

          <div className="auth-success-banner" role="status" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconCheckCircle size={18} />
            <span>Registration complete. You may now sign in to your banking portal.</span>
          </div>

          <div className="auth-actions" style={{ marginTop: 'var(--space-5)' }}>
            <Button
              type="button"
              variant="primary"
              onClick={onNavigateToLogin}
              className="w-full"
            >
              Proceed to Sign In
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // STEP 2: OTP VERIFICATION VIEW
  // -------------------------------------------------------------
  if (step === 'VERIFY_OTP') {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <div className="auth-brand">
              <span className="auth-brand__badge">NEXTGEN BANKING</span>
            </div>
            <h1 className="auth-title">Verify Your Email</h1>
            <p className="auth-subtitle">
              We sent a 6-digit verification code to<br />
              <strong style={{ color: 'var(--color-pine-950)' }}>{email}</strong>
            </p>
          </div>

          {otpError && (
            <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <IconAlertTriangle size={18} />
              <span>{otpError}</span>
            </div>
          )}

          {otpSuccessMessage && (
            <div className="auth-success-banner" role="status" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <IconCheckCircle size={18} />
              <span>{otpSuccessMessage}</span>
            </div>
          )}

          <form className="auth-form" onSubmit={handleVerifyOtp} noValidate>
            <div>
              <label
                htmlFor="otp-input"
                style={{
                  display: 'block',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: 'var(--color-pine-950)',
                  marginBottom: 'var(--space-2)',
                  textAlign: 'center',
                }}
              >
                Enter 6-Digit Code
              </label>
              <input
                id="otp-input"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={6}
                autoComplete="one-time-code"
                autoFocus
                value={otp}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                  setOtp(val);
                }}
                className="auth-otp-input"
                style={{
                  width: '100%',
                  borderRadius: 'var(--radius-md)',
                  border: '2px solid var(--color-pine-300)',
                  outline: 'none',
                  backgroundColor: '#FFFFFF',
                }}
                placeholder="••••••"
              />
            </div>

            <div className="auth-actions">
              <Button
                type="submit"
                variant="primary"
                disabled={isVerifying || otp.length !== 6}
                className="w-full"
              >
                {isVerifying ? 'Verifying Code...' : 'Verify OTP'}
              </Button>
            </div>
          </form>

          {/* Resend OTP Bar */}
          <div className="auth-resend-row">
            <span style={{ color: '#475569' }}>Didn't receive the code?</span>
            {resendCooldown > 0 ? (
              <span style={{ color: '#64748B', fontWeight: 600 }}>
                Resend in {resendCooldown}s
              </span>
            ) : (
              <button
                type="button"
                className="auth-link"
                onClick={handleResendOtp}
                disabled={isResending}
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}
              >
                <IconRefresh size={14} />
                {isResending ? 'Sending...' : 'Resend Code'}
              </button>
            )}
          </div>

          {/* Development Mode Helper Box */}
          <div className="auth-dev-badge">
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-2)' }}>
              <IconInfo size={16} style={{ flexShrink: 0, marginTop: '2px', color: '#0369a1' }} />
              <div>
                <strong>Local Development Mode:</strong> Check your backend console logs for the generated 6-digit OTP code (<code>[DEV FALLBACK]</code>).
              </div>
            </div>
          </div>

          <div className="auth-footer" style={{ marginTop: 'var(--space-4)' }}>
            <button
              type="button"
              className="auth-link"
              onClick={() => {
                setStep('REGISTER');
                setOtpError('');
                setOtpSuccessMessage('');
              }}
            >
              ← Back to Registration (Change Email)
            </button>
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // STEP 1: INITIAL REGISTRATION FORM
  // -------------------------------------------------------------
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
          <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconAlertTriangle size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleRegisterSubmit} noValidate>
          <FormField
            id="register-username"
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Choose username (3–50 characters)"
            error={errors.username}
            required
          />

          <FormField
            id="register-email"
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter email address"
            error={errors.email}
            required
          />

          <FormField
            id="register-password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Create password (min. 8 characters)"
            error={errors.password}
            required
          />

          <FormField
            id="register-confirm-password"
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
