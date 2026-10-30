import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import axiosClient from '../../api/axiosClient';
import './AuthPages.css';

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '', email: '', phoneNumber: '', password: '', transactionPin: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    // Validate password is not empty
    if (!form.password || form.password.trim() === '') {
      setError('Password is required.');
      return;
    }
    
    // Validate password length
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    
    // Validate transaction PIN
    if (!form.transactionPin || form.transactionPin.trim() === '') {
      setError('Transaction PIN is required.');
      return;
    }
    
    if (!/^\d{4}$/.test(form.transactionPin)) {
      setError('Transaction PIN must be exactly 4 digits.');
      return;
    }
    
    setLoading(true);
    try {
      const res = await axiosClient.post('/auth/register', form);
      // Construct user object from flat response
      const user = {
        id: res.data.id,
        name: res.data.name,
        email: res.data.email,
        role: res.data.role,
      };
      login(res.data.token, user);
      navigate('/dashboard');
    } catch (err) {
      const status = err.response?.status;
      if (status === 409) {
        setError('Email or phone number is already registered.');
      } else {
        setError('Registration failed. Please check your details.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Create Account</h1>
          <p>Join P2P Payment Ledger today</p>
        </div>

        {error && <div className="auth-message error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-field">
            <label htmlFor="name">Full Name</label>
            <input
              id="name"
              name="name"
              type="text"
              value={form.name}
              onChange={handleChange}
              placeholder="Enter your full name"
              required
              disabled={loading}
            />
          </div>

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
              disabled={loading}
            />
          </div>

          <div className="form-field">
            <label htmlFor="phoneNumber">Phone Number</label>
            <input
              id="phoneNumber"
              name="phoneNumber"
              type="tel"
              value={form.phoneNumber}
              onChange={handleChange}
              placeholder="Enter your phone number"
              required
              disabled={loading}
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
              placeholder="At least 8 characters"
              required
              minLength={8}
              disabled={loading}
            />
            <div className="form-helper">Minimum 8 characters</div>
          </div>

          <div className="form-field">
            <label htmlFor="transactionPin">Transaction PIN</label>
            <input
              id="transactionPin"
              name="transactionPin"
              type="password"
              value={form.transactionPin}
              onChange={handleChange}
              placeholder="Enter 4 digits"
              required
              maxLength={4}
              pattern="\d{4}"
              disabled={loading}
            />
            <div className="form-helper">Exactly 4 digits (0-9)</div>
          </div>

          <button 
            type="submit" 
            className="auth-submit-btn"
            disabled={loading}
          >
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
