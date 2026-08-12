import { apiClient } from './apiClient.js';

/**
 * Authenticates user credentials against the banking auth API.
 * @param {string} username - Account username
 * @param {string} password - Raw account password
 * @returns {Promise<{accessToken: string, refreshToken: string, tokenType: string, expiresIn: number}>}
 */
export async function login(username, password) {
  return apiClient.post(
    '/auth/login',
    {
      username: username.trim(),
      password,
    },
    { skipAuth: true }
  );
}

/**
 * Registers a new user account with default CUSTOMER role.
 * @param {Object} payload
 * @param {string} payload.username
 * @param {string} payload.email
 * @param {string} payload.password
 * @param {string} [payload.role='CUSTOMER']
 * @returns {Promise<{userId: string, username: string, email: string, role: string, createdAt: string}>}
 */
export async function register({ username, email, password, role = 'CUSTOMER' }) {
  return apiClient.post(
    '/auth/register',
    {
      username: username.trim(),
      email: email.trim().toLowerCase(),
      password,
      role,
    },
    { skipAuth: true }
  );
}

export const authApi = {
  login,
  register,
};

export default authApi;
