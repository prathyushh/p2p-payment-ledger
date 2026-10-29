import { useState, useEffect } from 'react';
import * as autoReloadApi from '../../api/autoReloadApi';
import * as bankAccountApi from '../../api/bankAccountApi';
import './AutoReloadPage.css';

export default function AutoReloadPage() {
  const [settings, setSettings] = useState(null);
  const [bankAccounts, setBankAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Form state
  const [formData, setFormData] = useState({
    isEnabled: false,
    accountId: '',
    thresholdAmount: '',
    reloadAmount: '',
  });
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');

  // Fetch settings function
  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [settingsRes, accountsRes] = await Promise.all([
        autoReloadApi.getAutoReloadSettings(),
        bankAccountApi.getBankAccounts(),
      ]);
      setSettings(settingsRes.data);
      setFormData({
        isEnabled: settingsRes.data.isEnabled,
        accountId: settingsRes.data.accountId || '',
        thresholdAmount: settingsRes.data.thresholdAmount || '',
        reloadAmount: settingsRes.data.reloadAmount || '',
      });
      setBankAccounts(accountsRes.data || []);
    } catch (_err) {
      setError('Failed to load Auto Reload settings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Load settings and bank accounts on mount
  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');

    // Validation
    if (!formData.isEnabled) {
      // If disabling, no need for threshold/reload/account
      try {
        setFormLoading(true);
        const response = await autoReloadApi.updateAutoReloadSettings({
          isEnabled: false,
          accountId: null,
          thresholdAmount: null,
          reloadAmount: null,
        });
        setSettings(response.data);
        setFormSuccess('Auto Reload disabled successfully.');
      } catch (err) {
        const message = err.response?.data?.message || 'Failed to update Auto Reload settings.';
        setFormError(message);
      } finally {
        setFormLoading(false);
      }
      return;
    }

    // If enabling, validate required fields
    if (!formData.accountId) {
      setFormError('Please select a simulated account.');
      return;
    }
    if (!formData.thresholdAmount || parseFloat(formData.thresholdAmount) <= 0) {
      setFormError('Threshold amount must be greater than 0.');
      return;
    }
    if (!formData.reloadAmount || parseFloat(formData.reloadAmount) <= 0) {
      setFormError('Reload amount must be greater than 0.');
      return;
    }

    try {
      setFormLoading(true);
      const response = await autoReloadApi.updateAutoReloadSettings({
        isEnabled: true,
        accountId: parseInt(formData.accountId),
        thresholdAmount: parseFloat(formData.thresholdAmount),
        reloadAmount: parseFloat(formData.reloadAmount),
      });
      setSettings(response.data);
      setFormSuccess('Auto Reload configured successfully.');
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to update Auto Reload settings.';
      setFormError(message);
    } finally {
      setFormLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="auto-reload-page">
        <div className="loading">Loading Auto Reload settings...</div>
      </div>
    );
  }

  // Get selected account details
  const selectedAccount = formData.accountId
    ? bankAccounts.find((acc) => acc.id === parseInt(formData.accountId))
    : null;

  // Get active accounts for dropdown
  const activeAccounts = bankAccounts.filter((acc) => acc.isActive);

  return (
    <div className="auto-reload-page">
      <div className="page-header">
        <h1>Auto Reload Configuration</h1>
        <p>Automatically reload your wallet when balance falls below a threshold</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Status Card */}
      {settings && (
        <div className="status-card">
          <div className="status-header">
            <h3>Current Status</h3>
            <span
              className="status-badge"
              style={{
                backgroundColor: settings.isEnabled ? '#4ade80' : '#ef4444',
              }}
            >
              {settings.isEnabled ? 'Enabled' : 'Disabled'}
            </span>
          </div>

          <div className="status-details">
            <div className="detail-row">
              <span className="detail-label">Auto Reload:</span>
              <span className="detail-value">
                {settings.isEnabled ? 'Active' : 'Inactive'}
              </span>
            </div>

            {settings.isEnabled && (
              <>
                <div className="detail-row">
                  <span className="detail-label">Account:</span>
                  <span className="detail-value">
                    {settings.accountNickname || 'Not configured'}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Threshold Amount:</span>
                  <span className="detail-value">
                    Rs {settings.thresholdAmount?.toFixed(2)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Reload Amount:</span>
                  <span className="detail-value">
                    Rs {settings.reloadAmount?.toFixed(2)}
                  </span>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Configuration Form */}
      <div className="config-card">
        <h3>Configure Auto Reload</h3>

        <form onSubmit={handleSubmit}>
          {/* Enable/Disable Toggle */}
          <div className="form-group toggle-group">
            <label className="toggle-label">
              <input
                type="checkbox"
                checked={formData.isEnabled}
                onChange={(e) =>
                  setFormData({ ...formData, isEnabled: e.target.checked })
                }
              />
              <span className="toggle-switch" />
              <span className="toggle-text">
                {formData.isEnabled ? 'Auto Reload Enabled' : 'Auto Reload Disabled'}
              </span>
            </label>
            <p className="toggle-hint">
              {formData.isEnabled
                ? 'Your wallet will automatically reload when balance drops below the threshold'
                : 'Auto Reload is currently disabled. Enable to set up automatic wallet reloads'}
            </p>
          </div>

          {/* Settings (shown when enabled) */}
          {formData.isEnabled && (
            <div className="settings-section">
              <div className="form-group">
                <label>Select Source Account *</label>
                <select
                  value={formData.accountId}
                  onChange={(e) =>
                    setFormData({ ...formData, accountId: e.target.value })
                  }
                  required={formData.isEnabled}
                >
                  <option value="">-- Choose a simulated account --</option>
                  {activeAccounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.accountNickname} - Rs {acc.simulatedBalance?.toFixed(2)}
                    </option>
                  ))}
                </select>
                {activeAccounts.length === 0 && (
                  <p className="no-accounts-hint">
                    No active simulated accounts available. Create one in Bank Accounts to enable Auto Reload.
                  </p>
                )}
              </div>

              {selectedAccount && (
                <div className="account-preview">
                  <strong>Selected Account:</strong> {selectedAccount.accountNickname}
                  <br />
                  <strong>Available Balance:</strong> Rs {selectedAccount.simulatedBalance?.toFixed(2)}
                </div>
              )}

              <div className="form-group">
                <label>Threshold Amount (Rs) *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.thresholdAmount}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      thresholdAmount: e.target.value,
                    })
                  }
                  placeholder="e.g., 5000"
                  required={formData.isEnabled}
                />
                <p className="field-hint">
                  When wallet balance falls below this amount, Auto Reload will trigger
                </p>
              </div>

              <div className="form-group">
                <label>Reload Amount (Rs) *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.reloadAmount}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      reloadAmount: e.target.value,
                    })
                  }
                  placeholder="e.g., 10000"
                  required={formData.isEnabled}
                />
                <p className="field-hint">
                  This amount will be automatically transferred to your wallet when threshold is met
                </p>
              </div>

              {/* Preview */}
              <div className="config-preview">
                <strong>How it works:</strong>
                <ul>
                  <li>Your wallet will be monitored continuously</li>
                  <li>If balance drops below Rs {formData.thresholdAmount || '0'}, the reload is triggered</li>
                  <li>Rs {formData.reloadAmount || '0'} will be transferred from {selectedAccount?.accountNickname || 'your account'}</li>
                  <li>Reloads respect daily limits and account balance</li>
                  <li>Failed reloads will not be retried automatically</li>
                </ul>
              </div>
            </div>
          )}

          {/* Error and Success Messages */}
          {formError && <div className="error-banner">{formError}</div>}
          {formSuccess && <div className="success-banner">{formSuccess}</div>}

          {/* Submit Button */}
          <button type="submit" disabled={formLoading} className="btn btn-primary">
            {formLoading
              ? 'Saving...'
              : formData.isEnabled
                ? 'Save Auto Reload Configuration'
                : 'Disable Auto Reload'}
          </button>
        </form>
      </div>

      {/* Info Section */}
      <div className="info-card">
        <h3>About Auto Reload</h3>
        <div className="info-section">
          <h4>What is Auto Reload?</h4>
          <p>
            Auto Reload automatically transfers simulated funds from your selected bank account to your wallet
            when the wallet balance falls below a configured threshold. This helps ensure you always have funds
            available for transactions.
          </p>
        </div>

        <div className="info-section">
          <h4>Important Limitations</h4>
          <ul>
            <li>Auto Reload respects your daily spending limit</li>
            <li>If the source account doesn&apos;t have sufficient funds, the reload will fail</li>
            <li>Failed reloads are not retried; you&apos;ll need to manually top up</li>
            <li>Only simulated funds are used; no real banking integration</li>
            <li>You can modify settings at any time</li>
          </ul>
        </div>

        <div className="info-section">
          <h4>Example Scenario</h4>
          <p>
            You set a threshold of <strong>Rs 5,000</strong> and reload amount of <strong>Rs 10,000</strong>.
            When your wallet balance drops to Rs 4,999, Auto Reload automatically transfers Rs 10,000 from your
            selected account to your wallet.
          </p>
        </div>
      </div>
    </div>
  );
}