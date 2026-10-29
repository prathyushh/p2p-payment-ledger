import React from "react";
import { Link, useLocation } from "react-router-dom";
import "./AdminLayout.css";

export default function AdminLayout({ children }) {
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <div className="admin-layout">
      <nav className="admin-nav">
        <div className="admin-nav-title">Admin Panel</div>
        <ul className="admin-nav-list">
          <li>
            <Link to="/admin/dashboard" className={isActive("/admin/dashboard") ? "admin-nav-link active" : "admin-nav-link"}>
              📊 Dashboard
            </Link>
          </li>
          <li>
            <Link to="/admin/users" className={isActive("/admin/users") ? "admin-nav-link active" : "admin-nav-link"}>
              👥 Users
            </Link>
          </li>
          <li>
            <Link to="/admin/transactions" className={isActive("/admin/transactions") ? "admin-nav-link active" : "admin-nav-link"}>
              💳 Transactions
            </Link>
          </li>
          <li>
            <Link to="/admin/settlement" className={isActive("/admin/settlement") ? "admin-nav-link active" : "admin-nav-link"}>
              ⏰ Settlement
            </Link>
          </li>
          <li>
            <Link to="/admin/fraud" className={isActive("/admin/fraud") ? "admin-nav-link active" : "admin-nav-link"}>
              🚨 Fraud
            </Link>
          </li>
          <li>
            <Link to="/admin/jobs" className={isActive("/admin/jobs") ? "admin-nav-link active" : "admin-nav-link"}>
              ⚙️ Jobs
            </Link>
          </li>
          <li>
            <Link to="/admin/reconciliation" className={isActive("/admin/reconciliation") ? "admin-nav-link active" : "admin-nav-link"}>
              📋 Reconciliation
            </Link>
          </li>
        </ul>
      </nav>
      <div className="admin-content">
        {children}
      </div>
    </div>
  );
}
