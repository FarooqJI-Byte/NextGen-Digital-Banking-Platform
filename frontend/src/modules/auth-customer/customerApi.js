import { apiClient } from './apiClient.js';

/**
 * Fetches the authenticated customer's profile, addresses, and nominees.
 * @returns {Promise<Object>} CustomerProfileResponseDto
 */
export async function getCustomerProfile() {
  return apiClient.get('/customers/profile');
}

/**
 * Creates a new customer profile with personal info, addresses, and nominees.
 * @param {Object} payload - CreateCustomerProfileRequestDto
 * @returns {Promise<Object>} CustomerProfileResponseDto
 */
export async function createCustomerProfile(payload) {
  return apiClient.post('/customers/profile', payload);
}

/**
 * Submits a customer KYC identity document (PAN, AADHAAR, PASSPORT, VOTER_ID).
 * @param {Object} payload - KYCSubmissionRequestDto
 * @param {string} payload.documentType
 * @param {string} payload.documentNumber
 * @param {string} payload.fileReference
 * @returns {Promise<Object>} KYCSubmissionResponseDto (202 Accepted)
 */
export async function submitKyc(payload) {
  return apiClient.post('/customers/kyc', payload);
}

/**
 * Staff verification/rejection of a customer KYC submission.
 * @param {Object} payload - KYCVerificationRequestDto
 * @param {string} payload.customerId
 * @param {string} payload.status - 'VERIFIED' | 'REJECTED'
 * @param {string} [payload.remarks]
 * @returns {Promise<Object>} KYCVerificationResponseDto
 */
export async function verifyKyc(payload) {
  return apiClient.post('/staff/kyc/verify', payload);
}

export const customerApi = {
  getCustomerProfile,
  createCustomerProfile,
  submitKyc,
  verifyKyc,
};

export default customerApi;
