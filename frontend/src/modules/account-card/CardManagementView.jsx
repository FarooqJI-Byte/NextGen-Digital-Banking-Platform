import React, { useState } from 'react';
import HorizonCard from '../../components/HorizonCard';
import StatusBadge from '../../components/StatusBadge';
import Button from '../../components/Button';
import { blockCard, unblockCard } from './accountCardApi';
import './CardManagementView.css';

export default function CardManagementView({ cards, onCardUpdated }) {
  const [loadingCardId, setLoadingCardId] = useState(null);

  const handleToggleBlock = async (card) => {
    setLoadingCardId(card.cardId);
    try {
      if (card.status === 'BLOCKED') {
        const updated = await unblockCard(card.cardId);
        onCardUpdated(updated);
      } else {
        const updated = await blockCard(card.cardId, 'CUSTOMER_REQUEST');
        onCardUpdated(updated);
      }
    } catch (err) {
      alert(err.message || 'Action failed');
    } finally {
      setLoadingCardId(null);
    }
  };

  if (!cards || cards.length === 0) {
    return <div className="card-empty-state">No active debit or credit cards issued yet.</div>;
  }

  return (
    <div className="card-management-grid">
      {cards.map((card) => (
        <div key={card.cardId} className="card-item-container">
          <HorizonCard
            cardNumber={card.maskedNumber}
            cardHolderName="ACCOUNT HOLDER"
            expiryDate={card.expiryDate}
            cardType={card.cardType}
            brand="VISA"
          />
          <div className="card-controls">
            <div className="card-status-row">
              <span className="card-status-label">Status:</span>
              <StatusBadge status={card.status} />
            </div>
            <div className="card-limits-preview">
              <div>Daily POS Limit: <strong>₹{card.dailyPosLimit?.toLocaleString() || '50,000'}</strong></div>
              <div>Daily ATM Limit: <strong>₹{card.dailyAtmLimit?.toLocaleString() || '25,000'}</strong></div>
            </div>
            <Button
              variant={card.status === 'BLOCKED' ? 'primary' : 'secondary'}
              disabled={loadingCardId === card.cardId}
              onClick={() => handleToggleBlock(card)}
            >
              {loadingCardId === card.cardId
                ? 'Updating...'
                : card.status === 'BLOCKED'
                ? 'Unblock Card'
                : 'Block Card'}
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
}
