import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import axiosClient from '../../api/axiosClient';

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '', email: '', phone_number: '', password: '', transaction_pin: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!/^\d{4}$/.test(form.transaction_pin)) {
      setError('Transaction PIN must be exactly 4 digits.');
      return;
    }
    setLoading(true);
    try {
      const res = await axiosClient.post('/auth/register', form);
      login(res.data.token, res.data.user);
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
        <div><label>Phone Number</label><input name="phone_number" value={form.phone_number} onChange={handleChange} required /></div>
        <div><label>Password</label><input name="password" type="password" value={form.password} onChange={handleChange} required minLength={8} /></div>
        <div><label>Transaction PIN (4 digits)</label><input name="transaction_pin" value={form.transaction_pin} onChange={handleChange} required maxLength={4} pattern="\d{4}" /></div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit" disabled={loading}>{loading ? 'Creating account...' : 'Register'}</button>
      </form>
      <p>Already have an account? <Link to="/login">Sign in</Link></p>
    </div>
  );
}