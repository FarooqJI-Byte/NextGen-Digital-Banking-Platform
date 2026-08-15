import React, { useState } from 'react';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconAlertTriangle } from '../../components/Icons.jsx';
import { customerApi } from './customerApi.js';
import './CustomerProfileView.css';

export const KycSubmissionModal = ({ isOpen, onClose, onKycSubmitted }) => {
  const [documentType, setDocumentType] = useState('PAN');
  const [documentNumber, setDocumentNumber] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleFileChange = (e) => {
    const file = e.target.files && e.target.files[0];
    if (file) {
      setSelectedFile(file);
      if (errors.file) {
        setErrors((prev) => ({ ...prev, file: null }));
      }
    }
  };

  const validate = () => {
    const errs = {};

    if (!documentType) {
      errs.documentType = 'Document type is required';
    }

    if (!documentNumber.trim()) {
      errs.documentNumber = 'Document number is required';
    } else {
      if (documentType === 'PAN' && !/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(documentNumber.trim().toUpperCase())) {
        errs.documentNumber = 'PAN format must be 5 letters, 4 digits, 1 letter';
      }
    }

    if (!selectedFile) {
      errs.file = 'Please select a document file (.pdf, .jpg, .png) to upload';
    } else if (selectedFile.size > 10 * 1024 * 1024) {
      errs.file = 'File size exceeds 10 MB maximum limit';
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
      const response = await customerApi.uploadKycDocument({
        documentType,
        documentNumber: documentNumber.trim().toUpperCase(),
        file: selectedFile,
      });

      if (typeof onKycSubmitted === 'function') {
        onKycSubmitted(response);
      }
      onClose();
    } catch (err) {
      if (err.errorCode === 'DOCUMENT_ALREADY_EXISTS') {
        setServerError('This document is already registered in the banking system.');
      } else if (err.errorCode === 'KYC_ALREADY_VERIFIED') {
        setServerError('Your KYC is already verified. Additional document submissions are not required.');
      } else if (err.errorCode === 'KYC_SUBMISSION_PENDING_REVIEW') {
        setServerError('A KYC document is already submitted and pending compliance review. Please wait for staff review.');
      } else if (err.errorCode === 'UNSUPPORTED_KYC_FILE_FORMAT' || err.errorCode === 'UNSUPPORTED_KYC_MIME_TYPE') {
        setServerError('Invalid file format. Please upload a PDF, JPG, or PNG document.');
      } else if (err.errorCode === 'KYC_FILE_TOO_LARGE') {
        setServerError('The uploaded file exceeds the 10 MB maximum limit.');
      } else {
        setServerError(err.message || 'Failed to upload KYC document. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content" style={{ maxWidth: '520px' }}>
        <div className="modal-header">
          <h2>Upload KYC Document</h2>
          <button type="button" className="modal-close-btn" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        {serverError && (
          <div className="auth-error-banner" style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }} role="alert">
            <IconAlertTriangle size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="modal-form-section">
            <div className="form-field">
              <label className="form-field__label">
                Document Type <span style={{ color: 'var(--color-danger-600)' }}>*</span>
              </label>
              <select
                className="form-field__input"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
              >
                <option value="PAN">PAN Card (Income Tax Department)</option>
                <option value="AADHAAR">Aadhaar Card (UIDAI)</option>
                <option value="PASSPORT">Indian Passport</option>
                <option value="VOTER_ID">Voter ID (Election Commission)</option>
              </select>
            </div>

            <FormField
              id="kyc-doc-number"
              label="Document Identification Number"
              value={documentNumber}
              onChange={(e) => setDocumentNumber(e.target.value.toUpperCase())}
              placeholder="Enter document identification number"
              error={errors.documentNumber}
              required
            />

            {/* Document File Selector */}
            <div className="form-field" style={{ marginTop: 'var(--space-3)' }}>
              <label className="form-field__label">
                Select Document File (PDF, PNG, JPG — Max 10MB) <span style={{ color: 'var(--color-danger-600)' }}>*</span>
              </label>
              <input
                id="kyc-file-input"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                onChange={handleFileChange}
                style={{
                  width: '100%',
                  padding: 'var(--space-2)',
                  border: '1px solid var(--color-cream-200)',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-cream-50)',
                  fontFamily: 'var(--font-body)',
                  fontSize: '0.9rem',
                }}
              />
              {errors.file && <span className="form-field__error-message">{errors.file}</span>}
            </div>

            {selectedFile && (
              <div
                style={{
                  marginTop: 'var(--space-3)',
                  padding: 'var(--space-3)',
                  backgroundColor: 'var(--color-cream-100)',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--color-cream-200)',
                  fontSize: '0.85rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>
                    📄 <strong>{selectedFile.name}</strong> ({(selectedFile.size / 1024).toFixed(1)} KB)
                  </span>
                  <span style={{ color: 'var(--color-success-600)', fontWeight: 600 }}>Ready for Upload</span>
                </div>
              </div>
            )}
          </div>

          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={isSubmitting}>
              {isSubmitting ? 'Uploading Document...' : 'Upload & Submit Document'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default KycSubmissionModal;
