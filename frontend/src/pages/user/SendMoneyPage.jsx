import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import * as transactionApi from '../../api/transactionApi';
import * as userApi from '../../api/userApi';
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
  const [searchResults, setSearchResults] = useState([]);
  const [selectedRecipient, setSelectedRecipient] = useState(null);
  const [showResults, setShowResults] = useState(false);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
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

  const handleSearchChange = async (e) => {
    const value = e.target.value;
    setFormData(prev => ({ ...prev, recipientIdentifier: value }));
    setSelectedRecipient(null);
    setError('');
    setShowResults(false);

    if (!value || value.trim().length < 2) {
      setSearchResults([]);
      return;
    }

    // Debounce search
    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    debounceTimer.current = setTimeout(async () => {
      setSearching(true);
      try {
        const response = await userApi.searchRecipients(value.trim());
        setSearchResults(response.data || []);
        setShowResults(true);
        setSearching(false);
      } catch (err) {
        console.error('Search error:', err);
        setSearchResults([]);
        setSearching(false);
      }
    }, 300);
  };

  const handleSelectRecipient = (recipient) => {
    // Use email or phone number as identifier
    const identifier = recipient.email || recipient.phoneNumber;
    setFormData(prev => ({ ...prev, recipientIdentifier: identifier }));
    setSelectedRecipient(recipient);
    setShowResults(false);
    setSearchResults([]);
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
      // Don't clear selectedRecipient here - we need it for the success screen
      setFormData({ recipientIdentifier: '', amount: '', remark: '', pin: '' });
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
          <h1>Transfer Complete</h1>
          <p>Your payment has been initiated</p>
        </div>

        <div className="success-card">
          <div className="success-icon">✓</div>
          <h2>Transfer Initiated Successfully</h2>
          <p className="success-subtitle">Your money will be delivered in 3 business days</p>
          
          <div className="transaction-details">
            <div className="detail-row">
              <span className="label">Transaction ID</span>
              <span className="value">#{transactionResult.transactionId}</span>
            </div>
            <div className="detail-row highlight">
              <span className="label">Amount</span>
              <span className="value amount">₹{parseFloat(transactionResult.amount).toFixed(2)}</span>
            </div>
            <div className="detail-row">
              <span className="label">Recipient</span>
              <span className="value">{transactionResult.recipientName || selectedRecipient?.name || 'N/A'}</span>
            </div>
            <div className="detail-row sub">
              <span className="label">Contact</span>
              <span className="value">{transactionResult.recipientEmail || selectedRecipient?.email || 'N/A'}</span>
            </div>
            <div className="detail-row">
              <span className="label">Status</span>
              <span className="value">
                <span className="status-badge pending">{transactionResult.status}</span>
              </span>
            </div>
            <div className="detail-row">
              <span className="label">Settlement Due Date</span>
              <span className="value">{new Date(transactionResult.settlementDueDate).toLocaleDateString('en-IN', { 
                day: '2-digit', 
                month: 'short', 
                year: 'numeric' 
              })}</span>
            </div>
            <div className="detail-row">
              <span className="label">New Balance</span>
              <span className="value">₹{parseFloat(transactionResult.senderNewBalance).toFixed(2)}</span>
            </div>
          </div>

          <div className="info-message">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M10 0C4.48 0 0 4.48 0 10C0 15.52 4.48 20 10 20C15.52 20 20 15.52 20 10C20 4.48 15.52 0 10 0ZM11 15H9V13H11V15ZM11 11H9V5H11V11Z" fill="#2563eb"/>
            </svg>
            <div>
              <strong>Settlement Information</strong>
              <p>This transfer will settle in 3 business days via ACH. You will receive a notification once the settlement is complete.</p>
            </div>
          </div>

          <div className="success-actions">
            <button 
              className="btn btn-primary"
              onClick={() => {
                setSuccess(false);
                setTransactionResult(null);
                setSelectedRecipient(null);
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
                placeholder="Search by name, email or phone"
                disabled={loading}
                autoComplete="off"
              />
              {searching && <div className="search-loading">Searching...</div>}
            </div>
            {showResults && !selectedRecipient && searchResults.length > 0 && (
              <div className="recipient-suggestions">
                {searchResults.map((recipient) => (
                  <div 
                    key={recipient.id} 
                    className="suggestion-item" 
                    onClick={() => handleSelectRecipient(recipient)}
                  >
                    <div className="recipient-info">
                      <span className="recipient-name">{recipient.name}</span>
                      <span className="recipient-contact">{recipient.email}</span>
                      {recipient.phoneNumber && (
                        <span className="recipient-phone">{recipient.phoneNumber}</span>
                      )}
                    </div>
                    <button type="button" className="btn-small">Select</button>
                  </div>
                ))}
              </div>
            )}
            {showResults && !selectedRecipient && formData.recipientIdentifier.length >= 2 && searchResults.length === 0 && !searching && (
              <div className="no-results">
                No users found matching "{formData.recipientIdentifier}"
              </div>
            )}
            {selectedRecipient && (
              <div className="selected-recipient">
                <div className="selected-info">
                  <span className="checkmark">{String.fromCharCode(10003)}</span>
                  <div>
                    <div className="selected-name">{selectedRecipient.name}</div>
                    <div className="selected-contact">{selectedRecipient.email}</div>
                  </div>
                </div>
                <button 
                  type="button" 
                  className="btn-change"
                  onClick={() => {
                    setSelectedRecipient(null);
                    setFormData(prev => ({ ...prev, recipientIdentifier: '' }));
                  }}
                >
                  Change
                </button>
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

