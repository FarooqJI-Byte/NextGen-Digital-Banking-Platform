import React, { useState, useEffect } from 'react';
import { adminApi } from './adminApi.js';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconUsers, IconRefresh, IconAlertTriangle, IconCheckCircle, IconInfo, IconLock } from '../../components/Icons.jsx';
import './AdminStaffManagementView.css';

export const AdminStaffManagementView = () => {
  const [staffList, setStaffList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [provisionedStaff, setProvisionedStaff] = useState(null);
  const [resendingId, setResendingId] = useState(null);

  // Form State
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState({});

  const loadStaff = async () => {
    try {
      setIsLoading(true);
      const data = await adminApi.listStaff();
      setStaffList(Array.isArray(data) ? data : []);
    } catch (err) {
      setServerError(err.message || 'Failed to load bank staff roster.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStaff();
  }, []);

  const validate = () => {
    const errs = {};
    if (!username.trim()) {
      errs.username = 'Staff username is required';
    } else if (username.trim().length < 3) {
      errs.username = 'Username must be at least 3 characters';
    }
    if (!email.trim()) {
      errs.email = 'Corporate email is required';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleCreateStaff = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccessMessage('');
    setProvisionedStaff(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const newStaff = await adminApi.createStaff({
        username,
        email,
      });

      setProvisionedStaff(newStaff);
      setUsername('');
      setEmail('');
      setErrors({});
      loadStaff();
    } catch (err) {
      if (err.statusCode === 409) {
        setServerError('An account with this username or email already exists.');
      } else {
        setServerError(err.message || 'Failed to provision staff account.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResendActivation = async (staff) => {
    setServerError('');
    setSuccessMessage('');
    setResendingId(staff.userId);
    try {
      await adminApi.resendStaffActivation(staff.userId);
      setSuccessMessage(`Fresh activation link generated for ${staff.username} (${staff.email}) and dispatched to notification service.`);
    } catch (err) {
      setServerError(err.message || 'Failed to regenerate activation link.');
    } finally {
      setResendingId(null);
    }
  };

  return (
    <div className="admin-staff-container">
      <div className="admin-header">
        <div className="admin-title-area">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconUsers size={22} /> Staff & Employee Management
          </h2>
          <p className="admin-subtitle">
            Admin console for provisioning and managing BANK_STAFF employee identities
          </p>
        </div>
        <Button variant="secondary" onClick={loadStaff} disabled={isLoading} style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconRefresh size={15} /> Refresh Roster
        </Button>
      </div>

      {serverError && (
        <div className="auth-error-banner" role="alert" style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconAlertTriangle size={18} />
          <span>{serverError}</span>
        </div>
      )}

      {successMessage && (
        <div className="auth-success-banner" role="status" style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconCheckCircle size={18} />
          <span>{successMessage}</span>
        </div>
      )}

      {provisionedStaff && (
        <div className="auth-success-banner" role="status" style={{ marginBottom: 'var(--space-4)', display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <IconCheckCircle size={18} />
            <div>
              <strong>Staff Account Successfully Provisioned:</strong> <code>{provisionedStaff.username}</code> (<code>{provisionedStaff.email}</code>)
            </div>
          </div>
          <div style={{ fontSize: '0.85rem', color: '#166534', backgroundColor: '#dcfce7', padding: 'var(--space-3)', borderRadius: 'var(--radius-md)', display: 'flex', alignItems: 'flex-start', gap: 'var(--space-2)' }}>
            <IconInfo size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div><strong>Activation Status:</strong> Account created in <code>Unactivated</code> state. Activation notification event dispatched. Employee will establish their own credentials through the Staff Activation portal.</div>
          </div>
        </div>
      )}

      <div className="admin-grid">
        {/* Form Card */}
        <div className="admin-card">
          <h3>Provision New Bank Staff</h3>

          <div className="staff-notice" style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-2)' }}>
            <IconLock size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Zero-Knowledge Provisioning:</strong> Admin does not set employee passwords. An activation token is generated for self-service onboarding.
            </div>
          </div>

          <form className="staff-form" onSubmit={handleCreateStaff} noValidate>
            <FormField
              id="staff-username"
              label="Staff Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter staff username"
              error={errors.username}
              required
            />

            <FormField
              id="staff-email"
              label="Corporate Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter corporate email address"
              error={errors.email}
              required
            />

            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="w-full"
            >
              {isSubmitting ? 'Provisioning Staff...' : 'Provision Staff Account'}
            </Button>
          </form>
        </div>

        {/* Staff Roster Card */}
        <div className="admin-card">
          <h3>Bank Staff Roster ({staffList.length})</h3>

          <div className="staff-table-wrapper">
            {isLoading ? (
              <div className="empty-state">Loading staff accounts...</div>
            ) : staffList.length === 0 ? (
              <div className="empty-state">No staff accounts provisioned yet. Use the form on the left to add one.</div>
            ) : (
              <table className="staff-table">
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {staffList.map((staff) => (
                    <tr key={staff.userId}>
                      <td><strong>{staff.username}</strong></td>
                      <td>{staff.email}</td>
                      <td><span className="badge-role">{staff.role}</span></td>
                      <td>
                        <span className={staff.isActive ? 'badge-active' : 'badge-role'} style={!staff.isActive ? { background: '#fed7aa', color: '#9a3412', borderColor: '#fb923c' } : {}}>
                          {staff.isActive ? 'Active' : 'Unactivated'}
                        </span>
                      </td>
                      <td>
                        {!staff.isActive ? (
                          <Button
                            variant="secondary"
                            onClick={() => handleResendActivation(staff)}
                            disabled={resendingId === staff.userId}
                            style={{ padding: '4px 10px', fontSize: '0.8rem' }}
                          >
                            {resendingId === staff.userId ? 'Sending...' : 'Resend Link'}
                          </Button>
                        ) : (
                          <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.8rem' }}>Activated</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminStaffManagementView;
