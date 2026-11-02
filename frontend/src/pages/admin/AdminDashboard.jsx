import React, { useState, useEffect } from 'react';
import { getDashboardStats } from '../../api/adminApi';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './AdminDashboard.css';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadStats = async () => {
    try {
      setLoading(true);
      const response = await getDashboardStats();
      setStats(response.data);
      setLastUpdated(new Date());
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStats();
    
    // Auto-refresh every 30 seconds for live data
    const intervalId = setInterval(() => {
      loadStats();
    }, 30000);

    // Cleanup interval on unmount
    return () => clearInterval(intervalId);
  }, []);

  if (loading) return <div className='admin-dashboard-loading'>Loading dashboard...</div>;
  if (error) return <div className='admin-dashboard-error'>{error}</div>;
  if (!stats) return <div className='admin-dashboard-error'>No data available</div>;

  // Format currency
  const formatCurrency = (amount) => {
    if (!amount) return '₹0';
    return '₹' + parseFloat(amount).toFixed(2);
  };

  return (
    <div className='admin-dashboard'>
      <div className='dashboard-header'>
        <h1>Admin Dashboard</h1>
        <div className='dashboard-controls'>
          {lastUpdated && (
            <span className='last-updated'>
              Last updated: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          <button 
            onClick={loadStats} 
            disabled={loading}
            className='btn-refresh'
            title='Refresh dashboard data'
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className='stats-grid'>
        <div className='stat-card'>
          <h3>Total Users</h3>
          <p className='stat-value'>{stats.totalUsers}</p>
        </div>
        <div className='stat-card'>
          <h3>Today Transfers Initiated</h3>
          <p className='stat-value'>{stats.todayTransfersInitiated}</p>
        </div>
        <div className='stat-card'>
          <h3>Today Transfers Settled</h3>
          <p className='stat-value'>{stats.todayTransfersSettled}</p>
        </div>
        <div className='stat-card'>
          <h3>Today Volume</h3>
          <p className='stat-value'>{formatCurrency(stats.todayTransactionVolume)}</p>
        </div>
        <div className='stat-card'>
          <h3>Pending Settlement</h3>
          <p className='stat-value'>{formatCurrency(stats.totalPendingSettlementAmount)}</p>
        </div>
        <div className='stat-card'>
          <h3>Pending Fraud Alerts</h3>
          <p className='stat-value alert'>{stats.pendingFraudAlertsCount}</p>
        </div>
      </div>

      {/* 30-Day Volume Chart */}
      <div className='chart-container'>
        <h2>Last 30 Days Volume</h2>
        {stats.last30DaysDailyVolume && stats.last30DaysDailyVolume.length > 0 ? (
          <ResponsiveContainer width='100%' height={400}>
            <LineChart data={stats.last30DaysDailyVolume}>
              <CartesianGrid strokeDasharray='3 3' />
              <XAxis 
                dataKey='date' 
                angle={-45}
                textAnchor='end'
                height={80}
                tick={{ fontSize: 12 }}
              />
              <YAxis label={{ value: 'Volume (₹)', angle: -90, position: 'insideLeft' }} />
              <Tooltip 
                formatter={(value) => formatCurrency(value)}
                labelFormatter={(label) => ('Date: ' + label)}
              />
              <Legend />
              <Line 
                type='monotone' 
                dataKey='volume' 
                stroke='#8884d8' 
                dot={{ fill: '#8884d8' }}
                activeDot={{ r: 6 }}
                name='Transaction Volume'
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className='chart-loading'>
            <p>Loading chart data...</p>
          </div>
        )}
      </div>
    </div>
  );
}
