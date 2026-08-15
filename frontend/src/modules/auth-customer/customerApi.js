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
 * Uploads an actual customer KYC document file (Multipart/Form-Data).
 * @param {Object} params
 * @param {string} params.documentType - 'PAN' | 'AADHAAR' | 'PASSPORT' | 'VOTER_ID'
 * @param {string} params.documentNumber
 * @param {File} params.file
 * @returns {Promise<Object>} KYCSubmissionResponseDto (202 Accepted)
 */
export async function uploadKycDocument({ documentType, documentNumber, file }) {
  const formData = new FormData();
  formData.append('documentType', documentType);
  formData.append('documentNumber', documentNumber);
  formData.append('file', file);

  return apiClient.postFormData('/customers/kyc', formData);
}

/**
 * Submits a customer KYC identity document via JSON reference.
 * @param {Object} payload - KYCSubmissionRequestDto
 * @returns {Promise<Object>} KYCSubmissionResponseDto (202 Accepted)
 */
export async function submitKyc(payload) {
  return apiClient.post('/customers/kyc', payload);
}

/**
 * Staff retrieves the queue of pending KYC submissions.
 * @returns {Promise<Array<Object>>} List of PendingKycItemDto
 */
export async function getPendingKycQueue() {
  return apiClient.get('/staff/kyc/pending');
}

/**
 * Staff retrieves full KYC inspection details for a specific customer.
 * @param {string} customerId - UUID
 * @returns {Promise<Object>} StaffCustomerKycDetailDto
 */
export async function getStaffKycDetail(customerId) {
  return apiClient.get(`/staff/kyc/${customerId}`);
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
  uploadKycDocument,
  submitKyc,
  getPendingKycQueue,
  getStaffKycDetail,
  verifyKyc,
};

export default customerApi;
