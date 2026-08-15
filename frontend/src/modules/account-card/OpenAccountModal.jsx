import React, { useState } from 'react';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import { openAccount } from './accountCardApi';
import './OpenAccountModal.css';

export default function OpenAccountModal({ customerId, isOpen, onClose, onAccountCreated }) {
  const [accountType, setAccountType] = useState('SAVINGS');
  const [currency, setCurrency] = useState('INR');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const newAcc = await openAccount({ customerId, accountType, currency });
      onAccountCreated(newAcc);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to open account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h3>Open New Bank Account</h3>
        {error && <div className="modal-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <FormField
            label="Account Type"
            type="select"
            value={accountType}
            onChange={(e) => setAccountType(e.target.value)}
            options={[
              { value: 'SAVINGS', label: 'Savings Account' },
              { value: 'CURRENT', label: 'Current Account' }
            ]}
          />
          <FormField
            label="Currency"
            type="text"
            value={currency}
            disabled
          />
          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={loading}>
              {loading ? 'Creating...' : 'Open Account'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
