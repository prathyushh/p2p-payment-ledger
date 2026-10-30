import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import * as walletApi from '../../api/walletApi';
import * as transactionApi from '../../api/transactionApi';
import * as bankAccountApi from '../../api/bankAccountApi';
import './UserPages.css';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [bankAccounts, setBankAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [topUpForm, setTopUpForm] = useState({ accountId: '', amount: '' });
  const [topUpLoading, setTopUpLoading] = useState(false);
  const [topUpError, setTopUpError] = useState('');
  const [topUpSuccess, setTopUpSuccess] = useState('');

  // Fetch wallet and transaction data
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError('');
      try {
        const [walletRes, transRes, bankRes] = await Promise.all([
          walletApi.getWallet(),
          transactionApi.getMiniStatement(),
          bankAccountApi.getBankAccounts(),
        ]);
        setWallet(walletRes.data);
        setRecentTransactions(transRes.data || []);
        setBankAccounts(bankRes.data || []);
      } catch (_err) {
        setError('Failed to load dashboard. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleTopUp = async (e) => {
    e.preventDefault();
    setTopUpError('');
    setTopUpSuccess('');

    if (!topUpForm.accountId) {
      setTopUpError('Please select a simulated account.');
      return;
    }
    if (!topUpForm.amount || parseFloat(topUpForm.amount) <= 0) {
      setTopUpError('Please enter a valid amount.');
      return;
    }

    setTopUpLoading(true);
    try {
      await walletApi.topUp({
        accountId: parseInt(topUpForm.accountId),
        amount: parseFloat(topUpForm.amount),
      });
      setTopUpSuccess(`Successfully topped up Rs ${topUpForm.amount}!`);
      setTopUpForm({ accountId: '', amount: '' });
      // Refresh wallet
      const updatedWallet = await walletApi.getWallet();
      setWallet(updatedWallet.data);
    } catch (err) {
      const message = err.response?.data?.message || 'Top-up failed. Please check your details.';
      setTopUpError(message);
    } finally {
      setTopUpLoading(false);
    }
  };

  const handleDeactivate = async () => {
    if (!confirm('Are you sure you want to deactivate your wallet?')) return;
    try {
      const res = await walletApi.deactivateWallet();
      setWallet(res.data);
    } catch (_err) {
      setError('Failed to deactivate wallet.');
    }
  };

  const handleReactivate = async () => {
    try {
      const res = await walletApi.reactivateWallet();
      setWallet(res.data);
    } catch (_err) {
      setError('Failed to reactivate wallet.');
    }
  };

  if (loading) {
    return (
      <div className="user-page">
        <div className="loading">Loading dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="user-page">
        <div className="error-banner">{error}</div>
      </div>
    );
  }

  const statusColor = {
    ACTIVE: '#4ade80',
    INACTIVE: '#ef4444',
    FROZEN: '#f97316',
  };

  // Filter active accounts for top-up
  const activeAccounts = bankAccounts.filter((acc) => acc.isActive);

  return (
    <div className="user-page">
      <div className="page-header">
        <h1>Welcome, {user?.name}!</h1>
        <p>Manage your wallet and track your transactions</p>
      </div>

      {/* Wallet Balance Card */}
      <div className="wallet-card">
        <div className="card-header">
          <h2>Your Wallet</h2>
          <span
            className="status-badge"
            style={{ backgroundColor: statusColor[wallet?.status] || '#6b6b6b' }}
          >
            {wallet?.status}
          </span>
        </div>

        <div className="balance-display">
          <div className="balance-amount">Rs {wallet?.balance?.toFixed(2)}</div>
          <div className="balance-label">Available Balance</div>
        </div>

        <div className="wallet-stats">
          <div className="stat">
            <div className="stat-label">Daily Limit</div>
            <div className="stat-value">Rs {wallet?.dailyLimit?.toFixed(2)}</div>
          </div>
          <div className="stat">
            <div className="stat-label">Spent Today</div>
            <div className="stat-value">Rs {wallet?.dailySpent?.toFixed(2)}</div>
          </div>
          <div className="stat">
            <div className="stat-label">Remaining Today</div>
            <div className="stat-value">Rs {wallet?.dailyRemaining?.toFixed(2)}</div>
          </div>
        </div>

        {/* Daily Spending Progress Bar */}
        <div className="spending-progress">
          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{
                width: `${Math.min(
                  (wallet?.dailySpent / wallet?.dailyLimit) * 100,
                  100
                )}%`,
              }}
            />
          </div>
          <div className="progress-label">
            {Math.round((wallet?.dailySpent / wallet?.dailyLimit) * 100)}% of daily limit used
          </div>
        </div>

        {/* Wallet Actions */}
        <div className="wallet-actions">
          {wallet?.status === 'ACTIVE' && (
            <button onClick={handleDeactivate} className="btn btn-secondary">
              Deactivate Wallet
            </button>
          )}
          {wallet?.status === 'INACTIVE' && (
            <button onClick={handleReactivate} className="btn btn-primary">
              Reactivate Wallet
            </button>
          )}
          {wallet?.status === 'FROZEN' && (
            <p className="frozen-notice">Your wallet has been frozen by the admin.</p>
          )}
        </div>
      </div>

      {/* Top-Up Form */}
      <div className="topup-card">
        <h3>Top Up Your Wallet</h3>
        <form onSubmit={handleTopUp}>
          <div className="form-group">
            <label>Select Simulated Account</label>
            <select
              value={topUpForm.accountId}
              onChange={(e) =>
                setTopUpForm({ ...topUpForm, accountId: e.target.value })
              }
              required
            >
              <option value="">-- Choose account --</option>
              {activeAccounts.length > 0 ? (
                activeAccounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.bankName} ({acc.accountNickname}) - Rs {acc.simulatedBalance?.toFixed(2)}
                  </option>
                ))
              ) : (
                <option disabled>No active accounts available</option>
              )}
            </select>
            {activeAccounts.length === 0 && (
              <p style={{ fontSize: '0.85rem', color: '#666', marginTop: '0.5rem' }}>
                ?? Create a simulated account in <a href="/bank-accounts" style={{ color: '#0066cc' }}>Bank Accounts</a> to top up your wallet
              </p>
            )}
          </div>

          <div className="form-group">
            <label>Amount (Rs)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={topUpForm.amount}
              onChange={(e) =>
                setTopUpForm({ ...topUpForm, amount: e.target.value })
              }
              placeholder="Enter amount"
              required
            />
          </div>

          {topUpError && <div className="error-banner">{topUpError}</div>}
          {topUpSuccess && <div className="success-banner">{topUpSuccess}</div>}

          <button type="submit" disabled={topUpLoading || activeAccounts.length === 0} className="btn btn-primary">
            {topUpLoading ? 'Processing...' : 'Top Up Now'}
          </button>
        </form>
      </div>

      {/* Recent Transactions */}
      <div className="transactions-card">
        <h3>Recent Transactions (Last 10)</h3>
        {recentTransactions.length === 0 ? (
          <p className="empty-state">No transactions yet.</p>
        ) : (
          <div className="transactions-list">
            {recentTransactions.map((tx) => (
              <div key={tx.id} className="transaction-item">
                <div className="tx-info">
                  <div className="tx-type">{tx.type}</div>
                  <div className="tx-details">
                    {tx.type === 'P2P_TRANSFER' ? (
                      <>
                        <span className="tx-party">
                          {tx.senderEmail === user?.email ? 'To: ' : 'From: '}
                          {tx.senderEmail === user?.email ? tx.receiverEmail : tx.senderEmail}
                        </span>
                        <span className="tx-remark">{tx.remark}</span>
                      </>
                    ) : (
                      <span className="tx-party">{tx.remark || 'Wallet operation'}</span>
                    )}
                  </div>
                  <div className="tx-time">
                    {new Date(tx.initiatedAt).toLocaleDateString()} {new Date(tx.initiatedAt).toLocaleTimeString()}
                  </div>
                </div>
                <div className={`tx-amount ${tx.senderEmail === user?.email ? 'out' : 'in'}`}>
                  {tx.senderEmail === user?.email ? '-' : '+'}Rs {tx.amount?.toFixed(2)}
                </div>
                <div className={`tx-status status-${tx.status.toLowerCase()}`}>{tx.status}</div>
              </div>
            ))}
          </div>
        )}
        <button onClick={() => navigate('/transactions')} className="btn btn-secondary">
          View All Transactions ?
        </button>
      </div>
    </div>
  );
}
