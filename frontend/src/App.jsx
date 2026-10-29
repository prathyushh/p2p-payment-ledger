import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';

// Header
import Header from './components/Header';

// Auth pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

// User pages
import DashboardPage from './pages/user/DashboardPage';
import WalletPage from './pages/user/WalletPage';
import BankAccountsPage from './pages/user/BankAccountsPage';
import AutoReloadPage from './pages/user/AutoReloadPage';
import SendMoneyPage from './pages/user/SendMoneyPage';
import TransactionHistoryPage from './pages/user/TransactionHistoryPage';
import NotificationsPage from './pages/user/NotificationsPage';

// Admin pages
import AdminLayout from './pages/admin/AdminLayout';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminUsers from './pages/admin/AdminUsers';
import AdminTransactions from './pages/admin/AdminTransactions';
import AdminSettlement from './pages/admin/AdminSettlement';
import AdminFraud from './pages/admin/AdminFraud';
import AdminJobs from './pages/admin/AdminJobs';
import AdminReconciliation from './pages/admin/AdminReconciliation';

function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) return <Navigate to='/login' replace />;
  if (adminOnly && !isAdmin) return <Navigate to='/dashboard' replace />;
  return children;
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  return (
    <>
      {isAuthenticated && <Header />}
      <Routes>
        <Route path='/login' element={<LoginPage />} />
        <Route path='/register' element={<RegisterPage />} />
        <Route
          path='/dashboard'
          element={<ProtectedRoute><DashboardPage /></ProtectedRoute>}
        />
        <Route
          path='/wallet'
          element={<ProtectedRoute><WalletPage /></ProtectedRoute>}
        />
        <Route
          path='/bank-accounts'
          element={<ProtectedRoute><BankAccountsPage /></ProtectedRoute>}
        />
        <Route
          path='/auto-reload'
          element={<ProtectedRoute><AutoReloadPage /></ProtectedRoute>}
        />
        <Route
          path='/send'
          element={<ProtectedRoute><SendMoneyPage /></ProtectedRoute>}
        />
        <Route
          path='/transactions'
          element={<ProtectedRoute><TransactionHistoryPage /></ProtectedRoute>}
        />
        <Route
          path='/notifications'
          element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>}
        />

        {/* Admin Routes */}
        <Route
          path='/admin'
          element={<ProtectedRoute adminOnly><Navigate to='/admin/dashboard' replace /></ProtectedRoute>}
        />
        <Route
          path='/admin/dashboard'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminDashboard />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/users'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminUsers />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/transactions'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminTransactions />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/settlement'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminSettlement />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/fraud'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminFraud />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/jobs'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminJobs />
              </AdminLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path='/admin/reconciliation'
          element={
            <ProtectedRoute adminOnly>
              <AdminLayout>
                <AdminReconciliation />
              </AdminLayout>
            </ProtectedRoute>
          }
        />

        <Route path='/' element={<Navigate to='/dashboard' replace />} />
        <Route path='*' element={<Navigate to='/dashboard' replace />} />
      </Routes>
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  );
}
