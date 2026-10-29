import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import * as transactionApi from '../../api/transactionApi';
import './UserPages.css';
import './SendMoneyPage.css';

export default function SendMoneyPage() {
  const navigate = useNavigate();
  
  // Form state
  const [formData, setFormData] = useState({
    recipientIdentifier: '',
    amount: '',
    remark: '',
    pin: '',
  });
  
  // Search and submission states
  const [selectedRecipient, setSelectedRecipient] = useState(null);
  const [showResults, setShowResults] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [transactionResult, setTransactionResult] = useState(null);
  
  const debounceTimer = useRef(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, []);

  const handleSearchChange = (e) => {
    const value = e.target.value;
    setFormData(prev => ({ ...prev, recipientIdentifier: value }));
    setSelectedRecipient(null);
    setError('');

    if (!value || value.trim().length < 2) {
      return;
    }

    // Debounce search
    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    debounceTimer.current = setTimeout(async () => {
      setLoading(true);
      try {
        // Backend will validate the recipient exists
        setShowResults(true);
        setLoading(false);
      } catch (err) {
        console.error('Search error:', err);
        setLoading(false);
      }
    }, 300);
  };

  const handleSelectRecipient = () => {
    setSelectedRecipient(formData.recipientIdentifier);
    setShowResults(false);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!selectedRecipient) {
      setError('Please select a recipient');
      return;
    }

    if (!formData.amount || parseFloat(formData.amount) <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    if (!formData.remark.trim()) {
      setError('Please enter a remark or purpose for the transfer');
      return;
    }

    if (!formData.pin) {
      setError('Please enter your PIN');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const response = await transactionApi.sendMoney({
        recipientIdentifier: formData.recipientIdentifier,
        amount: parseFloat(formData.amount),
        remark: formData.remark,
        pin: formData.pin
      });

      setSuccess(true);
      setTransactionResult(response.data);
      setFormData({ recipientIdentifier: '', amount: '', remark: '', pin: '' });
      setSelectedRecipient(null);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to send money';
      setError(errorMsg);
      console.error('Send money error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (success && transactionResult) {
    return (
      <div className="user-page send-money-page">
        <div className="page-header">
          <h1>Send Money</h1>
          <p>Transfer funds to another user</p>
        </div>

        <div className="success-card">
          <div className="success-icon">?</div>
          <h2>Transfer Initiated Successfully</h2>
          
          <div className="transaction-details">
            <div className="detail-row">
              <span className="label">Transaction ID:</span>
              <span className="value">{transactionResult.transactionId}</span>
            </div>
            <div className="detail-row">
              <span className="label">Amount:</span>
              <span className="value">Rs {parseFloat(transactionResult.amount).toFixed(2)}</span>
            </div>
            <div className="detail-row">
              <span className="label">Recipient:</span>
              <span className="value">{formData.recipientIdentifier}</span>
            </div>
            <div className="detail-row">
              <span className="label">Status:</span>
              <span className="value status">{transactionResult.status}</span>
            </div>
            <div className="detail-row">
              <span className="label">Settlement Due Date:</span>
              <span className="value">{new Date(transactionResult.settlementDueDate).toLocaleDateString()}</span>
            </div>
            <div className="detail-row">
              <span className="label">New Balance:</span>
              <span className="value">Rs {parseFloat(transactionResult.senderNewBalance).toFixed(2)}</span>
            </div>
          </div>

          <div className="info-message">
            <strong>Note:</strong> This transfer will settle in 3 business days via ACH. You will receive a notification once the settlement is complete.
          </div>

          <div className="success-actions">
            <button 
              className="btn btn-primary"
              onClick={() => {
                setSuccess(false);
                setTransactionResult(null);
              }}
            >
              Send Another Transfer
            </button>
            <button 
              className="btn btn-secondary"
              onClick={() => navigate('/transactions')}
            >
              View Transaction History
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="user-page send-money-page">
      <div className="page-header">
        <h1>Send Money</h1>
        <p>Transfer funds to another user</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="send-money-card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="recipient">Recipient (Email or Phone)</label>
            <div className="search-container">
              <input
                type="text"
                id="recipient"
                value={formData.recipientIdentifier}
                onChange={handleSearchChange}
                placeholder="Enter email or phone number"
                disabled={loading}
                autoComplete="off"
              />
              {loading && <div className="search-loading">Searching...</div>}
            </div>
            {showResults && !selectedRecipient && formData.recipientIdentifier && (
              <div className="recipient-suggestion">
                <div className="suggestion-item" onClick={handleSelectRecipient}>
                  <span className="recipient-name">{formData.recipientIdentifier}</span>
                  <button type="button" className="btn-small">Select</button>
                </div>
              </div>
            )}
            {selectedRecipient && (
              <div className="selected-recipient">
                {String.fromCharCode(10003)} {selectedRecipient}
              </div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="amount">Amount (Rs)</label>
            <input
              type="number"
              id="amount"
              name="amount"
              value={formData.amount}
              onChange={handleInputChange}
              placeholder="0.00"
              min="0.01"
              step="0.01"
              disabled={!selectedRecipient || loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="remark">Purpose / Remark</label>
            <input
              type="text"
              id="remark"
              name="remark"
              value={formData.remark}
              onChange={handleInputChange}
              placeholder="e.g., Payment for services, Loan repayment"
              disabled={!selectedRecipient || loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="pin">Transaction PIN</label>
            <input
              type="password"
              id="pin"
              name="pin"
              value={formData.pin}
              onChange={handleInputChange}
              placeholder="Enter your 4-digit PIN"
              disabled={!selectedRecipient || loading}
            />
          </div>

          <div className="form-actions">
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={!selectedRecipient || !formData.amount || !formData.remark || !formData.pin || loading}
            >
              {loading ? 'Processing...' : 'Send Money'}
            </button>
            <button 
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setFormData({ recipientIdentifier: '', amount: '', remark: '', pin: '' });
                setSelectedRecipient(null);
                setError('');
              }}
            >
              Clear
            </button>
          </div>
        </form>
      </div>

      <div className="transfer-info-card">
        <h3>Transfer Information</h3>
        <ul>
          <li><strong>Recipient Search:</strong> Enter the email or phone number of the recipient</li>
          <li><strong>Amount:</strong> Enter the amount you want to transfer (in Rupees)</li>
          <li><strong>Purpose:</strong> Provide a remark or purpose for the transfer</li>
          <li><strong>PIN:</strong> Your transaction PIN is required to confirm the transfer</li>
          <li><strong>Settlement:</strong> Transfers settle in 3 business days via ACH</li>
          <li><strong>Daily Limit:</strong> You can send up to Rs 50,000 per day</li>
          <li><strong>Fees:</strong> No transfer fees apply to P2P transfers</li>
        </ul>
      </div>
    </div>
  );
}
