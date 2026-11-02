import React, { useState, useEffect } from 'react';
import { getSettlementQueue, getPendingSettlements, triggerSettlement } from '../../api/adminApi';
import './AdminSettlement.css';

export default function AdminSettlement() {
  const [queue, setQueue] = useState(null);
  const [pending, setPending] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [triggeringId, setTriggeringId] = useState(null);

  useEffect(() => {
    loadSettlementData();
  }, []);

  const loadSettlementData = async () => {
    try {
      setLoading(true);
      const [queueRes, pendingRes] = await Promise.all([
        getSettlementQueue(),
        getPendingSettlements({ includeOverdue: true, includeToday: true, includeTomorrow: true, includeFuture: true })
      ]);
      setQueue(queueRes.data);
      setPending(pendingRes.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load settlement data');
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerSettlement = async (transactionId, amount, sender, receiver) => {
    const confirmed = window.confirm(
      `Are you sure you want to manually trigger settlement for this transaction?\n\n` +
      `Transaction ID: ${transactionId}\n` +
      `Amount: ${formatCurrency(amount)}\n` +
      `From: ${sender}\n` +
      `To: ${receiver}\n\n` +
      `This will immediately settle the transaction and credit the receiver's wallet.`
    );

    if (!confirmed) return;

    try {
      setTriggeringId(transactionId);
      setError(null);
      await triggerSettlement(transactionId);
      alert(`✅ Transaction ${transactionId} has been successfully settled!`);
      await loadSettlementData();
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to trigger settlement';
      setError(errorMsg);
      alert(`❌ Settlement failed: ${errorMsg}`);
    } finally {
      setTriggeringId(null);
    }
  };

  const formatCurrency = (amount) => {
    if (!amount) return '₹0';
    return '₹' + parseFloat(amount).toFixed(2);
  };

  if (loading) return <div className='settlement-loading'>Loading settlement data...</div>;
  if (error) return <div className='settlement-error'>{error}</div>;
  if (!queue) return <div className='settlement-error'>No settlement data available</div>;

  return (
    <div className='admin-settlement'>
      <h1>Settlement Monitor</h1>

      {/* Queue Summary Cards */}
      <div className='queue-summary'>
        <div className='queue-card overdue'>
          <h3>Overdue Pending</h3>
          <p className='count'>{queue.overduePendingCount}</p>
          <p className='amount'>{formatCurrency(queue.overdueAmount)}</p>
        </div>
        <div className='queue-card due-today'>
          <h3>Due Today</h3>
          <p className='count'>{queue.dueTodayCount}</p>
          <p className='amount'>{formatCurrency(queue.dueTodayAmount)}</p>
        </div>
        <div className='queue-card due-tomorrow'>
          <h3>Due Tomorrow</h3>
          <p className='count'>{queue.dueTomorrowCount}</p>
          <p className='amount'>{formatCurrency(queue.dueTomorrowAmount)}</p>
        </div>
        <div className='queue-card total'>
          <h3>Total Pending</h3>
          <p className='count'>{queue.totalPendingCount}</p>
          <p className='amount'>{formatCurrency(queue.totalPendingAmount)}</p>
        </div>
      </div>

      {/* Pending Transactions */}
      {pending && (
        <div className='pending-sections'>
          {/* Overdue */}
          {pending.overdue_transactions && pending.overdue_transactions.length > 0 && (
            <div className='pending-section overdue-section'>
              <h2>⚠️ Overdue Transactions ({pending.overdue_transactions.length})</h2>
              <div className='table-container'>
                <table className='settlement-table'>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Sender</th>
                      <th>Receiver</th>
                      <th>Due Date</th>
                      <th>Days Overdue</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pending.overdue_transactions.map((txn) => {
                      const daysOverdue = Math.floor((new Date() - new Date(txn.settlementDueDate)) / (1000 * 60 * 60 * 24));
                      return (
                        <tr key={txn.id} className='overdue-row'>
                          <td>{txn.id}</td>
                          <td>{formatCurrency(txn.amount)}</td>
                          <td>{txn.senderEmail}</td>
                          <td>{txn.receiverEmail}</td>
                          <td>{txn.settlementDueDate}</td>
                          <td><span className='overdue-badge'>{daysOverdue} days</span></td>
                          <td>
                            <button
                              className='btn-trigger'
                              onClick={() => handleTriggerSettlement(txn.id, txn.amount, txn.senderEmail, txn.receiverEmail)}
                              disabled={triggeringId === txn.id}
                              title="Manually trigger settlement for this overdue transaction"
                            >
                              {triggeringId === txn.id ? 'Settling...' : 'Trigger Settlement'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Due Today */}
          {pending.due_today_transactions && pending.due_today_transactions.length > 0 && (
            <div className='pending-section'>
              <h2>📅 Due Today ({pending.due_today_transactions.length})</h2>
              <div className='table-container'>
                <table className='settlement-table'>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Sender</th>
                      <th>Receiver</th>
                      <th>Due Date</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pending.due_today_transactions.map((txn) => (
                      <tr key={txn.id}>
                        <td>{txn.id}</td>
                        <td>{formatCurrency(txn.amount)}</td>
                        <td>{txn.senderEmail}</td>
                        <td>{txn.receiverEmail}</td>
                        <td>{txn.settlementDueDate}</td>
                        <td>
                          <button
                            className='btn-trigger'
                            onClick={() => handleTriggerSettlement(txn.id, txn.amount, txn.senderEmail, txn.receiverEmail)}
                            disabled={triggeringId === txn.id}
                            title="Manually trigger settlement for this transaction"
                          >
                            {triggeringId === txn.id ? 'Settling...' : 'Trigger Settlement'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Due Tomorrow */}
          {pending.due_tomorrow_transactions && pending.due_tomorrow_transactions.length > 0 && (
            <div className='pending-section'>
              <h2>⏰ Due Tomorrow ({pending.due_tomorrow_transactions.length})</h2>
              <div className='table-container'>
                <table className='settlement-table'>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Sender</th>
                      <th>Receiver</th>
                      <th>Due Date</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pending.due_tomorrow_transactions.map((txn) => (
                      <tr key={txn.id}>
                        <td>{txn.id}</td>
                        <td>{formatCurrency(txn.amount)}</td>
                        <td>{txn.senderEmail}</td>
                        <td>{txn.receiverEmail}</td>
                        <td>{txn.settlementDueDate}</td>
                        <td>
                          <button
                            className='btn-trigger'
                            onClick={() => handleTriggerSettlement(txn.id, txn.amount, txn.senderEmail, txn.receiverEmail)}
                            disabled={triggeringId === txn.id}
                            title="Manually trigger settlement for this transaction"
                          >
                            {triggeringId === txn.id ? 'Settling...' : 'Trigger Settlement'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Future Pending */}
          {pending.future_transactions && pending.future_transactions.length > 0 && (
            <div className='pending-section'>
              <h2>📆 Future Pending ({pending.future_transactions.length})</h2>
              <div className='table-container'>
                <table className='settlement-table'>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Sender</th>
                      <th>Receiver</th>
                      <th>Due Date</th>
                      <th>Days Until Due</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pending.future_transactions.map((txn) => {
                      const daysUntilDue = Math.ceil((new Date(txn.settlementDueDate) - new Date()) / (1000 * 60 * 60 * 24));
                      return (
                        <tr key={txn.id}>
                          <td>{txn.id}</td>
                          <td>{formatCurrency(txn.amount)}</td>
                          <td>{txn.senderEmail}</td>
                          <td>{txn.receiverEmail}</td>
                          <td>{txn.settlementDueDate}</td>
                          <td><span className='future-badge'>{daysUntilDue} days</span></td>
                          <td>
                            <button
                              className='btn-trigger'
                              onClick={() => handleTriggerSettlement(txn.id, txn.amount, txn.senderEmail, txn.receiverEmail)}
                              disabled={triggeringId === txn.id}
                              title="Manually trigger early settlement for this transaction"
                            >
                              {triggeringId === txn.id ? 'Settling...' : 'Trigger Settlement'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
