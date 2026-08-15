import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from './authApi.js';
import { getAuthToken, setAuthToken, clearAuthToken, registerUnauthorizedHandler } from './apiClient.js';

const AuthContext = createContext(null);

function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

function isTokenExpired(payload) {
  if (!payload || !payload.exp) return true;
  const nowInSeconds = Math.floor(Date.now() / 1000);
  return payload.exp <= nowInSeconds;
}

export const AuthProvider = ({ children }) => {
  const [accessToken, setAccessTokenState] = useState(null);
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    clearAuthToken();
    setAccessTokenState(null);
    setUser(null);
    setRole(null);
  }, []);

  const initializeAuth = useCallback(() => {
    const storedToken = getAuthToken();
    if (storedToken) {
      const payload = parseJwtPayload(storedToken);
      if (payload && !isTokenExpired(payload)) {
        setAccessTokenState(storedToken);
        setUser({
          username: payload.sub,
          userId: payload.userId,
          role: payload.role,
        });
        setRole(payload.role || 'CUSTOMER');
      } else {
        clearAuthToken();
      }
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    initializeAuth();
    registerUnauthorizedHandler(() => {
      logout();
    });
  }, [initializeAuth, logout]);

  const login = async (username, password, portal = 'CUSTOMER') => {
    const response = await authApi.login(username, password, portal);
    const token = response.accessToken;
    setAuthToken(token);
    setAccessTokenState(token);

    const payload = parseJwtPayload(token);
    if (payload) {
      setUser({
        username: payload.sub || username,
        userId: payload.userId,
        role: payload.role,
      });
      setRole(payload.role || 'CUSTOMER');
    } else {
      setUser({ username });
      setRole('CUSTOMER');
    }

    return response;
  };

  const register = async (userData) => {
    return authApi.register(userData);
  };

  const value = {
    accessToken,
    user,
    role,
    isAuthenticated: Boolean(accessToken && user),
    isLoading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
