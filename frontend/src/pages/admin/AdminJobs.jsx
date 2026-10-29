import React, { useState, useEffect } from 'react';
import { getJobLogs } from '../../api/adminApi';
import './AdminJobs.css';

export default function AdminJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadJobLogs();
  }, []);

  const loadJobLogs = async () => {
    try {
      setLoading(true);
      const response = await getJobLogs();
      setJobs(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load job logs');
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return '-';
    return new Date(dateTime).toLocaleString();
  };

  const formatDuration = (startedAt, endedAt) => {
    if (!startedAt || !endedAt) return '-';
    const ms = new Date(endedAt) - new Date(startedAt);
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) return seconds + 's';
    const minutes = Math.floor(seconds / 60);
    return minutes + 'm ' + (seconds % 60) + 's';
  };

  const formatCurrency = (amount) => {
    if (!amount) return '₹0';
    return '₹' + parseFloat(amount).toFixed(2);
  };

  if (loading) return <div className='jobs-loading'>Loading job logs...</div>;
  if (error) return <div className='jobs-error'>{error}</div>;

  return (
    <div className='admin-jobs'>
      <h1>Job Monitor</h1>
      <p className='subtitle'>Last 20 scheduled job runs (most recent first)</p>

      {/* Jobs Table */}
      <div className='table-container'>
        <table className='jobs-table'>
          <thead>
            <tr>
              <th>Job Name</th>
              <th>Status</th>
              <th>Started</th>
              <th>Duration</th>
              <th>Processed</th>
              <th>Failed</th>
              <th>Total Amount</th>
              <th>Error</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => {
              const statusClass = 'status-' + (job.status?.toLowerCase() || 'unknown');
              return (
                <tr key={job.id} className={statusClass}>
                  <td className='job-name'>{job.jobName}</td>
                  <td>
                    <span className={'status-badge ' + statusClass}>
                      {job.status}
                    </span>
                  </td>
                  <td>{formatDateTime(job.startedAt)}</td>
                  <td>{formatDuration(job.startedAt, job.endedAt)}</td>
                  <td>{job.recordsProcessed || 0}</td>
                  <td className='failed-count'>{job.recordsFailed || 0}</td>
                  <td>{formatCurrency(job.totalAmount)}</td>
                  <td>
                    <div className='error-cell'>
                      {job.errorMessage ? (
                        <span title={job.errorMessage} className='error-text'>
                          {job.errorMessage.length > 30 
                            ? job.errorMessage.substring(0, 30) + '...' 
                            : job.errorMessage}
                        </span>
                      ) : (
                        '-'
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {jobs.length === 0 && (
        <div className='jobs-empty'>
          <p>No job logs available</p>
        </div>
      )}
    </div>
  );
}
