import React, { useState, useEffect } from "react";
import { getFraudAlerts, blockAlert, dismissAlert } from "../../api/adminApi";
import "./AdminFraud.css";

export default function AdminFraud() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [modal, setModal] = useState(null); // { alertId, type }
  const [notes, setNotes] = useState("");
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    loadAlerts();
  }, []);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await getFraudAlerts();
      setAlerts(response.data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load fraud alerts");
    } finally {
      setLoading(false);
    }
  };

  const openModal = (alertId, type) => {
    setModal({ alertId, type });
    setNotes("");
    setActionSuccess(null);
    setError(null);
  };

  const closeModal = () => {
    setModal(null);
    setNotes("");
  };

  const handleConfirm = async () => {
    if (!modal) return;
    setProcessing(true);
    setError(null);
    try {
      const request = { notes };
      if (modal.type === "block") {
        await blockAlert(modal.alertId, request);
        setActionSuccess("Transaction blocked and sender refunded.");
      } else {
        await dismissAlert(modal.alertId, request);
        setActionSuccess("Alert dismissed successfully.");
      }
      closeModal();
      await loadAlerts();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to perform action");
    } finally {
      setProcessing(false);
    }
  };

  // A settled transaction = transactionStatus is SUCCESS, FAILED, or DECLINED
  const isSettled = (alert) =>
    ["SUCCESS", "FAILED", "DECLINED"].includes(alert.transactionStatus);

  const formatDateTime = (dt) => {
    if (!dt) return "-";
    return new Date(dt).toLocaleString();
  };

  const alertTypeBadge = (type) => {
    const labels = {
      HIGH_VALUE: { label: "High Value", cls: "badge-high-value" },
      DUPLICATE_TRANSACTION: { label: "Duplicate", cls: "badge-duplicate" },
      DAILY_LIMIT_EXCEEDED: { label: "Daily Limit", cls: "badge-limit" },
    };
    const t = labels[type] || { label: type, cls: "" };
    return <span className={`alert-type ${t.cls}`}>{t.label}</span>;
  };

  const statusBadge = (status) => {
    const cls = {
      PENDING: "status-pending",
      BLOCKED: "status-blocked",
      DISMISSED: "status-dismissed",
    }[status] || "";
    return <span className={`status-badge ${cls}`}>{status}</span>;
  };

  if (loading) return <div className="fraud-loading">Loading fraud alerts...</div>;

  return (
    <div className="admin-fraud">
      <h1>Fraud Alert Review</h1>

      {error && <div className="fraud-error">{error}</div>}
      {actionSuccess && <div className="fraud-success">{actionSuccess}</div>}

      {alerts.length === 0 ? (
        <div className="fraud-empty">
          <p>✓ No fraud alerts found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="fraud-table">
            <thead>
              <tr>
                <th>Alert ID</th>
                <th>Transaction ID</th>
                <th>Alert Type</th>
                <th>Alert Status</th>
                <th>Transaction Status</th>
                <th>Notes</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {alerts.map((alert) => (
                <tr key={alert.id}>
                  <td>{alert.id}</td>
                  <td>{alert.transactionId}</td>
                  <td>{alertTypeBadge(alert.alertType)}</td>
                  <td>{statusBadge(alert.status)}</td>
                  <td>
                    <span className={`status-badge status-${alert.transactionStatus?.toLowerCase()}`}>
                      {alert.transactionStatus || "-"}
                    </span>
                  </td>
                  <td>{alert.notes || "-"}</td>
                  <td>{formatDateTime(alert.createdAt)}</td>
                  <td className="action-buttons">
                    {alert.status === "PENDING" && !isSettled(alert) ? (
                      <>
                        <button
                          className="btn-block"
                          onClick={() => openModal(alert.id, "block")}
                        >
                          Block
                        </button>
                        <button
                          className="btn-dismiss"
                          onClick={() => openModal(alert.id, "dismiss")}
                        >
                          Dismiss
                        </button>
                      </>
                    ) : (
                      <span className="read-only-label">
                        {isSettled(alert) ? "Settled - Read Only" : alert.status}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirmation Modal */}
      {modal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{modal.type === "block" ? "Block Transaction" : "Dismiss Alert"}</h2>
            <p>Alert ID: <strong>{modal.alertId}</strong></p>
            {modal.type === "block" && (
              <p className="modal-warning">
                ⚠️ This will mark the transaction as FAILED and refund the sender.
              </p>
            )}
            <label>Notes (optional)</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add reasoning or comments..."
              rows={4}
            />
            <div className="modal-buttons">
              <button className="btn-cancel" onClick={closeModal} disabled={processing}>
                Cancel
              </button>
              <button
                className={modal.type === "block" ? "btn-block" : "btn-dismiss"}
                onClick={handleConfirm}
                disabled={processing}
              >
                {processing ? "Processing..." : modal.type === "block" ? "Confirm Block" : "Confirm Dismiss"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
