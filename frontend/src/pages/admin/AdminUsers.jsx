import React, { useState, useEffect } from "react";
import { getUsers, freezeWallet, unfreezeWallet } from "../../api/adminApi";
import "./AdminUsers.css";

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState("");
  const [totalPages, setTotalPages] = useState(0);
  const [freezingId, setFreezingId] = useState(null);

  useEffect(() => {
    loadUsers();
  }, [page, size, search]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const params = { page, size };
      if (search) params.search = search;
      const response = await getUsers(params);
      setUsers(response.data.content);
      setTotalPages(response.data.totalPages);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  const handleFreeze = async (userId) => {
    try {
      setFreezingId(userId);
      await freezeWallet(userId);
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to freeze wallet");
    } finally {
      setFreezingId(null);
    }
  };

  const handleUnfreeze = async (userId) => {
    try {
      setFreezingId(userId);
      await unfreezeWallet(userId);
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to unfreeze wallet");
    } finally {
      setFreezingId(null);
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

  if (loading && page === 0) return <div className="admin-users-loading">Loading users...</div>;
  if (error) return <div className="admin-users-error">{error}</div>;

  return (
    <div className="admin-users">
      <h1>User Management</h1>
      <div className="search-box">
        <input type="text" placeholder="Search by name, email, or phone..." value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
      </div>
      <div className="table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Wallet Balance</th>
              <th>Status</th>
              <th>Locked</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => {
              const statusClass = "badge-" + (user.walletStatus?.toLowerCase() || "unknown");
              return (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>{user.phoneNumber}</td>
                  <td>{formatCurrency(user.walletBalance)}</td>
                  <td><span className={"badge " + statusClass}>{user.walletStatus}</span></td>
                  <td>{user.accountLocked ? <span className="locked-badge">Locked</span> : <span className="unlocked-badge">Active</span>}</td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td>
                    {user.walletStatus === "FROZEN" ? (
                      <button className="btn-action btn-unfreeze" onClick={() => handleUnfreeze(user.id)} disabled={freezingId === user.id}>
                        {freezingId === user.id ? "Unfreezing..." : "Unfreeze"}
                      </button>
                    ) : (
                      <button className="btn-action btn-freeze" onClick={() => handleFreeze(user.id)} disabled={freezingId === user.id}>
                        {freezingId === user.id ? "Freezing..." : "Freeze"}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button onClick={() => setPage(p => (p + 1 < totalPages ? p + 1 : p))} disabled={page + 1 >= totalPages}>Next</button>
        </div>
      )}
    </div>
  );
}
