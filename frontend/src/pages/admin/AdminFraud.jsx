import React, { useState, useEffect } from 'react';
import { getAdminTransactions, blockAlert, dismissAlert } from '../../api/adminApi';
import './AdminFraud.css';

export default function AdminFraud() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showNotes, setShowNotes] = useState(null);
  const [notes, setNotes] = useState('');
  const [actionType, setActionType] = useState(null);
  const [actioningId, setActioningId] = useState(null);

  useEffect(() => {
    loadAlerts();
  }, []);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      const params = { status: 'PENDING', page: 0, size: 100 };
      const response = await getAdminTransactions(params);
      const fraudTransactions = response.data.content;
      setAlerts(fraudTransactions.map((txn, idx) => ({
        id: txn.id + idx,
        transactionId: txn.id,
        alertType: txn.type === 'SEND_MONEY' ? 'HIGH_VALUE' : 'DUPLICATE',
        status: 'PENDING',
        notes: '',
        createdAt: txn.initiatedAt
      })));
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load fraud alerts');
    } finally {
      setLoading(false);
    }
  };

  const handleActionClick = (alertId, type) => {
    setShowNotes(alertId);
    setActionType(type);
    setNotes('');
  };

  const handleConfirmAction = async () => {
    if (!showNotes) return;

    try {
      setActioningId(showNotes);
      const request = { notes };
      
      if (actionType === 'block') {
        await blockAlert(showNotes, request);
      } else if (actionType === 'dismiss') {
        await dismissAlert(showNotes, request);
      }
      
      await loadAlerts();
      setShowNotes(null);
      setNotes('');
      setActionType(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to perform action');
    } finally {
      setActioningId(null);
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return '-';
    return new Date(dateTime).toLocaleString();
  };

  if (loading) return <div className="fraud-loading">Loading fraud alerts...</div>;
  if (error) return <div className="fraud-error">{error}</div>;

  if (alerts.length === 0) {
    return (
      <div className="admin-fraud">
        <h1>Fraud Alert Review</h1>
        <div className="fraud-empty">
          <p>✓ No pending fraud alerts</p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-fraud">
      <h1>Fraud Alert Review ({alerts.length} pending)</h1>

      <div className="table-container">
        <table className="fraud-table">
          <thead>
            <tr>
              <th>Alert ID</th>
              <th>Transaction ID</th>
              <th>Alert Type</th>
              <th>Status</th>
              <th>Created</th>
              <th>Notes</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {alerts.map((alert) => (
              <tr key={alert.id}>
                <td>{alert.id}</td>
                <td>{alert.transactionId}</td>
                <td>
                  <span className={"alert-type alert-" + (alert.alertType?.toLowerCase() || "unknown")}>
                    {alert.alertType}
                  </span>
                </td>
                <td>
                  <span className="status-badge">{alert.status}</span>
                </td>
                <td>{formatDateTime(alert.createdAt)}</td>
                <td>
                  <div className="notes-preview">{alert.notes || "No notes"}</div>
                </td>
                <td className="action-buttons">
                  <button
                    className="btn-block"
                    onClick={() => handleActionClick(alert.id, "block")}
                    disabled={actioningId === alert.id}
                  >
                    Block
                  </button>
                  <button
                    className="btn-dismiss"
                    onClick={() => handleActionClick(alert.id, "dismiss")}
                    disabled={actioningId === alert.id}
                  >
                    Dismiss
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showNotes !== null && (
        <div className="modal-overlay" onClick={() => setShowNotes(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Add Notes</h2>
            <p>Alert ID: {showNotes}</p>
            <p>Action: <strong>{actionType === "block" ? "Block Alert" : "Dismiss Alert"}</strong></p>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add comments or reasoning..."
              rows={4}
            ></textarea>
            <div className="modal-buttons">
              <button
                className="btn-cancel"
                onClick={() => setShowNotes(null)}
              >
                Cancel
              </button>
              <button
                className={actionType === "block" ? "btn-block" : "btn-dismiss"}
                onClick={handleConfirmAction}
                disabled={actioningId === showNotes}
              >
                {actioningId === showNotes ? "Processing..." : "Confirm"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
