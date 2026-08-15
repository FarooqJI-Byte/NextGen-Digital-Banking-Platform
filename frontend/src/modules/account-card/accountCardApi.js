const API_BASE = '/api/v1';

export async function fetchCustomerAccounts(customerId) {
  const res = await fetch(`${API_BASE}/accounts?customerId=${customerId}`);
  if (!res.ok) throw new Error('Failed to fetch accounts');
  return res.json();
}

export async function openAccount(payload) {
  const res = await fetch(`${API_BASE}/accounts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) throw new Error('Failed to open account');
  return res.json();
}

export async function freezeAccount(accountId, reason) {
  const res = await fetch(`${API_BASE}/accounts/${accountId}/freeze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ freezeReason: reason })
  });
  if (!res.ok) throw new Error('Failed to freeze account');
  return res.json();
}

export async function unfreezeAccount(accountId) {
  const res = await fetch(`${API_BASE}/accounts/${accountId}/unfreeze`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Failed to unfreeze account');
  return res.json();
}

export async function fetchCustomerCards(customerId) {
  const res = await fetch(`${API_BASE}/cards?customerId=${customerId}`);
  if (!res.ok) throw new Error('Failed to fetch cards');
  return res.json();
}

export async function issueCard(payload) {
  const res = await fetch(`${API_BASE}/cards`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) throw new Error('Failed to issue card');
  return res.json();
}

export async function blockCard(cardId, reason) {
  const res = await fetch(`${API_BASE}/cards/${cardId}/block`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason })
  });
  if (!res.ok) throw new Error('Failed to block card');
  return res.json();
}

export async function unblockCard(cardId) {
  const res = await fetch(`${API_BASE}/cards/${cardId}/unblock`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error('Failed to unblock card');
  return res.json();
}
