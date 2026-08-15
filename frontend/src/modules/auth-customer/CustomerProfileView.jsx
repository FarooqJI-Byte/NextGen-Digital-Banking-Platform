import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext.jsx';
import { customerApi } from './customerApi.js';
import { StatusBadge } from '../../components/StatusBadge.jsx';
import { Button } from '../../components/Button.jsx';
import { IconUser, IconShield, IconAlertTriangle, IconCheckCircle, IconInfo, IconClock, IconBan } from '../../components/Icons.jsx';
import { CreateProfileModal } from './CreateProfileModal.jsx';
import { KycSubmissionModal } from './KycSubmissionModal.jsx';
import './CustomerProfileView.css';

export const CustomerProfileView = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isKycModalOpen, setIsKycModalOpen] = useState(false);
  const [notification, setNotification] = useState('');

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await customerApi.getCustomerProfile();
      setProfile(data);
    } catch (err) {
      if (err.statusCode === 404) {
        setProfile(null);
      } else {
        setError(err.message || 'Unable to load customer profile.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleProfileCreated = (newProfile) => {
    setProfile(newProfile);
    setNotification('Customer profile created successfully!');
    setTimeout(() => setNotification(''), 4000);
  };

  const handleKycSubmitted = (submissionResult) => {
    loadProfile();
    setNotification(`KYC Document uploaded and submitted successfully! Verification reference: ${submissionResult.documentId}`);
    setTimeout(() => setNotification(''), 5000);
  };

  if (loading) {
    return (
      <div className="customer-profile-container" style={{ textAlign: 'center', padding: 'var(--space-8)' }}>
        <p style={{ color: 'var(--color-text-secondary)' }}>Loading customer profile...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="customer-profile-container">
        <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconAlertTriangle size={18} />
          <span>{error}</span>
        </div>
        <Button variant="secondary" onClick={loadProfile} style={{ alignSelf: 'center', marginTop: 'var(--space-4)' }}>
          Retry
        </Button>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="customer-profile-container">
        <div className="empty-profile-card">
          <div className="empty-profile-icon" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <IconUser size={40} />
          </div>
          <h2 style={{ fontFamily: 'var(--font-display)', color: 'var(--color-pine-950)' }}>
            Welcome, {user?.username || 'Customer'}
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', maxWidth: '480px' }}>
            You haven't set up your digital banking customer profile yet. Create your profile with personal details, address, and nominee to unlock banking features.
          </p>
          <Button variant="primary" onClick={() => setIsProfileModalOpen(true)}>
            Complete Customer Profile
          </Button>
        </div>

        <CreateProfileModal
          isOpen={isProfileModalOpen}
          onClose={() => setIsProfileModalOpen(false)}
          onProfileCreated={handleProfileCreated}
          initialEmail={user?.email || ''}
        />
      </div>
    );
  }

  const getKycBadgeVariant = (status) => {
    if (status === 'VERIFIED') return 'success';
    if (status === 'PENDING') return 'warning';
    if (status === 'REJECTED') return 'danger';
    return 'neutral';
  };

  const isKycVerified = profile.kycStatus === 'VERIFIED';
  const isKycRejected = profile.kycStatus === 'REJECTED';
  const isKycPendingUnderReview = profile.kycStatus === 'PENDING' && profile.hasSubmittedKycDocument;
  const isKycPendingInitial = profile.kycStatus === 'PENDING' && !profile.hasSubmittedKycDocument;

  return (
    <div className="customer-profile-container">
      {notification && (
        <div className="auth-success-banner" role="status" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconCheckCircle size={18} />
          <span>{notification}</span>
        </div>
      )}

      {/* Profile Header */}
      <div className="profile-header-card">
        <div className="profile-header-info">
          <h2>
            {profile.firstName} {profile.lastName}
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>
            Customer ID: <code style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--color-pine-950)' }}>{profile.customerNumber || profile.customerId}</code>
          </p>
          <div className="profile-badges">
            <StatusBadge
              status={getKycBadgeVariant(profile.kycStatus)}
              label={`KYC: ${profile.kycStatus || 'PENDING'}`}
            />
            {profile.riskCategory && (
              <span
                style={{
                  fontSize: '0.8rem',
                  padding: '2px 8px',
                  borderRadius: 'var(--radius-pill)',
                  backgroundColor: 'var(--color-cream-200)',
                  fontWeight: 600,
                }}
              >
                Risk: {profile.riskCategory}
              </span>
            )}
          </div>
        </div>

        <div>
          {isKycPendingInitial && (
            <Button variant="primary" onClick={() => setIsKycModalOpen(true)}>
              Submit KYC Document
            </Button>
          )}

          {isKycRejected && (
            <Button variant="primary" onClick={() => setIsKycModalOpen(true)}>
              Resubmit KYC Document
            </Button>
          )}

          {isKycPendingUnderReview && (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 'var(--space-2)',
                fontSize: '0.85rem',
                fontWeight: 600,
                color: 'var(--color-warning-600)',
                padding: 'var(--space-2) var(--space-3)',
                backgroundColor: 'var(--color-warning-100)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <IconClock size={15} /> Review in Progress
            </span>
          )}
        </div>
      </div>

      {/* KYC Status Guidance */}
      {isKycPendingInitial && (
        <div
          style={{
            backgroundColor: 'var(--color-info-100)',
            border: '1px solid var(--color-info-600)',
            color: 'var(--color-pine-950)',
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
          }}
        >
          <IconInfo size={20} style={{ flexShrink: 0 }} />
          <span>
            <strong>KYC Verification Required:</strong> Please submit a valid Government ID (PAN, Aadhaar, Passport, or Voter ID) using the button above to verify your identity and activate banking services.
          </span>
        </div>
      )}

      {isKycPendingUnderReview && (
        <div
          style={{
            backgroundColor: 'var(--color-warning-100)',
            border: '1px solid var(--color-warning-600)',
            color: 'var(--color-pine-950)',
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
          }}
        >
          <IconClock size={20} style={{ flexShrink: 0 }} />
          <span>
            <strong>KYC Submitted & Under Review:</strong> Your document has been submitted and is currently awaiting compliance verification by bank staff. You will be notified once reviewed.
          </span>
        </div>
      )}

      {isKycRejected && (
        <div
          style={{
            backgroundColor: 'var(--color-danger-100)',
            border: '1px solid var(--color-danger-600)',
            color: 'var(--color-danger-600)',
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
          }}
        >
          <IconBan size={20} style={{ flexShrink: 0 }} />
          <span>
            <strong>KYC Verification Rejected:</strong> Your previous document submission was rejected by compliance staff. Please click 'Resubmit KYC Document' above to upload a valid identity document.
          </span>
        </div>
      )}

      {isKycVerified && (
        <div
          style={{
            backgroundColor: 'var(--color-success-100)',
            border: '1px solid var(--color-success-600)',
            color: 'var(--color-pine-950)',
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
          }}
        >
          <IconShield size={20} style={{ flexShrink: 0 }} />
          <span>
            <strong>KYC Verified:</strong> Your identity is verified. Full digital banking and account facilities are active.
          </span>
        </div>
      )}

      {/* Details Grid */}
      <div className="profile-details-grid">
        {/* Personal Details */}
        <div className="profile-section-card">
          <h3>Personal Details</h3>
          <div className="profile-field-row">
            <span className="profile-field-label">Date of Birth</span>
            <span className="profile-field-value">{profile.dateOfBirth}</span>
          </div>
          <div className="profile-field-row">
            <span className="profile-field-label">Phone</span>
            <span className="profile-field-value">{profile.phone}</span>
          </div>
          <div className="profile-field-row">
            <span className="profile-field-label">Email</span>
            <span className="profile-field-value">{profile.email}</span>
          </div>
          <div className="profile-field-row">
            <span className="profile-field-label">Member Since</span>
            <span className="profile-field-value">
              {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : 'N/A'}
            </span>
          </div>
        </div>

        {/* Addresses */}
        <div className="profile-section-card">
          <h3>Addresses</h3>
          {profile.addresses && profile.addresses.length > 0 ? (
            profile.addresses.map((addr, idx) => (
              <div key={idx} style={{ marginBottom: 'var(--space-3)', paddingBottom: 'var(--space-2)' }}>
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    letterSpacing: '0.5px',
                    color: 'var(--color-gold-600)',
                  }}
                >
                  {addr.addressType} ADDRESS
                </span>
                <p style={{ marginTop: '4px', fontWeight: 500, color: 'var(--color-ink-900)' }}>
                  {addr.street}
                </p>
                <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
                  {addr.city}, {addr.state} — {addr.postalCode}
                </p>
                <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>{addr.country}</p>
              </div>
            ))
          ) : (
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>No addresses recorded</p>
          )}
        </div>

        {/* Nominees */}
        <div className="profile-section-card" style={{ gridColumn: '1 / -1' }}>
          <h3>Registered Nominees</h3>
          {profile.nominees && profile.nominees.length > 0 ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 'var(--space-4)' }}>
              {profile.nominees.map((nom, idx) => (
                <div
                  key={idx}
                  style={{
                    background: 'var(--color-cream-50)',
                    padding: 'var(--space-3)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-cream-200)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 'var(--space-2)' }}>
                    <strong>{nom.fullName}</strong>
                    <span
                      style={{
                        background: 'var(--color-pine-950)',
                        color: 'var(--color-gold-500)',
                        padding: '2px 8px',
                        borderRadius: 'var(--radius-pill)',
                        fontSize: '0.8rem',
                        fontWeight: 600,
                      }}
                    >
                      {nom.allocationPercentage}%
                    </span>
                  </div>
                  <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                    Relationship: {nom.relationship} | DOB: {nom.dateOfBirth}
                  </p>
                  <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                    Phone: {nom.phone}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>No nominee registered</p>
          )}
        </div>
      </div>

      <CreateProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        onProfileCreated={handleProfileCreated}
        initialEmail={user?.email || ''}
      />

      <KycSubmissionModal
        isOpen={isKycModalOpen}
        onClose={() => setIsKycModalOpen(false)}
        onKycSubmitted={handleKycSubmitted}
      />
    </div>
  );
};

export default CustomerProfileView;
