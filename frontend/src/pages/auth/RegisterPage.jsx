import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import axiosClient from '../../api/axiosClient';

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
    <div style={{ maxWidth: 400, margin: '4rem auto', padding: '2rem' }}>
      <h1>Create Account</h1>
      <form onSubmit={handleSubmit}>
        <div><label>Name</label><input name="name" value={form.name} onChange={handleChange} required /></div>
        <div><label>Email</label><input name="email" type="email" value={form.email} onChange={handleChange} required /></div>
        <div><label>Phone Number</label><input name="phoneNumber" value={form.phoneNumber} onChange={handleChange} required /></div>
        <div><label>Password</label><input name="password" type="password" value={form.password} onChange={handleChange} required minLength={8} /></div>
        <div><label>Transaction PIN (4 digits)</label><input name="transactionPin" value={form.transactionPin} onChange={handleChange} required maxLength={4} pattern="\d{4}" /></div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit" disabled={loading}>{loading ? 'Creating account...' : 'Register'}</button>
      </form>
      <p>Already have an account? <Link to="/login">Sign in</Link></p>
    </div>
  );
}
