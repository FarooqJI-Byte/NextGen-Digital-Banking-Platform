import React, { useState, useEffect } from 'react';
import LedgerCard from '../../components/LedgerCard';
import Button from '../../components/Button';
import StatusBadge from '../../components/StatusBadge';
import OpenAccountModal from './OpenAccountModal';
import CardManagementView from './CardManagementView';
import { fetchCustomerAccounts, fetchCustomerCards, freezeAccount, unfreezeAccount } from './accountCardApi';
import './AccountDashboard.css';

export default function AccountDashboard({ customerId = 'cust-demo-123' }) {
  const [accounts, setAccounts] = useState([]);
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('accounts');

  const loadData = async () => {
    setLoading(true);
    try {
      const accData = await fetchCustomerAccounts(customerId);
      setAccounts(accData);
      const cardData = await fetchCustomerCards(customerId);
      setCards(cardData);
    } catch (err) {
      console.warn('Using mock dataset for fallback preview:', err.message);
      setAccounts([
        {
          accountId: 'acc-1',
          accountNumber: '501000123456',
          accountType: 'SAVINGS',
          balance: 124500.50,
          availableBalance: 119500.50,
          currency: 'INR',
          status: 'ACTIVE'
        }
      ]);
      setCards([
        {
          cardId: 'crd-1',
          maskedNumber: 'XXXX-XXXX-XXXX-4321',
          cardType: 'DEBIT',
          expiryDate: '2029-08-31',
          status: 'ACTIVE',
          dailyPosLimit: 50000,
          dailyAtmLimit: 25000
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [customerId]);

  const handleAccountCreated = (newAccount) => {
    setAccounts((prev) => [...prev, newAccount]);
  };

  const handleCardUpdated = (updatedCard) => {
    setCards((prev) =>
      prev.map((c) => (c.cardId === updatedCard.cardId ? updatedCard : c))
    );
  };

  const handleFreezeToggle = async (account) => {
    try {
      if (account.status === 'FROZEN') {
        const res = await unfreezeAccount(account.accountId);
        setAccounts((prev) => prev.map((a) => (a.accountId === res.accountId ? res : a)));
      } else {
        const res = await freezeAccount(account.accountId, 'Customer self-service request');
        setAccounts((prev) => prev.map((a) => (a.accountId === res.accountId ? res : a)));
      }
    } catch (err) {
      alert(err.message || 'Freeze action failed');
    }
  };

  return (
    <div className="account-dashboard-container">
      <div className="dashboard-header">
        <div>
          <h2>Accounts & Cards Management</h2>
          <p className="dashboard-subtitle">Overview of your bank accounts, debit/credit cards, and balance statements</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>+ Open Account</Button>
      </div>

      <div className="dashboard-tabs">
        <button
          className={`tab-button ${activeTab === 'accounts' ? 'active' : ''}`}
          onClick={() => setActiveTab('accounts')}
        >
          Bank Accounts ({accounts.length})
        </button>
        <button
          className={`tab-button ${activeTab === 'cards' ? 'active' : ''}`}
          onClick={() => setActiveTab('cards')}
        >
          Linked Cards ({cards.length})
        </button>
      </div>

      {loading ? (
        <div className="dashboard-loader">Loading accounts and card details...</div>
      ) : activeTab === 'accounts' ? (
        <div className="accounts-grid">
          {accounts.map((acc) => (
            <div key={acc.accountId} className="account-card-wrapper">
              <LedgerCard
                accountNumber={acc.accountNumber}
                accountType={acc.accountType}
                balance={acc.balance}
                availableBalance={acc.availableBalance}
                currency={acc.currency}
                status={acc.status}
              />
              <div className="account-card-actions">
                <StatusBadge status={acc.status} />
                <Button
                  variant="secondary"
                  size="small"
                  onClick={() => handleFreezeToggle(acc)}
                >
                  {acc.status === 'FROZEN' ? 'Unfreeze' : 'Freeze'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <CardManagementView cards={cards} onCardUpdated={handleCardUpdated} />
      )}

      <OpenAccountModal
        customerId={customerId}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAccountCreated={handleAccountCreated}
      />
    </div>
  );
}
