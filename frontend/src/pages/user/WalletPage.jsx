import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as walletApi from '../../api/walletApi';
import './UserPages.css';

export default function WalletPage() {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    const fetchWallet = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await walletApi.getWallet();
        setWallet(response.data);
      } catch (err) {
        setError('Failed to load wallet. Please try again.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchWallet();
  }, []);

  const handleDeactivate = async () => {
    if (!confirm('Are you sure you want to deactivate your wallet? You will not be able to send or receive money.')) {
      return;
    }
    setActionLoading(true);
    setError('');
    try {
      await walletApi.deactivateWallet();
      const updatedWallet = await walletApi.getWallet();
      setWallet(updatedWallet.data);
    } catch (err) {
      setError('Failed to deactivate wallet. Please try again.');
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReactivate = async () => {
    setActionLoading(true);
    setError('');
    try {
      await walletApi.reactivateWallet();
      const updatedWallet = await walletApi.getWallet();
      setWallet(updatedWallet.data);
    } catch (err) {
      setError('Failed to reactivate wallet. Please try again.');
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="user-page">
        <div className="loading">Loading wallet...</div>
      </div>
    );
  }

  if (error && !wallet) {
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

  return (
    <div className="user-page">
      <div className="page-header">
        <h1>Wallet Management</h1>
        <p>View and manage your wallet status and settings</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Wallet Status Card */}
      <div className="wallet-card full-width">
        <div className="card-header">
          <h2>Wallet Details</h2>
          <span
            className="status-badge"
            style={{ backgroundColor: statusColor[wallet?.status] || '#6b6b6b' }}
          >
            {wallet?.status}
          </span>
        </div>

        <div className="wallet-details-grid">
          <div className="detail-item">
            <div className="detail-label">Current Balance</div>
            <div className="detail-value">Rs {wallet?.balance?.toFixed(2)}</div>
          </div>
          <div className="detail-item">
            <div className="detail-label">Daily Limit</div>
            <div className="detail-value">Rs {wallet?.dailyLimit?.toFixed(2)}</div>
          </div>
          <div className="detail-item">
            <div className="detail-label">Amount Spent Today</div>
            <div className="detail-value">Rs {wallet?.dailySpent?.toFixed(2)}</div>
          </div>
          <div className="detail-item">
            <div className="detail-label">Daily Remaining</div>
            <div className="detail-value">Rs {wallet?.dailyRemaining?.toFixed(2)}</div>
          </div>
        </div>

        {/* Spending Progress */}
        <div className="spending-section">
          <h3>Daily Spending Progress</h3>
          <div className="progress-bar large">
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
          <div className="progress-stats">
            <span>{Math.round((wallet?.dailySpent / wallet?.dailyLimit) * 100)}% used</span>
            <span>{Math.round((wallet?.dailyRemaining / wallet?.dailyLimit) * 100)}% remaining</span>
          </div>
        </div>

        {/* Status Information */}
        <div className="status-info">
          {wallet?.status === 'ACTIVE' && (
            <div className="info-box success">
              <strong>Active Wallet</strong>
              <p>Your wallet is active. You can send and receive money.</p>
            </div>
          )}
          {wallet?.status === 'INACTIVE' && (
            <div className="info-box warning">
              <strong>Inactive Wallet</strong>
              <p>Your wallet is currently inactive. You cannot send or receive money until you reactivate it.</p>
            </div>
          )}
          {wallet?.status === 'FROZEN' && (
            <div className="info-box error">
              <strong>Frozen Wallet</strong>
              <p>Your wallet has been frozen by the admin. Contact support for more information.</p>
            </div>
          )}
        </div>

        {/* Wallet Actions */}
        <div className="wallet-actions full-width">
          {wallet?.status === 'ACTIVE' && (
            <button
              onClick={handleDeactivate}
              disabled={actionLoading}
              className="btn btn-secondary"
            >
              {actionLoading ? 'Processing...' : 'Deactivate Wallet'}
            </button>
          )}
          {wallet?.status === 'INACTIVE' && (
            <button
              onClick={handleReactivate}
              disabled={actionLoading}
              className="btn btn-primary"
            >
              {actionLoading ? 'Processing...' : 'Reactivate Wallet'}
            </button>
          )}
          {wallet?.status === 'FROZEN' && (
            <p className="frozen-notice">
              Your wallet is frozen and cannot be modified. Please contact support.
            </p>
          )}
          <button
            onClick={() => navigate('/dashboard')}
            className="btn btn-outline"
          >
            Back to Dashboard
          </button>
        </div>
      </div>

      {/* Info Section */}
      <div className="info-card">
        <h3>About Your Wallet</h3>
        <ul>
          <li><strong>Daily Limit:</strong> You can spend up to Rs {wallet?.dailyLimit?.toFixed(2)} per day</li>
          <li><strong>Current Status:</strong> Your wallet is currently {wallet?.status.toLowerCase()}</li>
          <li><strong>Deactivation:</strong> You can temporarily deactivate your wallet to prevent transactions</li>
          <li><strong>Reactivation:</strong> Reactivate your wallet anytime from this page</li>
        </ul>
      </div>
    </div>
  );
}