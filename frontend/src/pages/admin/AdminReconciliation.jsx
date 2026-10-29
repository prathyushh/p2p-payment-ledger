import React, { useState, useEffect } from "react";
import { getReconciliation } from "../../api/adminApi";
import "./AdminReconciliation.css";

export default function AdminReconciliation() {
  const [reconciliation, setReconciliation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadReconciliation();
  }, []);

  const loadReconciliation = async () => {
    try {
      setLoading(true);
      const response = await getReconciliation();
      setReconciliation(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load reconciliation data");
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    if (!amount) return "₹0";
    return "₹" + parseFloat(amount).toFixed(2);
  };

  const formatDate = (date) => {
    if (!date) return "-";
    return new Date(date).toLocaleDateString();
  };

  if (loading) return <div className="recon-loading">Loading reconciliation data...</div>;
  if (error) return <div className="recon-error">{error}</div>;
  if (!reconciliation) return <div className="recon-error">No reconciliation data available</div>;

  const hasMismatch = reconciliation.reconciliationMismatch && reconciliation.reconciliationMismatch !== 0;

  return (
    <div className="admin-reconciliation">
      <h1>Daily Reconciliation</h1>
      <p className="date-label">Date: {formatDate(reconciliation.summaryDate)}</p>

      {hasMismatch && (
        <div className="mismatch-alert">
          <h3>⚠️ Reconciliation Mismatch Detected</h3>
          <p>Debit/Credit imbalance: {formatCurrency(reconciliation.reconciliationMismatch)}</p>
          <p className="alert-text">Please investigate the ledger entries immediately.</p>
        </div>
      )}

      <div className="recon-grid">
        <div className="recon-card">
          <h3>Transfers Initiated</h3>
          <div className="metric">
            <span className="label">Count:</span>
            <span className="value">{reconciliation.totalTransfersInitiated}</span>
          </div>
          <div className="metric">
            <span className="label">Amount:</span>
            <span className="value">{formatCurrency(reconciliation.totalTransfersInitiatedAmount)}</span>
          </div>
        </div>
        <div className="recon-card">
          <h3>Transfers Settled</h3>
          <div className="metric">
            <span className="label">Count:</span>
            <span className="value">{reconciliation.totalTransfersSettled}</span>
          </div>
          <div className="metric">
            <span className="label">Amount:</span>
            <span className="value">{formatCurrency(reconciliation.totalTransfersSettledAmount)}</span>
          </div>
        </div>
        <div className="recon-card">
          <h3>Fraud Activity</h3>
          <div className="metric">
            <span className="label">Alerts Raised:</span>
            <span className="value">{reconciliation.fraudAlertsRaised}</span>
          </div>
        </div>
        <div className="recon-card">
          <h3>User Activity</h3>
          <div className="metric">
            <span className="label">New Registrations:</span>
            <span className="value">{reconciliation.newUserRegistrations}</span>
          </div>
        </div>
        <div className="recon-card">
          <h3>Pending Settlement</h3>
          <div className="metric">
            <span className="label">Amount:</span>
            <span className="value">{formatCurrency(reconciliation.totalPendingSettlementAmount)}</span>
          </div>
        </div>
      </div>

      <div className="ledger-summary">
        <h2>Ledger Summary</h2>
        <div className="ledger-cards">
          <div className="ledger-card debit">
            <h3>Total Debits</h3>
            <p className="amount">{formatCurrency(reconciliation.reconciliationDebitSum)}</p>
          </div>
          <div className="ledger-card credit">
            <h3>Total Credits</h3>
            <p className="amount">{formatCurrency(reconciliation.reconciliationCreditSum)}</p>
          </div>
          <div className={hasMismatch ? "ledger-card mismatch alert" : "ledger-card mismatch"}>
            <h3>Mismatch</h3>
            <p className="amount">{formatCurrency(reconciliation.reconciliationMismatch)}</p>
            <p className="status">{hasMismatch ? "❌ Out of Balance" : "✓ Balanced"}</p>
          </div>
        </div>
      </div>

      <div className="last-updated">
        <p>Last updated: {formatDate(reconciliation.createdAt)} at {new Date(reconciliation.createdAt).toLocaleTimeString()}</p>
      </div>
    </div>
  );
}
