import React, { useState, useEffect } from "react";
import { getAdminTransactions } from "../../api/adminApi";
import "./AdminTransactions.css";

export default function AdminTransactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  useEffect(() => {
    loadTransactions();
  }, [page, size, typeFilter, statusFilter]);

  const loadTransactions = async () => {
    try {
      setLoading(true);
      const params = { page, size };
      if (typeFilter) params.type = typeFilter;
      if (statusFilter) params.status = statusFilter;
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

  const formatCurrency = (amount) => {
    if (!amount) return "₹0";
    return "₹" + parseFloat(amount).toFixed(2);
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString();
  };

  if (loading && page === 0) return <div className="admin-txn-loading">Loading transactions...</div>;
  if (error) return <div className="admin-txn-error">{error}</div>;

  return (
    <div className="admin-transactions">
      <h1>Transaction Monitoring</h1>

      <div className="filters">
        <div className="filter-group">
          <label>Type:</label>
          <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}>
            <option value="">All</option>
            <option value="SEND_MONEY">Send Money</option>
            <option value="WALLET_TOP_UP">Wallet Top-Up</option>
          </select>
        </div>
        <div className="filter-group">
          <label>Status:</label>
          <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="transactions-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Type</th>
              <th>Status</th>
              <th>Amount</th>
              <th>Sender</th>
              <th>Receiver</th>
              <th>Due Date</th>
              <th>Initiated</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((txn) => {
              const typeClass = "badge-" + (txn.type?.toLowerCase() || "unknown");
              const statusClass = "status-" + (txn.status?.toLowerCase() || "unknown");
              return (
                <tr key={txn.id}>
                  <td>{txn.id}</td>
                  <td><span className={"badge " + typeClass}>{txn.type}</span></td>
                  <td><span className={statusClass}>{txn.status}</span></td>
                  <td>{formatCurrency(txn.amount)}</td>
                  <td>{txn.senderEmail}</td>
                  <td>{txn.receiverEmail}</td>
                  <td>{txn.settlementDueDate}</td>
                  <td>{formatDateTime(txn.initiatedAt)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

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
    </div>
  );
}
