import { apiClient } from './apiClient.js';

/**
 * Provisions a new BANK_STAFF employee account without password (Admin only).
 * Generates an activation token.
 * @param {Object} payload
 * @param {string} payload.username
 * @param {string} payload.email
 * @returns {Promise<{userId: string, username: string, email: string, role: string, isActive: boolean, createdAt: string}>}
 */
export async function createStaff({ username, email }) {
  return apiClient.post('/admin/staff', {
    username: username.trim(),
    email: email.trim().toLowerCase(),
  });
}

/**
 * Resends activation link by generating a fresh activation token for an unactivated staff member (Admin only).
 * @param {string} userId
 * @returns {Promise<{userId: string, username: string, email: string, role: string, isActive: boolean, createdAt: string}>}
 */
export async function resendStaffActivation(userId) {
  return apiClient.post(`/admin/staff/${userId}/resend-activation`);
}

/**
 * Retrieves list of active BANK_STAFF employees (Admin only).
 * @returns {Promise<Array<{userId: string, username: string, email: string, role: string, isActive: boolean, createdAt: string}>>}
 */
export async function listStaff() {
  return apiClient.get('/admin/staff');
}

export const adminApi = {
  createStaff,
  resendStaffActivation,
  listStaff,
};

export default adminApi;
