import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, Package, Clock, ChevronRight, Loader2 } from 'lucide-react';
import api from '../../api/axios';
import './Driver.css';

const STATUS_LABEL = {
  WAITING: 'Chờ giao',
  ARRIVED: 'Đã đến',
  COMPLETED: 'Hoàn thành',
  FAILED: 'Thất bại',
};

const STATUS_COLOR = {
  WAITING: '#64748b',
  ARRIVED: '#2563eb',
  COMPLETED: '#16a34a',
  FAILED: '#dc2626',
};

const ROUTE_STATUS_LABEL = {
  PLANNED: 'Chưa bắt đầu',
  IN_PROGRESS: 'Đang giao',
  COMPLETED: 'Hoàn thành',
};

const DriverRoutes = () => {
  const [route, setRoute] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api.get('/api/drivers/my-route')
      .then(res => setRoute(res.data.data))
      .catch(err => setError(err.response?.data?.message || 'Không tìm thấy lộ trình hôm nay'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="driver-center">
        <Loader2 className="spin" size={32} color="var(--primary)" />
        <p>Đang tải lộ trình...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="driver-center driver-error">
        <Package size={40} color="#94a3b8" />
        <p>{error}</p>
      </div>
    );
  }

  const doneCount = route.stops.filter(
    s => s.status === 'COMPLETED' || s.status === 'FAILED'
  ).length;
  const progress = route.totalStops > 0 ? (doneCount / route.totalStops) * 100 : 0;

  return (
    <div className="driver-page">
      <div className="route-summary-card">
        <div className="route-summary-top">
          <div>
            <h2>Lộ trình hôm nay</h2>
            <p className="route-date">{new Date(route.routeDate).toLocaleDateString('vi-VN')}</p>
          </div>
          <span className={`badge badge-${route.routeStatus.toLowerCase()}`}>
            {ROUTE_STATUS_LABEL[route.routeStatus] ?? route.routeStatus}
          </span>
        </div>

        <div className="route-meta-row">
          <div className="meta-chip">
            <Package size={14} />
            <span>{route.totalStops} điểm</span>
          </div>
          <div className="meta-chip">
            <MapPin size={14} />
            <span>{route.totalDistanceKm?.toFixed(1)} km</span>
          </div>
          <div className="meta-chip">
            <span className="text-muted">Xe:</span>
            <span>{route.vehicleLicensePlate}</span>
          </div>
        </div>

        <div className="progress-row">
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
          <span className="progress-text">{doneCount}/{route.totalStops}</span>
        </div>
      </div>

      <div className="stops-list">
        <h3>Danh sách điểm giao</h3>
        {route.stops.map(stop => (
          <div
            key={stop.id}
            className="stop-card"
            onClick={() => navigate(`/driver/stops/${stop.id}`, { state: { stop } })}
          >
            <div className="stop-order-badge">{stop.stopOrder}</div>
            <div className="stop-info">
              <p className="stop-customer">{stop.customerName || 'Khách hàng'}</p>
              <p className="stop-address">{stop.address}</p>
              {stop.estimatedArrival && (
                <p className="stop-time">
                  <Clock size={12} />
                  {new Date(stop.estimatedArrival).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                </p>
              )}
            </div>
            <div className="stop-right">
              <span className="stop-status-text" style={{ color: STATUS_COLOR[stop.status] }}>
                {STATUS_LABEL[stop.status]}
              </span>
              <ChevronRight size={16} color="#94a3b8" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default DriverRoutes;
