import axiosClient from "./axiosClient";

// Dashboard
export const getDashboardStats = () => axiosClient.get("/admin/dashboard/stats");

// Users
export const getUsers = (params) => axiosClient.get("/admin/users", { params });
export const getUser = (userId) => axiosClient.get("/admin/users/" + userId);
export const getUserTransactions = (userId, params) => axiosClient.get("/admin/users/" + userId + "/transactions", { params });
export const freezeWallet = (userId) => axiosClient.put("/admin/users/" + userId + "/wallet/freeze");
export const unfreezeWallet = (userId) => axiosClient.put("/admin/users/" + userId + "/wallet/unfreeze");

// Transactions
export const getAdminTransactions = (params) => axiosClient.get("/admin/transactions", { params });
export const getTransactionDetail = (transactionId) => axiosClient.get("/admin/transactions/" + transactionId);

// Settlement
export const getSettlementQueue = () => axiosClient.get("/admin/settlement/queue");
export const getPendingSettlements = (params) => axiosClient.get("/admin/settlement/pending", { params });
export const triggerSettlement = (transactionId) => axiosClient.post("/admin/settlement/" + transactionId + "/trigger");

// Fraud
export const blockAlert = (alertId, request) => axiosClient.put("/admin/fraud/" + alertId + "/block", request);
export const dismissAlert = (alertId, request) => axiosClient.put("/admin/fraud/" + alertId + "/dismiss", request);

// Jobs
export const getJobLogs = () => axiosClient.get("/admin/jobs");
export const getJobLog = (jobId) => axiosClient.get("/admin/jobs/" + jobId);

// Reconciliation
export const getReconciliation = () => axiosClient.get("/admin/reconciliation");
export const getReconciliationByDate = (date) => axiosClient.get("/admin/reconciliation/" + date);
