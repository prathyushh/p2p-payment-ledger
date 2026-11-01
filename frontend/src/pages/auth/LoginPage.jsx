import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import axiosClient from '../../api/axiosClient';
import './AuthPages.css';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [lockedUntil, setLockedUntil] = useState(null);
  const [remainingTime, setRemainingTime] = useState(null);

  // Countdown timer
  useEffect(() => {
    if (!lockedUntil) {
      setRemainingTime(null);
      return;
    }

    const updateTimer = () => {
      const diff = new Date(lockedUntil) - new Date();
      if (diff <= 0) {
        setLockedUntil(null);
        setRemainingTime(null);
        setError('');
        return;
      }
      const minutes = Math.floor(diff / 60000);
      const seconds = Math.floor((diff % 60000) / 1000);
      setRemainingTime(`${minutes}m ${seconds}s`);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [lockedUntil]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await axiosClient.post('/auth/login', form);
      const user = {
        id: res.data.id,
        name: res.data.name,
        email: res.data.email,
        role: res.data.role,
      };
      login(res.data.token, user);
      navigate(user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard');
    } catch (err) {
      const status = err.response?.status;
      const message = err.response?.data?.message || err.response?.data?.error || err.message;

      if (status === 423) {
        // Account locked - extract timestamp and show countdown
        const match = message?.match(/locked until (.+)/i);
        if (match && match[1]) {
          setLockedUntil(match[1].trim());
        }
        setError('Account locked due to too many failed attempts.');
      } else if (status === 401) {
        // Wrong password - plain invalid credentials message
        setLockedUntil(null);
        setError('Invalid email or password.');
      } else if (status === 403) {
        setLockedUntil(null);
        setError('Account is disabled. Please contact support.');
      } else {
        setLockedUntil(null);
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Sign In</h1>
          <p>Welcome back to P2P Payment Ledger</p>
        </div>

        {error && (
          <div className={`auth-message ${lockedUntil ? 'locked' : 'error'}`}>
            <div className="error-text">{error}</div>
            {lockedUntil && remainingTime && (
              <div className="countdown-timer">
                <strong>Time remaining:</strong> {remainingTime}
              </div>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-field">
            <label htmlFor="email">Email Address</label>
            <input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              placeholder="Enter your email"
              required
              disabled={loading || !!lockedUntil}
            />
          </div>

          <div className="form-field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
              disabled={loading || !!lockedUntil}
            />
          </div>

          <button
            type="submit"
            className="auth-submit-btn"
            disabled={loading || !!lockedUntil}
          >
            {lockedUntil ? 'Account Locked' : loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Don&apos;t have an account?{' '}
            <Link to="/register">Create one</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
