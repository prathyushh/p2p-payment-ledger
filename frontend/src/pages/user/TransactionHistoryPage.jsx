import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import * as transactionApi from '../../api/transactionApi';
import './UserPages.css';
import './TransactionHistoryPage.css';

export default function TransactionHistoryPage() {
  const { user } = useAuth();

  // List view state
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination state
  const pageSize = 10;
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter state
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  // Detail view state
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');

  // Fetch transaction history
  const fetchTransactions = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const response = await transactionApi.getTransactions({
        page,
        size: pageSize,
      });

      if (response.data && response.data.content) {
        setTransactions(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setCurrentPage(page);
      } else {
        setTransactions([]);
      }
    } catch (err) {
      setError('Failed to load transactions. Please try again.');
      console.error('Fetch transactions error:', err);
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  // Fetch transaction detail
  const handleViewDetail = async (transactionId) => {
    setDetailLoading(true);
    setDetailError('');
    try {
      const response = await transactionApi.getTransaction(transactionId);
      setSelectedTransaction(response.data);
    } catch (err) {
      setDetailError('Failed to load transaction details. Please try again.');
      console.error('Fetch detail error:', err);
    } finally {
      setDetailLoading(false);
    }
  };

  // Apply filters (client-side filtering on current page)
  const getFilteredTransactions = () => {
    return transactions.filter((tx) => {
      const statusMatch = statusFilter === 'ALL' || tx.status === statusFilter;
      const typeMatch = typeFilter === 'ALL' || tx.type === typeFilter;
      return statusMatch && typeMatch;
    });
  };

  // Initial load
  useEffect(() => {
    fetchTransactions(0);
  }, [fetchTransactions]);

  // Handle page change
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchTransactions(newPage);
    }
  };

  // Get status color
  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING':
        return '#f59e0b';
      case 'SETTLED':
        return '#4ade80';
      case 'FAILED':
        return '#ef4444';
      case 'BLOCKED':
        return '#8b5cf6';
      default:
        return '#6b7280';
    }
  };

  // Get transaction type label and color
  const getTypeLabel = (type) => {
    switch (type) {
      case 'P2P_TRANSFER':
        return 'P2P Transfer';
      case 'TOPUP':
        return 'Top Up';
      default:
        return type;
    }
  };

  // Determine if current user is sender
  const isSender = (transaction) => transaction.senderEmail === user?.email;

  const filteredTransactions = getFilteredTransactions();

  // Detail view
  if (selectedTransaction && !detailLoading) {
    return (
      <div className="user-page transaction-detail-page">
        <div className="detail-header">
          <button className="btn-back" onClick={() => setSelectedTransaction(null)}>
            {String.fromCharCode(8592)} Back to History
          </button>
          <h1>Transaction Detail</h1>
        </div>

        {detailError && <div className="error-banner">{detailError}</div>}

        <div className="detail-card">
          <div className="detail-section">
            <h2>Transaction Information</h2>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">Transaction ID</span>
                <span className="value">{selectedTransaction.id}</span>
              </div>
              <div className="detail-item">
                <span className="label">Type</span>
                <span className="value">{getTypeLabel(selectedTransaction.type)}</span>
              </div>
              <div className="detail-item">
                <span className="label">Status</span>
                <span
                  className="value status-badge"
                  style={{ backgroundColor: getStatusColor(selectedTransaction.status) }}
                >
                  {selectedTransaction.status}
                </span>
              </div>
              <div className="detail-item">
                <span className="label">Amount</span>
                <span className="value">Rs {parseFloat(selectedTransaction.amount).toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h2>Parties</h2>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">Sender</span>
                <span className="value">{selectedTransaction.senderEmail}</span>
              </div>
              <div className="detail-item">
                <span className="label">Receiver</span>
                <span className="value">{selectedTransaction.receiverEmail}</span>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h2>Remark</h2>
            <div className="remark-box">{selectedTransaction.remark || 'No remark provided'}</div>
          </div>

          <div className="detail-section">
            <h2>Timeline</h2>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">Initiated At</span>
                <span className="value">
                  {new Date(selectedTransaction.initiatedAt).toLocaleString()}
                </span>
              </div>
              {selectedTransaction.settledAt && (
                <div className="detail-item">
                  <span className="label">Settled At</span>
                  <span className="value">
                    {new Date(selectedTransaction.settledAt).toLocaleString()}
                  </span>
                </div>
              )}
              {selectedTransaction.settlementDueDate && (
                <div className="detail-item">
                  <span className="label">Settlement Due</span>
                  <span className="value">{selectedTransaction.settlementDueDate}</span>
                </div>
              )}
            </div>
          </div>

          {selectedTransaction.ledgerEntries && selectedTransaction.ledgerEntries.length > 0 && (
            <div className="detail-section">
              <h2>Ledger Entries</h2>
              <div className="ledger-table">
                <table>
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Amount</th>
                      <th>Balance Before</th>
                      <th>Balance After</th>
                      <th>Created At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedTransaction.ledgerEntries.map((entry) => (
                      <tr key={entry.id}>
                        <td>{entry.type}</td>
                        <td>Rs {parseFloat(entry.amount).toFixed(2)}</td>
                        <td>Rs {parseFloat(entry.balanceBefore).toFixed(2)}</td>
                        <td>Rs {parseFloat(entry.balanceAfter).toFixed(2)}</td>
                        <td>{new Date(entry.createdAt).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // List view
  return (
    <div className="user-page transaction-history-page">
      <div className="page-header">
        <h1>Transaction History</h1>
        <p>View all your sent and received transactions</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Filters */}
      <div className="filters-card">
        <div className="filters-group">
          <div className="filter-item">
            <label htmlFor="status-filter">Status</label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="SETTLED">Settled</option>
              <option value="FAILED">Failed</option>
              <option value="BLOCKED">Blocked</option>
            </select>
          </div>

          <div className="filter-item">
            <label htmlFor="type-filter">Type</label>
            <select
              id="type-filter"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              <option value="ALL">All Types</option>
              <option value="P2P_TRANSFER">P2P Transfer</option>
              <option value="TOPUP">Top Up</option>
            </select>
          </div>

          <button
            className="btn btn-secondary"
            onClick={() => {
              setStatusFilter('ALL');
              setTypeFilter('ALL');
            }}
          >
            Clear Filters
          </button>
        </div>
      </div>

      {/* Loading state */}
      {loading && (
        <div className="user-page">
          <div className="loading">Loading transactions...</div>
        </div>
      )}

      {/* Empty state */}
      {!loading && filteredTransactions.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon">?</div>
          <h3>No Transactions Found</h3>
          <p>
            {statusFilter !== 'ALL' || typeFilter !== 'ALL'
              ? 'Try adjusting your filters'
              : 'You have no transactions yet'}
          </p>
          {(statusFilter !== 'ALL' || typeFilter !== 'ALL') && (
            <button
              className="btn btn-primary"
              onClick={() => {
                setStatusFilter('ALL');
                setTypeFilter('ALL');
              }}
            >
              Reset Filters
            </button>
          )}
        </div>
      )}

      {/* Transactions list */}
      {!loading && filteredTransactions.length > 0 && (
        <>
          <div className="transactions-list">
            {filteredTransactions.map((tx) => (
              <div key={tx.id} className="transaction-item">
                <div className="transaction-icon">
                  {isSender(tx) ? (String.fromCharCode(8594)) : (String.fromCharCode(8592))}
                </div>
                <div className="transaction-info">
                  <div className="transaction-header">
                    <span className="transaction-type">{getTypeLabel(tx.type)}</span>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: getStatusColor(tx.status) }}
                    >
                      {tx.status}
                    </span>
                  </div>
                  <div className="transaction-details">
                    <span className="counterparty">
                      {isSender(tx) ? 'To: ' : 'From: '}
                      {isSender(tx) ? tx.receiverEmail : tx.senderEmail}
                    </span>
                    <span className="remark">{tx.remark}</span>
                  </div>
                  <div className="transaction-date">
                    {new Date(tx.initiatedAt).toLocaleDateString()}
                  </div>
                </div>
                <div className="transaction-amount">
                  <span className={isSender(tx) ? 'debit' : 'credit'}>
                    {isSender(tx) ? '-' : '+'}Rs {parseFloat(tx.amount).toFixed(2)}
                  </span>
                </div>
                <div className="transaction-actions">
                  <button
                    className="btn-detail"
                    onClick={() => handleViewDetail(tx.id)}
                    disabled={detailLoading}
                  >
                    View
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          <div className="pagination">
            <button
              className="btn btn-secondary"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
            >
              Previous
            </button>
            <span className="page-info">
              Page {currentPage + 1} of {totalPages} ({totalElements} total)
            </span>
            <button
              className="btn btn-secondary"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}
