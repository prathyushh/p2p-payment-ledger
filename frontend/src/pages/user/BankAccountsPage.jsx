import { useState, useEffect } from 'react';
import * as bankAccountApi from '../../api/bankAccountApi';
import './BankAccountsPage.css';

export default function BankAccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Create account form state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm] = useState({ bankName: '', accountNickname: '' });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');

  // Demo funds form state
  const [demoFundsForm, setDemoFundsForm] = useState({});
  const [demoFundsLoading, setDemoFundsLoading] = useState({});

  // Fetch accounts function
  const fetchAccounts = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await bankAccountApi.getBankAccounts();
      setAccounts(response.data || []);
    } catch (_err) {
      setError('Failed to load bank accounts. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Load accounts on mount
  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleCreateAccount = async (e) => {
    e.preventDefault();
    setCreateError('');

    if (!createForm.bankName.trim()) {
      setCreateError('Bank name is required.');
      return;
    }
    if (!createForm.accountNickname.trim()) {
      setCreateError('Account nickname is required.');
      return;
    }

    setCreateLoading(true);
    try {
      await bankAccountApi.createBankAccount(createForm);
      setCreateForm({ bankName: '', accountNickname: '' });
      setShowCreateForm(false);
      await fetchAccounts();
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to create account. Please try again.';
      setCreateError(message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleAddDemoFunds = async (accountId) => {
    const amount = demoFundsForm[accountId];
    if (!amount || parseFloat(amount) <= 0) {
      alert('Please enter a valid amount greater than 0.');
      return;
    }

    setDemoFundsLoading({ ...demoFundsLoading, [accountId]: true });
    try {
      await bankAccountApi.addDemoFunds(accountId, { amount: parseFloat(amount) });
      setDemoFundsForm({ ...demoFundsForm, [accountId]: '' });
      await fetchAccounts();
    } catch (_err) {
      alert('Failed to add demo funds.');
    } finally {
      setDemoFundsLoading({ ...demoFundsLoading, [accountId]: false });
    }
  };

  const handleDeactivateAccount = async (accountId) => {
    if (!confirm('Are you sure you want to deactivate this account? You will no longer be able to top up your wallet from it.')) {
      return;
    }

    try {
      await bankAccountApi.deactivateBankAccount(accountId);
      await fetchAccounts();
    } catch (_err) {
      alert('Failed to deactivate account. Please try again.');
    }
  };

  if (loading) {
    return (
      <div className="bank-accounts-page">
        <div className="loading">Loading bank accounts...</div>
      </div>
    );
  }

  return (
    <div className="bank-accounts-page">
      <div className="page-header">
        <h1>Simulated Bank Accounts</h1>
        <p>Manage your simulated bank accounts and add demo funds</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Create Account Button */}
      <div className="create-account-section">
        {!showCreateForm ? (
          <button onClick={() => setShowCreateForm(true)} className="btn btn-primary">
            + Create New Account
          </button>
        ) : (
          <div className="create-form-card">
            <div className="form-header">
              <h3>Create Simulated Bank Account</h3>
              <button
                onClick={() => setShowCreateForm(false)}
                className="close-btn"
                aria-label="Close form"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateAccount}>
              <div className="form-group">
                <label>Bank Name</label>
                <input
                  type="text"
                  value={createForm.bankName}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, bankName: e.target.value })
                  }
                  placeholder="e.g., State Bank of India"
                  required
                />
              </div>

              <div className="form-group">
                <label>Account Nickname</label>
                <input
                  type="text"
                  value={createForm.accountNickname}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, accountNickname: e.target.value })
                  }
                  placeholder="e.g., My SBI Savings"
                  required
                />
              </div>

              {createError && <div className="error-banner">{createError}</div>}

              <div className="form-actions">
                <button type="submit" disabled={createLoading} className="btn btn-primary">
                  {createLoading ? 'Creating...' : 'Create Account'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateForm(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}
      </div>

      {/* Accounts List */}
      <div className="accounts-section">
        {accounts.length === 0 ? (
          <div className="empty-state">
            <p>No bank accounts yet. Create one to get started!</p>
          </div>
        ) : (
          <div className="accounts-grid">
            {accounts.map((account) => (
              <div key={account.id} className="account-card">
                <div className="card-header">
                  <div className="header-left">
                    <h3>{account.bankName}</h3>
                    <p className="account-nickname">{account.accountNickname}</p>
                  </div>
                  <span
                    className="status-badge"
                    style={{
                      backgroundColor: account.isActive ? '#4ade80' : '#ef4444',
                    }}
                  >
                    {account.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>

                <div className="card-body">
                  <div className="account-detail">
                    <span className="detail-label">Account ID:</span>
                    <span className="detail-value">{account.accountIdentifier}</span>
                  </div>

                  <div className="balance-section">
                    <div className="balance-label">Demo Balance</div>
                    <div className="balance-amount">Rs {account.simulatedBalance?.toFixed(2)}</div>
                  </div>

                  <div className="account-detail">
                    <span className="detail-label">Created:</span>
                    <span className="detail-value">
                      {new Date(account.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                {/* Demo Funds Section */}
                {account.isActive && (
                  <div className="demo-funds-section">
                    <label>Add Demo Funds</label>
                    <div className="demo-funds-form">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="Amount (Rs)"
                        value={demoFundsForm[account.id] || ''}
                        onChange={(e) =>
                          setDemoFundsForm({
                            ...demoFundsForm,
                            [account.id]: e.target.value,
                          })
                        }
                      />
                      <button
                        onClick={() => handleAddDemoFunds(account.id)}
                        disabled={demoFundsLoading[account.id]}
                        className="btn btn-secondary"
                      >
                        {demoFundsLoading[account.id] ? 'Adding...' : 'Add'}
                      </button>
                    </div>
                    <p className="demo-notice">
                      💡 This adds simulated demo funds for testing purposes only
                    </p>
                  </div>
                )}

                {/* Deactivate Button */}
                {account.isActive && (
                  <button
                    onClick={() => handleDeactivateAccount(account.id)}
                    className="btn btn-secondary full-width"
                  >
                    Deactivate Account
                  </button>
                )}

                {!account.isActive && (
                  <p className="inactive-notice">This account is inactive and cannot be used for transactions.</p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Info Section */}
      <div className="info-card">
        <h3>About Simulated Bank Accounts</h3>
        <ul>
          <li><strong>Demo Funds:</strong> Add simulated demo funds to test wallet top-up functionality</li>
          <li><strong>Multiple Accounts:</strong> Create multiple bank accounts to simulate real banking scenarios</li>
          <li><strong>Top-Up Source:</strong> Use active accounts as source for wallet top-ups</li>
          <li><strong>Deactivation:</strong> Deactivate accounts you no longer need; they cannot be used for transactions</li>
          <li><strong>Testing Only:</strong> This is a demo system with simulated funds — not a real banking service</li>
        </ul>
      </div>
    </div>
  );
}