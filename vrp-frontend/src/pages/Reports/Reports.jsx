import React, { useState, useEffect } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell, AreaChart, Area
} from 'recharts';
import api from '../../api/axios';

// ── Màu cho PieChart theo status đơn hàng ─────────────────────
const STATUS_COLORS = {
  PENDING:    '#f59e0b',
  ASSIGNED:   '#2170e4',
  IN_TRANSIT: '#7c3aed',
  COMPLETED:  '#16a34a',
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
      <div className="flex-1 p-4 md:p-6 lg:p-8 flex flex-col gap-lg max-w-container-max mx-auto w-full">
        <div className="flex flex-col items-center justify-center min-h-[400px] text-on-surface-variant gap-4">
          <span className="material-symbols-outlined text-[48px] animate-spin">refresh</span>
          <p className="text-[14px]">Đang tải dữ liệu báo cáo...</p>
        </div>
      </div>
    );
  }

  // ── Error State ───────────────────────────────────────────────
  if (error) {
    return (
      <div className="flex-1 p-4 md:p-6 lg:p-8 flex flex-col gap-lg max-w-container-max mx-auto w-full">
        <div className="flex flex-col items-center justify-center min-h-[400px] text-error gap-4">
          <span className="material-symbols-outlined text-[48px]">warning</span>
          <p className="text-[14px]">{error}</p>
          <button 
            className="flex items-center gap-2 px-4 py-2 bg-secondary text-on-secondary rounded-lg font-label-md text-[13px] hover:opacity-90 transition-opacity"
            onClick={() => window.location.reload()}
          >
            Thử lại
          </button>
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

  // ── Stat cards data ───────────────────────────────────────────
  const stats = [
    {
      icon: 'moving',
      label: 'Tổng quãng đường',
      value: formatKm(summary?.totalDistanceKm),
      sub: `${summary?.totalOptimizationRuns || 0} lần chạy optimization`,
      color: '#0058be',
      bgColor: '#e5eeff'
    },
    {
      icon: 'payments',
      label: 'Tổng chi phí vận hành',
      value: `${formatVnd(summary?.totalCostVnd)} VNĐ`,
      sub: `${summary?.successfulRuns || 0}/${summary?.totalOptimizationRuns || 0} thành công`,
      color: '#dc2626',
      bgColor: '#fef2f2'
    },
    {
      icon: 'group',
      label: 'Tài xế hoạt động',
      value: `${summary?.activeDrivers || 0}/${summary?.totalDrivers || 0}`,
      sub: summary?.totalDrivers > 0
        ? `${Math.round(summary.activeDrivers / summary.totalDrivers * 100)}% sẵn sàng`
        : 'Chưa có dữ liệu',
      color: '#16a34a',
      bgColor: '#f0fdf4'
    },
    {
      icon: 'local_shipping',
      label: 'Xe sẵn sàng',
      value: `${summary?.availableVehicles || 0}/${summary?.totalVehicles || 0}`,
      sub: summary?.maintenanceVehicles > 0
        ? `${summary.maintenanceVehicles} xe đang bảo trì`
        : 'Tất cả xe sẵn sàng',
      color: '#d97706',
      bgColor: '#fffbeb'
    },
  ];

  return (
    <div className="flex-1 p-4 md:p-6 lg:p-8 flex flex-col gap-lg max-w-container-max mx-auto w-full">
      {/* ══ Page Header ═══════════════════════════════════ */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="font-headline-lg text-[32px] font-bold text-on-surface mb-2" style={{ fontFamily: 'Manrope' }}>Báo cáo & Phân tích</h2>
          <p className="font-body-md text-[14px] text-on-surface-variant">Theo dõi các chỉ số vận hành và hiệu suất đội xe — dữ liệu thực từ hệ thống</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-4 py-2 bg-surface-container border border-outline-variant/50 rounded-lg">
            <span className="material-symbols-outlined text-[18px] text-secondary">inventory_2</span>
            <span className="font-label-md text-[13px] font-semibold text-on-surface">{summary?.totalOrders || 0} đơn hàng</span>
          </div>
        </div>
      </div>

      {/* ══ Stat Cards Grid ═══════════════════════════════ */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((stat, i) => (
          <div key={i} className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-5 shadow-sm flex items-center gap-4">
            <div 
              className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: stat.bgColor, color: stat.color }}
            >
              <span className="material-symbols-outlined text-[24px]">{stat.icon}</span>
            </div>
            <div className="flex flex-col min-w-0">
              <span className="font-label-md text-[11px] font-semibold text-on-surface-variant uppercase tracking-wider">{stat.label}</span>
              <span className="text-[22px] font-bold text-on-surface leading-tight mt-0.5" style={{ fontFamily: 'Manrope' }}>{stat.value}</span>
              <span className="font-code-data text-[11px] text-on-tertiary-container font-medium mt-0.5">{stat.sub}</span>
            </div>
          </div>
        ))}
      </div>

      {/* ══ Charts Row 1: Area + Pie ══════════════════════ */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">
        {/* Optimization History Chart (3/5 width) */}
        <div className="lg:col-span-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-5 shadow-sm flex flex-col">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[20px] text-secondary">show_chart</span>
              <h3 className="text-[15px] font-bold text-on-surface" style={{ fontFamily: 'Manrope' }}>Quãng đường & Chi phí</h3>
            </div>
            <span className="font-code-data text-[11px] text-on-surface-variant bg-surface-container px-2 py-1 rounded-md">Theo lần chạy gần nhất</span>
          </div>
          <div className="flex-1 min-h-[280px]">
            {historyChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={historyChartData}>
                  <defs>
                    <linearGradient id="colorDistance" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#0058be" stopOpacity={0.15}/>
                      <stop offset="95%" stopColor="#0058be" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5eeff" />
                  <XAxis dataKey="name" stroke="#76777d" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis stroke="#76777d" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip
                    contentStyle={{ borderRadius: 8, border: '1px solid #c6c6cd', fontSize: 12 }}
                    formatter={(value, name) =>
                      name === 'Chi phí (VNĐ)' ? `${Number(value).toLocaleString('vi-VN')}đ` : `${value} km`
                    }
                  />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                  <Area type="monotone" dataKey="distance" name="Quãng đường (km)" stroke="#0058be" fillOpacity={1} fill="url(#colorDistance)" strokeWidth={2.5} />
                  <Area type="monotone" dataKey="cost" name="Chi phí (VNĐ)" stroke="#dc2626" fillOpacity={0} strokeWidth={2} strokeDasharray="5 5" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex flex-col items-center justify-center h-[280px] text-on-surface-variant gap-3">
                <span className="material-symbols-outlined text-[40px] opacity-40">schedule</span>
                <p className="text-[13px]">Chưa có dữ liệu optimization</p>
              </div>
            )}
          </div>
        </div>

        {/* Order Status Pie (2/5 width) */}
        <div className="lg:col-span-2 bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-5 shadow-sm flex flex-col">
          <div className="flex items-center gap-2 mb-5">
            <span className="material-symbols-outlined text-[20px] text-secondary">donut_small</span>
            <h3 className="text-[15px] font-bold text-on-surface" style={{ fontFamily: 'Manrope' }}>Trạng thái Đơn hàng</h3>
          </div>
          <div className="flex-1 flex items-center justify-center min-h-[280px]">
            {orderPieData.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={orderPieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={95}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {orderPieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ borderRadius: 8, border: '1px solid #c6c6cd', fontSize: 12 }}
                    formatter={(value) => `${value} đơn`} 
                  />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex flex-col items-center justify-center gap-3 text-on-surface-variant">
                <span className="material-symbols-outlined text-[40px] opacity-40">inventory_2</span>
                <p className="text-[13px]">Chưa có đơn hàng nào</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ══ Driver Performance ════════════════════════════ */}
      <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-5 shadow-sm flex flex-col">
        <div className="flex items-center gap-2 mb-5">
          <span className="material-symbols-outlined text-[20px] text-secondary">emoji_events</span>
          <h3 className="text-[15px] font-bold text-on-surface" style={{ fontFamily: 'Manrope' }}>Xếp hạng Tài xế (Tỷ lệ giao thành công)</h3>
        </div>
        <div className="min-h-[280px]">
          {driverChartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={driverChartData} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e5eeff" />
                <XAxis type="number" domain={[0, 100]} hide />
                <YAxis dataKey="name" type="category" stroke="#0b1c30" fontSize={12} width={140} tickLine={false} axisLine={false} />
                <Tooltip
                  contentStyle={{ borderRadius: 8, border: '1px solid #c6c6cd', fontSize: 12 }}
                  formatter={(value, name) => {
                    if (name === 'Tỷ lệ thành công') return `${value}%`;
                    return `${value} stop`;
                  }}
                  cursor={{fill: '#f8f9ff'}}
                />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="successRate" name="Tỷ lệ thành công" fill="#0058be" radius={[0, 6, 6, 0]} barSize={22} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex flex-col items-center justify-center h-[280px] text-on-surface-variant gap-3">
              <span className="material-symbols-outlined text-[40px] opacity-40">group</span>
              <p className="text-[13px]">Chưa có dữ liệu hiệu suất tài xế</p>
            </div>
          )}
        </div>
      </div>

      {/* ══ Optimization History Table ════════════════════ */}
      <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm overflow-hidden flex flex-col">
        <div className="flex items-center gap-2 p-5 pb-0">
          <span className="material-symbols-outlined text-[20px] text-secondary">history</span>
          <h3 className="text-[15px] font-bold text-on-surface" style={{ fontFamily: 'Manrope' }}>Lịch sử Phân tuyến</h3>
        </div>
        <div className="overflow-x-auto mt-4">
          {history.length > 0 ? (
            <table className="w-full text-left border-collapse min-w-[800px]">
              <thead>
                <tr className="bg-surface border-b border-outline-variant/50">
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">#</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Thời điểm</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Thuật toán</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Đơn hàng</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Xe</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Khoảng cách</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Chi phí</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Thời gian</th>
                  <th className="py-3 px-4 font-label-md text-[11px] text-on-surface-variant font-semibold uppercase tracking-wider">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30">
                {history.map((h, i) => (
                  <tr key={h.id || i} className="hover:bg-surface transition-colors">
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface font-medium">{h.id}</td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface-variant">{h.runDate ? new Date(h.runDate).toLocaleString('vi-VN') : '—'}</td>
                    <td className="py-3 px-4">
                      <span className="inline-block px-2 py-0.5 rounded-md bg-surface-container text-secondary font-code-data text-[11px] font-semibold">
                        {h.algorithmName || 'N/A'}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface">{h.totalOrders || 0}</td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface">{h.totalVehicles || 0}</td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface">{h.totalDistanceKm ? `${Number(h.totalDistanceKm).toFixed(2)} km` : '—'}</td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface">{h.totalCost ? `${Number(h.totalCost).toLocaleString('vi-VN')}đ` : '—'}</td>
                    <td className="py-3 px-4 font-code-data text-[12px] text-on-surface-variant">{h.executionTimeMs ? `${h.executionTimeMs}ms` : '—'}</td>
                    <td className="py-3 px-4">
                      {h.status === 'SUCCESS' ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#f0fdf4] text-[#16a34a] font-code-data text-[11px] font-semibold">
                          <span className="material-symbols-outlined text-[14px]">check_circle</span>
                          SUCCESS
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-error-container text-on-error-container font-code-data text-[11px] font-semibold">
                          <span className="material-symbols-outlined text-[14px]">error</span>
                          {h.status || 'N/A'}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-on-surface-variant gap-3">
              <span className="material-symbols-outlined text-[40px] opacity-40">schedule</span>
              <p className="text-[13px]">Chưa có lịch sử phân tuyến</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Reports;
