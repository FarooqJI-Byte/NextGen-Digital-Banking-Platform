import { apiClient } from './apiClient.js';

export async function register(payload) {
  return apiClient.post('/auth/register', {
    username: payload.username.trim(),
    email: payload.email.trim(),
    password: payload.password,
    role: 'CUSTOMER',
  }, { skipAuth: true });
}

export async function generateOtp({ identifier, purpose }) {
  return apiClient.post('/auth/otp/generate', {
    identifier: identifier.trim(),
    purpose,
  }, { skipAuth: true });
}

export async function verifyOtp({ identifier, purpose, otp }) {
  return apiClient.post('/auth/otp/verify', {
    identifier: identifier.trim(),
    purpose,
    otp: otp.trim(),
  }, { skipAuth: true });
}

export async function resendOtp({ identifier, purpose }) {
  return apiClient.post('/auth/otp/resend', {
    identifier: identifier.trim(),
    purpose,
  }, { skipAuth: true });
}

export async function customerLogin(username, password) {
  return apiClient.post(
    '/auth/customer/login',
    {
      username: username.trim(),
      password,
    },
    { skipAuth: true }
  );
}

export async function staffLogin(username, password) {
  return apiClient.post(
    '/auth/staff/login',
    {
      username: username.trim(),
      password,
    },
    { skipAuth: true }
  );
}

export async function activateStaff({ token, password, confirmPassword }) {
  return apiClient.post(
    '/auth/staff/activate',
    {
      token: token.trim(),
      password,
      confirmPassword,
    },
    { skipAuth: true }
  );
}

export async function login(username, password, portal = 'CUSTOMER') {
  if (portal === 'STAFF' || portal === 'ADMIN') {
    return staffLogin(username, password);
  }
  return customerLogin(username, password);
}

export const authApi = {
  register,
  generateOtp,
  verifyOtp,
  resendOtp,
  login,
  customerLogin,
  staffLogin,
  activateStaff,
};

export default authApi;
