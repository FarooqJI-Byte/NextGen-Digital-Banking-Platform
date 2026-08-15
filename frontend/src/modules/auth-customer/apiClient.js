const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const TOKEN_KEY = 'nextgen_auth_token';

let onUnauthorizedCallback = null;

export function getAuthToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch (e) {
    return null;
  }
}

export function setAuthToken(token) {
  try {
    if (token) {
      sessionStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
    }
  } catch (e) {
    // SessionStorage unavailable or restricted
  }
}

export function clearAuthToken() {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch (e) {
    // Ignore
  }
}

export function registerUnauthorizedHandler(callback) {
  onUnauthorizedCallback = callback;
}

export async function request(endpoint, options = {}) {
  const url = endpoint.startsWith('http')
    ? endpoint
    : `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const isFormData = options.body instanceof FormData;

  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {}),
  };

  const token = getAuthToken();
  if (token && !options.skipAuth) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  if (!isFormData && config.body && typeof config.body === 'object') {
    config.body = JSON.stringify(config.body);
  }

  let response;
  try {
    response = await fetch(url, config);
  } catch (networkErr) {
    const error = new Error('Unable to connect to the banking server. Please check your network connection.');
    error.isNetworkError = true;
    throw error;
  }

  if (response.status === 401 && !options.skipAuth) {
    if (typeof onUnauthorizedCallback === 'function') {
      onUnauthorizedCallback();
    }
  }

  let data = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }
  } else {
    try {
      data = await response.text();
    } catch (e) {
      data = null;
    }
  }

  if (!response.ok) {
    let errorMessage = 'An unexpected banking service error occurred.';
    let errorCode = 'API_ERROR';
    let fieldErrors = [];

    if (data && typeof data === 'object') {
      errorMessage = data.message || data.error || errorMessage;
      errorCode = data.errorCode || errorCode;
      fieldErrors = data.errors || [];
    } else if (typeof data === 'string' && data.trim().length > 0) {
      errorMessage = data;
    }

    const error = new Error(errorMessage);
    error.status = response.status;
    error.statusCode = response.status;
    error.errorCode = errorCode;
    error.fieldErrors = fieldErrors;
    error.response = data;
    throw error;
  }

  return data;
}

export const apiClient = {
  get: (endpoint, options = {}) => request(endpoint, { ...options, method: 'GET' }),
  post: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'POST', body }),
  postFormData: (endpoint, formData, options = {}) => request(endpoint, { ...options, method: 'POST', body: formData }),
  put: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'PUT', body }),
  delete: (endpoint, options = {}) => request(endpoint, { ...options, method: 'DELETE' }),
};

export default apiClient;
