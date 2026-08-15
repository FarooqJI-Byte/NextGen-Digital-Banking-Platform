import React, { useState } from 'react';
import { FormField } from '../../components/FormField.jsx';
import { Button } from '../../components/Button.jsx';
import { IconAlertTriangle } from '../../components/Icons.jsx';
import { customerApi } from './customerApi.js';
import './CustomerProfileView.css';

const normalizeIndianPhone = (raw) => {
  if (!raw) return '';
  let val = raw.trim().replace(/[\s\-()]/g, '');
  if (/^[6-9]\d{9}$/.test(val)) {
    return '+91' + val;
  }
  return val;
};

export const CreateProfileModal = ({ isOpen, onClose, onProfileCreated, initialEmail = '' }) => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState(initialEmail);

  // Primary address
  const [addressType, setAddressType] = useState('PERMANENT');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [stateName, setStateName] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [country, setCountry] = useState('India');

  // Nominee
  const [nomineeName, setNomineeName] = useState('');
  const [nomineeRel, setNomineeRel] = useState('Spouse');
  const [nomineeDob, setNomineeDob] = useState('');
  const [nomineePhone, setNomineePhone] = useState('');
  const [nomineeAlloc, setNomineeAlloc] = useState('100.00');

  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const validate = () => {
    const errs = {};

    if (!firstName.trim()) errs.firstName = 'First name is required';
    if (!lastName.trim()) errs.lastName = 'Last name is required';

    if (!dateOfBirth) {
      errs.dateOfBirth = 'Date of birth is required';
    } else {
      const dob = new Date(dateOfBirth);
      const today = new Date();
      let age = today.getFullYear() - dob.getFullYear();
      const m = today.getMonth() - dob.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
        age--;
      }
      if (age < 18) {
        errs.dateOfBirth = 'Customer must be at least 18 years old (BR-CUST-003)';
      }
    }

    const phoneRegex = /^\+91[6-9]\d{9}$/;
    const normalizedPhone = normalizeIndianPhone(phone);
    if (!normalizedPhone) {
      errs.phone = 'Phone number is required';
    } else if (!phoneRegex.test(normalizedPhone)) {
      errs.phone = 'Phone must be a valid 10-digit Indian mobile number (+91XXXXXXXXXX)';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      errs.email = 'Email is required';
    } else if (!emailRegex.test(email.trim())) {
      errs.email = 'Valid email is required';
    }

    if (!street.trim()) errs.street = 'Street address is required';
    if (!city.trim()) errs.city = 'City is required';
    if (!stateName.trim()) errs.stateName = 'State is required';

    const pinRegex = /^[1-9][0-9]{5}$/;
    if (!postalCode.trim()) {
      errs.postalCode = 'PIN code is required';
    } else if (!pinRegex.test(postalCode.trim())) {
      errs.postalCode = 'PIN code must be a valid 6-digit Indian postal code';
    }

    if (nomineeName.trim()) {
      if (!nomineeDob) errs.nomineeDob = 'Nominee date of birth is required';
      const normalizedNomineePhone = normalizeIndianPhone(nomineePhone);
      if (!normalizedNomineePhone) {
        errs.nomineePhone = 'Nominee phone number is required';
      } else if (!phoneRegex.test(normalizedNomineePhone)) {
        errs.nomineePhone = 'Nominee phone must be a valid 10-digit Indian mobile number (+91XXXXXXXXXX)';
      }
      const allocNum = parseFloat(nomineeAlloc);
      if (isNaN(allocNum) || allocNum !== 100.0) {
        errs.nomineeAlloc = 'Nominee allocation must equal exactly 100.00% (BR-CUST-004)';
      }
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
      const addresses = [
        {
          addressType,
          street: street.trim(),
          city: city.trim(),
          state: stateName.trim(),
          postalCode: postalCode.trim(),
          country: country.trim() || 'India',
        },
      ];

      const nominees = nomineeName.trim()
        ? [
            {
              fullName: nomineeName.trim(),
              relationship: nomineeRel.trim(),
              dateOfBirth: nomineeDob,
              phone: normalizeIndianPhone(nomineePhone),
              allocationPercentage: parseFloat(nomineeAlloc),
            },
          ]
        : [];

      const payload = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        dateOfBirth,
        phone: normalizeIndianPhone(phone),
        email: email.trim().toLowerCase(),
        addresses,
        nominees,
      };

      const created = await customerApi.createCustomerProfile(payload);
      if (typeof onProfileCreated === 'function') {
        onProfileCreated(created);
      }
      onClose();
    } catch (err) {
      if (err.errorCode === 'CUSTOMER_PROFILE_ALREADY_EXISTS') {
        setServerError('A customer profile already exists for your account.');
      } else if (err.errorCode === 'EMAIL_ALREADY_EXISTS') {
        setServerError('Email is already registered to another customer profile.');
      } else if (err.errorCode === 'PHONE_ALREADY_EXISTS') {
        setServerError('Phone number is already registered to another customer profile.');
      } else if (err.errorCode === 'UNDERAGE_CUSTOMER') {
        setServerError('Customer must be at least 18 years old (BR-CUST-003).');
      } else if (err.errorCode === 'INVALID_NOMINEE_ALLOCATION') {
        setServerError('Total nominee allocation must equal exactly 100.00% (BR-CUST-004).');
      } else {
        setServerError(err.message || 'Failed to create customer profile. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content">
        <div className="modal-header">
          <h2>Create Customer Profile</h2>
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
          {/* Section 1: Personal Info */}
          <div className="modal-form-section">
            <h4>1. Personal Information</h4>
            <div className="form-grid-2">
              <FormField
                id="profile-first-name"
                label="First Name"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="First name"
                error={errors.firstName}
                required
              />
              <FormField
                id="profile-last-name"
                label="Last Name"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Last name"
                error={errors.lastName}
                required
              />
            </div>
            <div className="form-grid-2">
              <FormField
                id="profile-dob"
                label="Date of Birth"
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                error={errors.dateOfBirth}
                required
              />
              <FormField
                id="profile-phone"
                label="Mobile Phone (+91...)"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+91 Mobile number"
                error={errors.phone}
                required
              />
            </div>
            <FormField
              id="profile-email"
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email address"
              error={errors.email}
              required
            />
          </div>

          {/* Section 2: Address */}
          <div className="modal-form-section">
            <h4>2. Primary Address</h4>
            <div className="form-grid-2">
              <div className="form-field">
                <label className="form-field__label">Address Type</label>
                <select
                  className="form-field__input"
                  value={addressType}
                  onChange={(e) => setAddressType(e.target.value)}
                >
                  <option value="PERMANENT">PERMANENT</option>
                  <option value="CURRENT">CURRENT</option>
                  <option value="COMMUNICATION">COMMUNICATION</option>
                </select>
              </div>
              <FormField
                id="profile-postal-code"
                label="PIN Code (6 digits)"
                value={postalCode}
                onChange={(e) => setPostalCode(e.target.value)}
                placeholder="6-digit PIN code"
                error={errors.postalCode}
                required
              />
            </div>
            <FormField
              id="profile-street"
              label="Street Address"
              value={street}
              onChange={(e) => setStreet(e.target.value)}
              placeholder="Building, street, and area"
              error={errors.street}
              required
            />
            <div className="form-grid-2">
              <FormField
                id="profile-city"
                label="City"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="City"
                error={errors.city}
                required
              />
              <FormField
                id="profile-state"
                label="State"
                value={stateName}
                onChange={(e) => setStateName(e.target.value)}
                placeholder="State"
                error={errors.stateName}
                required
              />
            </div>
          </div>

          {/* Section 3: Nominee */}
          <div className="modal-form-section">
            <h4>3. Nominee Details (Optional)</h4>
            <div className="form-grid-2">
              <FormField
                id="profile-nominee-name"
                label="Nominee Name"
                value={nomineeName}
                onChange={(e) => setNomineeName(e.target.value)}
                placeholder="Nominee full name"
              />
              <div className="form-field">
                <label className="form-field__label">Relationship</label>
                <select
                  className="form-field__input"
                  value={nomineeRel}
                  onChange={(e) => setNomineeRel(e.target.value)}
                >
                  <option value="Spouse">Spouse</option>
                  <option value="Parent">Parent</option>
                  <option value="Child">Child</option>
                  <option value="Sibling">Sibling</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>
            {nomineeName.trim() && (
              <>
                <div className="form-grid-2">
                  <FormField
                    id="profile-nominee-dob"
                    label="Nominee Date of Birth"
                    type="date"
                    value={nomineeDob}
                    onChange={(e) => setNomineeDob(e.target.value)}
                    error={errors.nomineeDob}
                    required
                  />
                  <FormField
                    id="profile-nominee-phone"
                    label="Nominee Phone"
                    value={nomineePhone}
                    onChange={(e) => setNomineePhone(e.target.value)}
                    placeholder="+91 Nominee mobile number"
                    error={errors.nomineePhone}
                    required
                  />
                </div>
                <FormField
                  id="profile-nominee-alloc"
                  label="Allocation Percentage (%)"
                  type="number"
                  value={nomineeAlloc}
                  onChange={(e) => setNomineeAlloc(e.target.value)}
                  placeholder="100.00"
                  error={errors.nomineeAlloc}
                  required
                />
              </>
            )}
          </div>

          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={isSubmitting}>
              {isSubmitting ? 'Creating Profile...' : 'Save Profile'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateProfileModal;
