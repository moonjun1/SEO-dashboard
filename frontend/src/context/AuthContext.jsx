import { createContext, useContext, useState, useEffect } from 'react';
import { getToken, setToken as saveToken, removeToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({ id: payload.user_id, email: payload.email });
      } catch {
        removeToken();
      }
    }
    setLoading(false);
  }, []);

  const login = (token) => {
    saveToken(token);
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      setUser({ id: payload.user_id, email: payload.email });
    } catch {
      setUser({ email: 'user' });
    }
  };

  const logout = () => {
    removeToken();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
