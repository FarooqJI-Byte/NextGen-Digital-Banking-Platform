import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext.jsx';
import { customerApi } from './customerApi.js';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { StatusBadge } from '../../components/StatusBadge.jsx';
import {
  IconShield,
  IconRefresh,
  IconAlertTriangle,
  IconCheckCircle,
  IconCheck,
  IconX,
  IconBan,
  IconClock,
  IconUser,
} from '../../components/Icons.jsx';
import './StaffKycView.css';

export const StaffKycView = () => {
  const { role } = useAuth();
  const [queue, setQueue] = useState([]);
  const [loadingQueue, setLoadingQueue] = useState(true);
  const [queueError, setQueueError] = useState('');
  const [successBanner, setSuccessBanner] = useState('');

  // Selected customer for review
  const [selectedItem, setSelectedItem] = useState(null);
  const [customerDetail, setCustomerDetail] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [detailError, setDetailError] = useState('');

  // Review Form state
  const [decision, setDecision] = useState('VERIFIED');
  const [remarks, setRemarks] = useState('');
  const [remarksError, setRemarksError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [verifyError, setVerifyError] = useState('');

  const isStaffOrAdmin = role === 'BANK_STAFF' || role === 'ADMIN';

  const loadPendingQueue = useCallback(async () => {
    setLoadingQueue(true);
    setQueueError('');
    try {
      const data = await customerApi.getPendingKycQueue();
      setQueue(Array.isArray(data) ? data : []);
    } catch (err) {
      setQueueError(err.message || 'Failed to load pending KYC queue.');
    } finally {
      setLoadingQueue(false);
    }
  }, []);

  useEffect(() => {
    if (isStaffOrAdmin) {
      loadPendingQueue();
    }
  }, [isStaffOrAdmin, loadPendingQueue]);

  const handleOpenReview = async (item) => {
    setSelectedItem(item);
    setCustomerDetail(null);
    setDetailError('');
    setDecision('VERIFIED');
    setRemarks('Identity documents verified against official records.');
    setRemarksError('');
    setVerifyError('');
    setLoadingDetail(true);

    try {
      const detail = await customerApi.getStaffKycDetail(item.customerId);
      setCustomerDetail(detail);
    } catch (err) {
      setDetailError(err.message || 'Failed to load customer KYC details.');
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleCloseReview = () => {
    setSelectedItem(null);
    setCustomerDetail(null);
  };

  const handleDecisionChange = (newDecision) => {
    setDecision(newDecision);
    if (newDecision === 'VERIFIED') {
      setRemarks('Identity documents verified against official records.');
    } else {
      setRemarks('Document image is blurred or details do not match profile.');
    }
    setRemarksError('');
  };

  const handleVerifySubmit = async (e) => {
    e.preventDefault();
    setVerifyError('');

    if (decision === 'REJECTED' && !remarks.trim()) {
      setRemarksError('Compliance audit remarks are required when rejecting KYC.');
      return;
    }

    setIsSubmitting(true);
    try {
      await customerApi.verifyKyc({
        customerId: selectedItem.customerId,
        status: decision,
        remarks: remarks.trim() || (decision === 'VERIFIED' ? 'Approved by staff' : 'Rejected by staff'),
      });

      const customerRef = selectedItem.customerNumber || selectedItem.customerName || 'Customer';
      setSuccessBanner(
        decision === 'VERIFIED'
          ? `KYC Approved: ${customerRef} successfully verified.`
          : `KYC Rejected: ${customerRef} marked as rejected.`
      );

      handleCloseReview();
      await loadPendingQueue();
    } catch (err) {
      setVerifyError(err.message || 'Failed to submit KYC verification decision.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isStaffOrAdmin) {
    return (
      <div className="staff-kyc-container">
        <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconBan size={18} />
          <span>Access Restricted: KYC verification portal is only accessible to BANK_STAFF and ADMIN roles.</span>
        </div>
      </div>
    );
  }

  return (
    <div className="staff-kyc-container">
      {/* Top Header */}
      <div className="staff-kyc-header">
        <div className="staff-kyc-title-area">
          <h2>
            <IconShield size={22} /> Staff KYC Verification Portal
          </h2>
          <p className="staff-kyc-subtitle">
            Review submitted customer identity documents and enforce regulatory KYC compliance.
          </p>
        </div>
        <Button
          variant="secondary"
          onClick={loadPendingQueue}
          disabled={loadingQueue}
          style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-2)' }}
        >
          <IconRefresh size={15} /> Refresh Queue
        </Button>
      </div>

      {successBanner && (
        <div className="auth-success-banner" role="status" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconCheckCircle size={18} />
          <span>{successBanner}</span>
        </div>
      )}

      {queueError && (
        <div className="auth-error-banner" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <IconAlertTriangle size={18} />
          <span>{queueError}</span>
        </div>
      )}

      {/* Main Queue Card */}
      <div className="staff-queue-card">
        <div className="staff-queue-header">
          <h3>
            Pending Reviews
            <span className="staff-queue-count-badge">{queue.length}</span>
          </h3>
        </div>

        {loadingQueue ? (
          <div style={{ textAlign: 'center', padding: 'var(--space-8)', color: 'var(--color-text-secondary)' }}>
            Loading pending KYC submissions...
          </div>
        ) : queue.length === 0 ? (
          <div className="empty-queue-card">
            <div className="empty-queue-icon">
              <IconShield size={28} />
            </div>
            <h4>Queue Cleared</h4>
            <p>
              There are currently no pending KYC submissions requiring staff review.
            </p>
          </div>
        ) : (
          <div className="queue-items-list">
            {queue.map((item) => (
              <div key={item.documentId || item.customerId} className="queue-item-card">
                <div className="queue-item-primary">
                  <div className="queue-item-name">{item.customerName || 'Customer'}</div>
                  <div className="queue-item-meta">
                    <span>
                      Customer ID: <code>{item.customerNumber || item.customerId}</code>
                    </span>
                    <span>•</span>
                    <span className="doc-pill">{item.documentType}</span>
                    <span>•</span>
                    <span>{item.email}</span>
                    <span>•</span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: 'var(--color-warning-600)' }}>
                      <IconClock size={13} /> Pending Review
                    </span>
                  </div>
                </div>
                <Button variant="primary" onClick={() => handleOpenReview(item)}>
                  Review Application
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Review Modal */}
      {selectedItem && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content" style={{ maxWidth: '680px' }}>
            <div className="modal-header">
              <h2 style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <IconShield size={20} /> KYC Compliance Review
              </h2>
              <button type="button" className="modal-close-btn" onClick={handleCloseReview} aria-label="Close">
                ×
              </button>
            </div>

            {detailError && (
              <div className="auth-error-banner" role="alert" style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <IconAlertTriangle size={18} />
                <span>{detailError}</span>
              </div>
            )}

            {verifyError && (
              <div className="auth-error-banner" role="alert" style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <IconAlertTriangle size={18} />
                <span>{verifyError}</span>
              </div>
            )}

            {loadingDetail ? (
              <div style={{ textAlign: 'center', padding: 'var(--space-8)', color: 'var(--color-text-secondary)' }}>
                Loading customer verification details...
              </div>
            ) : (
              <form onSubmit={handleVerifySubmit} noValidate>
                <div className="review-detail-grid">
                  {/* Customer Information */}
                  <div className="review-section">
                    <h4>Customer Profile</h4>
                    <div className="review-field-row">
                      <span className="review-field-label">Customer ID</span>
                      <span className="review-field-value">
                        <code>{customerDetail?.customerNumber || selectedItem?.customerNumber || '—'}</code>
                      </span>
                    </div>
                    <div className="review-field-row">
                      <span className="review-field-label">Full Name</span>
                      <span className="review-field-value">
                        {customerDetail ? `${customerDetail.firstName} ${customerDetail.lastName}` : selectedItem?.customerName}
                      </span>
                    </div>
                    <div className="review-field-row">
                      <span className="review-field-label">Email</span>
                      <span className="review-field-value">{customerDetail?.email || selectedItem?.email}</span>
                    </div>
                    <div className="review-field-row">
                      <span className="review-field-label">Phone</span>
                      <span className="review-field-value">{customerDetail?.phone || selectedItem?.phone}</span>
                    </div>
                    <div className="review-field-row">
                      <span className="review-field-label">Date of Birth</span>
                      <span className="review-field-value">{customerDetail?.dateOfBirth || '—'}</span>
                    </div>
                    {customerDetail?.addresses && customerDetail.addresses.length > 0 && (
                      <div className="review-field-row">
                        <span className="review-field-label">Address</span>
                        <span className="review-field-value" style={{ textAlign: 'right', fontSize: '0.8rem' }}>
                          {customerDetail.addresses[0].city}, {customerDetail.addresses[0].state}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Submitted KYC Documents */}
                  <div className="review-section">
                    <h4>Submitted Documents</h4>
                    <div className="doc-item-box">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span className="doc-pill">{selectedItem.documentType}</span>
                        <StatusBadge status="warning" label="PENDING" />
                      </div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                        File: <code>{selectedItem.fileReference || 'Stored Document'}</code>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Decision Selection */}
                <div style={{ marginTop: 'var(--space-2)' }}>
                  <label className="form-field__label" style={{ fontWeight: 600 }}>Verification Decision</label>
                  <div className="staff-action-group">
                    <div
                      className={`action-radio-card ${
                        decision === 'VERIFIED' ? 'action-radio-card--selected-verify' : ''
                      }`}
                      onClick={() => handleDecisionChange('VERIFIED')}
                    >
                      <strong style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                        <IconCheck size={16} /> APPROVE (VERIFIED)
                      </strong>
                      <span>Validates document and activates full banking access for customer.</span>
                    </div>

                    <div
                      className={`action-radio-card ${
                        decision === 'REJECTED' ? 'action-radio-card--selected-reject' : ''
                      }`}
                      onClick={() => handleDecisionChange('REJECTED')}
                    >
                      <strong style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                        <IconX size={16} /> REJECT (REJECTED)
                      </strong>
                      <span>Rejects submission due to blurred image, mismatch, or invalid ID.</span>
                    </div>
                  </div>
                </div>

                {/* Remarks Field */}
                <FormField
                  id="review-remarks"
                  label="Compliance Audit Remarks"
                  value={remarks}
                  onChange={(e) => {
                    setRemarks(e.target.value);
                    if (remarksError) setRemarksError('');
                  }}
                  placeholder="Enter verification notes and compliance rationale"
                  error={remarksError}
                  required={decision === 'REJECTED'}
                />

                {/* Actions */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)', marginTop: 'var(--space-6)' }}>
                  <Button type="button" variant="secondary" onClick={handleCloseReview} disabled={isSubmitting}>
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant={decision === 'VERIFIED' ? 'primary' : 'danger'}
                    disabled={isSubmitting}
                  >
                    {isSubmitting
                      ? 'Submitting...'
                      : decision === 'VERIFIED'
                      ? 'Approve Customer KYC'
                      : 'Reject Customer KYC'}
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default StaffKycView;
