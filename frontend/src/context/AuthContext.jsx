import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user');
      // Only parse if stored data exists and is not the literal string "undefined"
      if (stored && stored !== 'undefined') {
        return JSON.parse(stored);
      }
      return null;
    } catch (e) {
      // If parsing fails, clear the invalid data and return null
      localStorage.removeItem('user');
      return null;
    }
  });

  const login = useCallback((token, userData) => {
    // Only store if userData is a valid object
    if (userData) {
      localStorage.setItem('jwt_token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      setUser(userData);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  const isAdmin = user?.role === 'ADMIN';
  const isAuthenticated = !!user;

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
