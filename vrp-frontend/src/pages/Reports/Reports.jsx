import React, { useState, useEffect } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell, AreaChart, Area
} from 'recharts';
import { TrendingUp, Truck, Users, DollarSign, Calendar, Loader, AlertTriangle, Clock, CheckCircle, Package } from 'lucide-react';
import api from '../../api/axios';
import './Reports.css';

// ── Màu cho PieChart theo status đơn hàng ─────────────────────
const STATUS_COLORS = {
  PENDING:    '#f59e0b',
  ASSIGNED:   '#3b82f6',
  IN_TRANSIT: '#8b5cf6',
  COMPLETED:  '#22c55e',
  FAILED:     '#ef4444',
};

const STATUS_LABELS = {
  PENDING:    'Chờ xử lý',
  ASSIGNED:   'Đã phân tuyến',
  IN_TRANSIT: 'Đang giao',
  COMPLETED:  'Đã giao',
  FAILED:     'Thất bại',
};

// ── Format số tiền VNĐ ────────────────────────────────────────
const formatVnd = (value) => {
  if (value == null) return '0đ';
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)} Tr`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(0)}K`;
  return `${value}`;
};

const formatKm = (value) => {
  if (value == null) return '0 km';
  return `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} km`;
};

const Reports = () => {
  const [summary, setSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        setLoading(true);
        const [summaryRes, historyRes, driversRes] = await Promise.all([
          api.get('/api/reports/summary'),
          api.get('/api/optimization/history'),
          api.get('/api/reports/drivers'),
        ]);
        setSummary(summaryRes.data.data);
        setHistory(historyRes.data.data || []);
        setDrivers(driversRes.data.data || []);
      } catch (err) {
        console.error('Reports fetch error:', err);
        setError('Không thể tải dữ liệu báo cáo. Vui lòng thử lại.');
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  // ── Loading State ─────────────────────────────────────────────
  if (loading) {
    return (
      <div className="reports-page">
        <div className="reports-loading">
          <Loader size={40} className="spinner" />
          <p>Đang tải dữ liệu báo cáo...</p>
        </div>
      </div>
    );
  }

  // ── Error State ───────────────────────────────────────────────
  if (error) {
    return (
      <div className="reports-page">
        <div className="reports-error">
          <AlertTriangle size={40} />
          <p>{error}</p>
          <button className="btn-primary" onClick={() => window.location.reload()}>Thử lại</button>
        </div>
      </div>
    );
  }

  // ── Chuẩn bị data cho charts ──────────────────────────────────
  const orderPieData = summary?.ordersByStatus
    ? Object.entries(summary.ordersByStatus)
        .filter(([, count]) => count > 0)
        .map(([status, count]) => ({
          name: STATUS_LABELS[status] || status,
          value: count,
          color: STATUS_COLORS[status] || '#94a3b8',
        }))
    : [];

  const historyChartData = [...history]
    .reverse()
    .slice(-10)
    .map((h, i) => ({
      name: h.runDate ? new Date(h.runDate).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : `#${i + 1}`,
      distance: h.totalDistanceKm ? Number(h.totalDistanceKm) : 0,
      cost: h.totalCost ? Number(h.totalCost) : 0,
    }));

  const driverChartData = drivers
    .filter(d => d.totalStops > 0)
    .sort((a, b) => b.successRate - a.successRate)
    .slice(0, 8)
    .map(d => ({
      name: d.driverName?.length > 20 ? d.driverName.substring(0, 20) + '...' : (d.driverName || 'N/A'),
      successRate: d.successRate,
      completedStops: d.completedStops,
      failedStops: d.failedStops,
    }));

  // ── Stat cards ────────────────────────────────────────────────
  const stats = [
    {
      icon: <TrendingUp size={24} />,
      label: 'Tổng quãng đường',
      value: formatKm(summary?.totalDistanceKm),
      sub: `${summary?.totalOptimizationRuns || 0} lần chạy optimization`,
      color: '#2563eb'
    },
    {
      icon: <DollarSign size={24} />,
      label: 'Tổng chi phí vận hành',
      value: `${formatVnd(summary?.totalCostVnd)} VNĐ`,
      sub: `${summary?.successfulRuns || 0}/${summary?.totalOptimizationRuns || 0} thành công`,
      color: '#dc2626'
    },
    {
      icon: <Users size={24} />,
      label: 'Tài xế hoạt động',
      value: `${summary?.activeDrivers || 0}/${summary?.totalDrivers || 0}`,
      sub: summary?.totalDrivers > 0
        ? `${Math.round(summary.activeDrivers / summary.totalDrivers * 100)}% sẵn sàng`
        : 'Chưa có dữ liệu',
      color: '#16a34a'
    },
    {
      icon: <Truck size={24} />,
      label: 'Xe sẵn sàng',
      value: `${summary?.availableVehicles || 0}/${summary?.totalVehicles || 0}`,
      sub: summary?.maintenanceVehicles > 0
        ? `${summary.maintenanceVehicles} xe đang bảo trì`
        : 'Tất cả xe sẵn sàng',
      color: '#d97706'
    },
  ];

  return (
    <div className="reports-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Báo cáo & Phân tích</h2>
          <p>Theo dõi các chỉ số vận hành và hiệu suất đội xe — dữ liệu thực từ hệ thống</p>
        </div>
        <div className="header-actions">
          <div className="total-orders-badge">
            <Package size={16} />
            <span>{summary?.totalOrders || 0} đơn hàng</span>
          </div>
        </div>
      </div>

      {/* ── Stat Cards ───────────────────────────────────────── */}
      <div className="stats-grid">
        {stats.map((stat, i) => (
          <div key={i} className="stat-card card">
            <div className="stat-icon" style={{ backgroundColor: `${stat.color}15`, color: stat.color }}>
              {stat.icon}
            </div>
            <div className="stat-content">
              <span className="stat-label">{stat.label}</span>
              <h3 className="stat-value">{stat.value}</h3>
              <span className="stat-sub">{stat.sub}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="reports-grid">
        {/* ── Optimization History Chart ────────────────────── */}
        <div className="chart-card card">
          <div className="chart-header">
            <h3>Quãng đường & Chi phí (theo lần chạy gần nhất)</h3>
          </div>
          <div className="chart-container">
            {historyChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={historyChartData}>
                  <defs>
                    <linearGradient id="colorDistance" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#2563eb" stopOpacity={0.1}/>
                      <stop offset="95%" stopColor="#2563eb" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="name" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                  <Tooltip
                    formatter={(value, name) =>
                      name === 'Chi phí (VNĐ)' ? `${Number(value).toLocaleString('vi-VN')}đ` : `${value} km`
                    }
                  />
                  <Legend />
                  <Area type="monotone" dataKey="distance" name="Quãng đường (km)" stroke="#2563eb" fillOpacity={1} fill="url(#colorDistance)" strokeWidth={2} />
                  <Area type="monotone" dataKey="cost" name="Chi phí (VNĐ)" stroke="#dc2626" fillOpacity={0} strokeWidth={2} strokeDasharray="5 5" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="chart-empty">
                <Clock size={32} />
                <p>Chưa có dữ liệu optimization</p>
              </div>
            )}
          </div>
        </div>

        {/* ── Order Status Pie ─────────────────────────────── */}
        <div className="chart-card card">
          <div className="chart-header">
            <h3>Trạng thái Đơn hàng</h3>
          </div>
          <div className="chart-container flex items-center justify-center">
            {orderPieData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={orderPieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {orderPieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => `${value} đơn`} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="chart-empty">
                <Package size={32} />
                <p>Chưa có đơn hàng nào</p>
              </div>
            )}
          </div>
        </div>

        {/* ── Driver Performance ───────────────────────────── */}
        <div className="chart-card card col-span-2">
          <div className="chart-header">
            <h3>Xếp hạng Tài xế (Tỷ lệ giao thành công %)</h3>
          </div>
          <div className="chart-container">
            {driverChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={driverChartData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
                  <XAxis type="number" domain={[0, 100]} hide />
                  <YAxis dataKey="name" type="category" stroke="#1e293b" fontSize={12} width={140} tickLine={false} axisLine={false} />
                  <Tooltip
                    formatter={(value, name) => {
                      if (name === 'Tỷ lệ thành công') return `${value}%`;
                      return `${value} stop`;
                    }}
                    cursor={{fill: '#f8fafc'}}
                  />
                  <Legend />
                  <Bar dataKey="successRate" name="Tỷ lệ thành công" fill="#2563eb" radius={[0, 4, 4, 0]} barSize={20} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="chart-empty">
                <Users size={32} />
                <p>Chưa có dữ liệu hiệu suất tài xế</p>
              </div>
            )}
          </div>
        </div>

        {/* ── Optimization History Table ───────────────────── */}
        <div className="chart-card card col-span-2">
          <div className="chart-header">
            <h3>Lịch sử Phân tuyến</h3>
          </div>
          <div className="history-table-wrapper">
            {history.length > 0 ? (
              <table className="history-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Thời điểm</th>
                    <th>Thuật toán</th>
                    <th>Đơn hàng</th>
                    <th>Xe</th>
                    <th>Khoảng cách</th>
                    <th>Chi phí</th>
                    <th>Thời gian</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h, i) => (
                    <tr key={h.id || i}>
                      <td>{h.id}</td>
                      <td>{h.runDate ? new Date(h.runDate).toLocaleString('vi-VN') : '—'}</td>
                      <td><span className="algo-badge">{h.algorithmName || 'N/A'}</span></td>
                      <td>{h.totalOrders || 0}</td>
                      <td>{h.totalVehicles || 0}</td>
                      <td>{h.totalDistanceKm ? `${Number(h.totalDistanceKm).toFixed(2)} km` : '—'}</td>
                      <td>{h.totalCost ? `${Number(h.totalCost).toLocaleString('vi-VN')}đ` : '—'}</td>
                      <td>{h.executionTimeMs ? `${h.executionTimeMs}ms` : '—'}</td>
                      <td>
                        <span className={`status-badge status-${(h.status || '').toLowerCase()}`}>
                          {h.status === 'SUCCESS' ? <CheckCircle size={12} /> : <AlertTriangle size={12} />}
                          {h.status || 'N/A'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="chart-empty">
                <Clock size={32} />
                <p>Chưa có lịch sử phân tuyến</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Reports;
