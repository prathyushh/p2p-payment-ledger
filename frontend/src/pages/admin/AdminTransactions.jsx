import React, { useState, useEffect } from "react";
import { getAdminTransactions, getAdminTransactionDetail } from "../../api/adminApi";
import "./AdminTransactions.css";

export default function AdminTransactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  
  // Filters
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [minAmount, setMinAmount] = useState("");
  const [maxAmount, setMaxAmount] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  
  // Sorting
  const [sortBy, setSortBy] = useState("initiatedAt");
  const [sortDir, setSortDir] = useState("desc");
  
  // Transaction detail modal
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);

  useEffect(() => {
    loadTransactions();
  }, [page, size, typeFilter, statusFilter, minAmount, maxAmount, startDate, endDate, sortBy, sortDir]);

  const loadTransactions = async () => {
    try {
      setLoading(true);
      const params = { page, size, sortBy, sortDir };
      if (typeFilter) params.type = typeFilter;
      if (statusFilter) params.status = statusFilter;
      if (minAmount) params.minAmount = minAmount;
      if (maxAmount) params.maxAmount = maxAmount;
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      
      const response = await getAdminTransactions(params);
      setTransactions(response.data.content);
      setTotalPages(response.data.totalPages);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load transactions");
    } finally {
      setLoading(false);
    }
  };

  const loadTransactionDetail = async (id) => {
    try {
      setModalLoading(true);
      setShowModal(true);
      const response = await getAdminTransactionDetail(id);
      setSelectedTransaction(response.data);
    } catch (err) {
      alert("Failed to load transaction detail: " + (err.response?.data?.message || err.message));
      setShowModal(false);
    } finally {
      setModalLoading(false);
    }
  };

  const handleSort = (field) => {
    if (sortBy === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortBy(field);
      setSortDir("desc");
    }
    setPage(0);
  };

  const resetFilters = () => {
    setTypeFilter("");
    setStatusFilter("");
    setMinAmount("");
    setMaxAmount("");
    setStartDate("");
    setEndDate("");
    setPage(0);
  };

  const formatCurrency = (amount) => {
    if (!amount) return "₹0";
    return "₹" + parseFloat(amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString('en-IN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDate = (date) => {
    if (!date) return "-";
    return new Date(date).toLocaleDateString('en-IN');
  };

  const getSortIcon = (field) => {
    if (sortBy !== field) return "⇅";
    return sortDir === "asc" ? "↑" : "↓";
  };

  if (loading && page === 0) return <div className="admin-txn-loading">Loading transactions...</div>;
  if (error) return <div className="admin-txn-error">{error}</div>;

  return (
    <div className="admin-transactions">
      <h1>Transaction Monitoring</h1>

      {/* Filters */}
      <div className="filters-section">
        <div className="filters-row">
          <div className="filter-group">
            <label>Type:</label>
            <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}>
              <option value="">All</option>
              <option value="P2P_TRANSFER">P2P Transfer</option>
              <option value="WALLET_TOP_UP">Wallet Top-Up</option>
              <option value="AUTO_RELOAD">Auto Reload</option>
            </select>
          </div>

          <div className="filter-group">
            <label>Status:</label>
            <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>

          <div className="filter-group">
            <label>Min Amount:</label>
            <input 
              type="number" 
              value={minAmount} 
              onChange={(e) => { setMinAmount(e.target.value); setPage(0); }}
              placeholder="₹0"
            />
          </div>

          <div className="filter-group">
            <label>Max Amount:</label>
            <input 
              type="number" 
              value={maxAmount} 
              onChange={(e) => { setMaxAmount(e.target.value); setPage(0); }}
              placeholder="₹999999"
            />
          </div>
        </div>

        <div className="filters-row">
          <div className="filter-group">
            <label>Start Date:</label>
            <input 
              type="date" 
              value={startDate} 
              onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
            />
          </div>

          <div className="filter-group">
            <label>End Date:</label>
            <input 
              type="date" 
              value={endDate} 
              onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
            />
          </div>

          <div className="filter-group">
            <button className="btn-reset" onClick={resetFilters}>Reset Filters</button>
          </div>
        </div>
      </div>

      {/* Results count */}
      <div className="results-info">
        Showing {transactions.length} transactions
      </div>

      {/* Table */}
      <div className="table-container">
        <table className="transactions-table">
          <thead>
            <tr>
              <th onClick={() => handleSort("id")} className="sortable">
                ID {getSortIcon("id")}
              </th>
              <th onClick={() => handleSort("type")} className="sortable">
                Type {getSortIcon("type")}
              </th>
              <th onClick={() => handleSort("status")} className="sortable">
                Status {getSortIcon("status")}
              </th>
              <th onClick={() => handleSort("amount")} className="sortable">
                Amount {getSortIcon("amount")}
              </th>
              <th>Sender</th>
              <th>Receiver</th>
              <th onClick={() => handleSort("settlementDueDate")} className="sortable">
                Due Date {getSortIcon("settlementDueDate")}
              </th>
              <th onClick={() => handleSort("initiatedAt")} className="sortable">
                Initiated {getSortIcon("initiatedAt")}
              </th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {transactions.length === 0 ? (
              <tr>
                <td colSpan="9" className="no-data">No transactions found</td>
              </tr>
            ) : (
              transactions.map((txn) => {
                const typeClass = "badge-" + (txn.type?.toLowerCase().replace(/_/g, '-') || "unknown");
                const statusClass = "status-" + (txn.status?.toLowerCase() || "unknown");
                const isPending = txn.status === "PENDING";
                return (
                  <tr key={txn.id} className={isPending ? "pending-row" : ""}>
                    <td>{txn.id}</td>
                    <td><span className={"badge " + typeClass}>{txn.type?.replace(/_/g, ' ')}</span></td>
                    <td><span className={statusClass}>{txn.status}</span></td>
                    <td className="amount">{formatCurrency(txn.amount)}</td>
                    <td>{txn.senderEmail}</td>
                    <td>{txn.receiverEmail}</td>
                    <td>{formatDate(txn.settlementDueDate)}</td>
                    <td>{formatDateTime(txn.initiatedAt)}</td>
                    <td>
                      <button 
                        className="btn-view-detail"
                        onClick={() => loadTransactionDetail(txn.id)}
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
            Previous
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button onClick={() => setPage(p => (p + 1 < totalPages ? p + 1 : p))} disabled={page + 1 >= totalPages}>
            Next
          </button>
        </div>
      )}

      {/* Transaction Detail Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Transaction Details</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            <div className="modal-body">
              {modalLoading ? (
                <div className="modal-loading">Loading details...</div>
              ) : selectedTransaction ? (
                <>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <label>Transaction ID:</label>
                      <span>{selectedTransaction.id}</span>
                    </div>
                    <div className="detail-item">
                      <label>Type:</label>
                      <span className={"badge badge-" + selectedTransaction.type?.toLowerCase().replace(/_/g, '-')}>
                        {selectedTransaction.type?.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <div className="detail-item">
                      <label>Status:</label>
                      <span className={"status-" + selectedTransaction.status?.toLowerCase()}>
                        {selectedTransaction.status}
                      </span>
                    </div>
                    <div className="detail-item">
                      <label>Amount:</label>
                      <span className="amount-large">{formatCurrency(selectedTransaction.amount)}</span>
                    </div>
                    <div className="detail-item">
                      <label>Sender:</label>
                      <span>{selectedTransaction.senderEmail}</span>
                    </div>
                    <div className="detail-item">
                      <label>Receiver:</label>
                      <span>{selectedTransaction.receiverEmail}</span>
                    </div>
                    <div className="detail-item">
                      <label>Remark:</label>
                      <span>{selectedTransaction.remark || "-"}</span>
                    </div>
                    <div className="detail-item">
                      <label>Settlement Due:</label>
                      <span>{formatDate(selectedTransaction.settlementDueDate)}</span>
                    </div>
                    <div className="detail-item">
                      <label>Initiated At:</label>
                      <span>{formatDateTime(selectedTransaction.initiatedAt)}</span>
                    </div>
                    <div className="detail-item">
                      <label>Settled At:</label>
                      <span>{formatDateTime(selectedTransaction.settledAt)}</span>
                    </div>
                  </div>

                  {/* Ledger Entries */}
                  <div className="ledger-section">
                    <h3>Ledger Entries</h3>
                    {selectedTransaction.ledgerEntries && selectedTransaction.ledgerEntries.length > 0 ? (
                      <table className="ledger-table">
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
                              <td className={"ledger-type ledger-" + entry.type?.toLowerCase()}>
                                {entry.type}
                              </td>
                              <td>{formatCurrency(entry.amount)}</td>
                              <td>{formatCurrency(entry.balanceBefore)}</td>
                              <td>{formatCurrency(entry.balanceAfter)}</td>
                              <td>{formatDateTime(entry.createdAt)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <p>No ledger entries found</p>
                    )}
                  </div>
                </>
              ) : (
                <div>No data available</div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}