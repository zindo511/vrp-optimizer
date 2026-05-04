import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, Tooltip, useMap } from 'react-leaflet';
import { Info, AlertCircle, CheckCircle2, Cpu, Trash2, RefreshCw, Play, Route, Clock, Weight, MapPin, Package } from 'lucide-react';
import api from '../../api/axios';
import { useDriverLocations } from '../../hooks/useDriverLocations';
import 'leaflet/dist/leaflet.css';
import './Optimization.css';
import L from 'leaflet';
import { toast } from 'react-toastify';

// Google Maps-inspired route color palette
const ROUTE_COLORS = [
  '#4285F4', '#EA4335', '#34A853', '#FBBC04', '#9334E6',
  '#E8710A', '#1A73E8', '#D93025', '#0D652D', '#A142F4'
];

// Tile layer options (Google Maps-like)
const TILE_LAYERS = {
  map: {
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
    attribution: '&copy; <a href="https://carto.com/">CARTO</a>',
  },
  satellite: {
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: '&copy; Esri',
  },
  terrain: {
    url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
    attribution: '&copy; OpenTopoMap',
  },
};

// Map type control component (like Google Maps)
const MapTypeControl = ({ mapType, setMapType }) => (
  <div className="gmap-type-control">
    {['map', 'satellite', 'terrain'].map(type => (
      <button
        key={type}
        className={`gmap-type-btn ${mapType === type ? 'active' : ''}`}
        onClick={() => setMapType(type)}
      >
        {type === 'map' ? 'Bản đồ' : type === 'satellite' ? 'Vệ tinh' : 'Địa hình'}
      </button>
    ))}
  </div>
);

// Helper component to auto-fit map to routes
const AutoFitBounds = ({ routes, selectedRouteId, depotCoords }) => {
  const map = useMap();
  
  useEffect(() => {
    if (routes.length === 0 && !depotCoords) return;
    
    let points = [];
    if (depotCoords) {
      points.push(depotCoords);
    }
    
    if (selectedRouteId) {
      const selectedRoute = routes.find(r => r.id === selectedRouteId);
      if (selectedRoute && selectedRoute.routeStops) {
        selectedRoute.routeStops.forEach(s => points.push([s.lat, s.lng]));
      }
    } else {
      routes.forEach(r => {
        r.routeStops?.forEach(s => points.push([s.lat, s.lng]));
      });
    }

    if (points.length > 0) {
      map.fitBounds(L.latLngBounds(points), { padding: [50, 50], maxZoom: 15 });
    }
  }, [routes, selectedRouteId, depotCoords, map]);

  return null;
};

const Optimization = () => {
  const [depots, setDepots] = useState([]);
  const [selectedDepot, setSelectedDepot] = useState('');
  const [routeDate, setRouteDate] = useState(new Date().toISOString().split('T')[0]);
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [optimizationResult, setOptimizationResult] = useState(null);
  const [error, setError] = useState(null);
  const [focusedRouteId, setFocusedRouteId] = useState(null);
  const [routeGeometries, setRouteGeometries] = useState({}); 
  const [isFetchingPaths, setIsFetchingPaths] = useState(false);
  const [mapType, setMapType] = useState('map');

  // ── Algorithm Config state ──────────────────────────
  const [algorithmConfigs, setAlgorithmConfigs] = useState([]);
  const [selectedConfigId, setSelectedConfigId] = useState('');
  
  // Drag & Drop State
  const [reassigningStop, setReassigningStop] = useState(null);

  // Real-time driver locations via WebSocket
  const driverLocations = useDriverLocations();

  const fetchDepots = useCallback(async () => {
    try {
      const res = await api.get('/api/depots');
      const data = res.data.data.content || res.data.data;
      setDepots(data);
      if (data.length > 0 && !selectedDepot) setSelectedDepot(data[0].id);
    } catch (err) {
      console.error('Error fetching depots:', err);
    }
  }, [selectedDepot]);

  const fetchAlgorithmConfigs = useCallback(async () => {
    try {
      const res = await api.get('/api/algorithm-configs');
      const data = res.data.data || [];
      setAlgorithmConfigs(data);
      // Auto-select: prefer active config, fallback to first
      if (data.length > 0 && !selectedConfigId) {
        const active = data.find(c => c.isActive);
        setSelectedConfigId(active ? active.id : data[0].id);
      }
    } catch (err) {
      console.error('Error fetching algorithm configs:', err);
    }
  }, [selectedConfigId]);

  const fetchRoutePath = useCallback(async (route) => {
    if (!route.routeStops || route.routeStops.length === 0) return null;
    
    const depot = depots.find(d => d.id == selectedDepot);
    if (!depot) return null;

    // Sắp xếp theo thứ tự giao hàng (stopOrder)
    const sortedStops = [...route.routeStops].sort((a, b) => (a.stopOrder || 0) - (b.stopOrder || 0));
    
    const coords = [
      `${depot.longitude},${depot.latitude}`,
      ...sortedStops.map(s => `${s.lng},${s.lat}`),
      `${depot.longitude},${depot.latitude}`
    ].join(';');

    try {
      const response = await fetch(`https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`);
      const data = await response.json();
      if (data.code === 'Ok' && data.routes[0]) {
        return {
          coordinates: data.routes[0].geometry.coordinates.map(coord => [coord[1], coord[0]]),
          distance: data.routes[0].distance / 1000,
          duration: data.routes[0].duration / 60
        };
      }
    } catch (err) {
      console.error('OSRM Fetch Error:', err);
    }
    return null;
  }, [depots, selectedDepot]);

  const fetchAllRoutePaths = useCallback(async (allRoutes) => {
    setIsFetchingPaths(true);
    const geometries = {};
    for (const route of allRoutes) {
      const pathData = await fetchRoutePath(route);
      if (pathData) geometries[route.id] = pathData;
    }
    setRouteGeometries(geometries);
    setIsFetchingPaths(false);
  }, [fetchRoutePath]);

  const fetchExistingRoutes = useCallback(async () => {
    try {
      const res = await api.get(`/api/routes?date=${routeDate}`);
      const fetchedRoutes = res.data.data || [];
      setRoutes(fetchedRoutes);
      setFocusedRouteId(null);
      
      if (fetchedRoutes.length > 0) {
        fetchAllRoutePaths(fetchedRoutes);
      }
    } catch (err) {
      console.error('Error fetching routes:', err);
    }
  }, [routeDate, fetchAllRoutePaths]);

  useEffect(() => {
    fetchDepots();
    fetchAlgorithmConfigs();
  }, [fetchDepots, fetchAlgorithmConfigs]);

  useEffect(() => {
    fetchExistingRoutes();
  }, [fetchExistingRoutes]);

  // Refetch paths when depot changes or routes change
  useEffect(() => {
    if (routes.length > 0 && selectedDepot) {
      fetchAllRoutePaths(routes);
    }
  }, [selectedDepot, routes, fetchAllRoutePaths]);

  const handleRunOptimization = async () => {
    setLoading(true);
    setError(null);
    setOptimizationResult(null);
    try {
      // Bước 1: Submit job — server trả về jobId ngay lập tức
      const submitRes = await api.post('/api/optimization/run', {
        depotId: selectedDepot,
        routeDate: routeDate,
        algorithmConfigId: selectedConfigId
      });
      
      const jobId = submitRes.data.data?.jobId;
      if (!jobId) {
        setError('Không nhận được Job ID từ server.');
        setLoading(false);
        return;
      }

      // Bước 2: Poll trạng thái mỗi 1.5s cho đến khi hoàn thành
      const pollInterval = setInterval(async () => {
        try {
          const statusRes = await api.get(`/api/optimization/status/${jobId}`);
          const job = statusRes.data.data;
          
          if (!job) return;

          // Cập nhật progress message cho user
          if (job.status === 'RUNNING' || job.status === 'QUEUED') {
            setOptimizationResult({
              status: 'RUNNING',
              message: job.message || 'Đang xử lý...',
              progressPercent: job.progressPercent || 0,
              elapsedMs: job.elapsedMs || 0
            });
          }

          // Kết thúc khi SUCCESS hoặc FAILED
          if (job.status === 'SUCCESS') {
            clearInterval(pollInterval);
            setOptimizationResult({
              status: 'SUCCESS',
              executionTimeMs: job.elapsedMs,
              resultId: job.resultId
            });
            setLoading(false);
            fetchExistingRoutes();
          } else if (job.status === 'FAILED') {
            clearInterval(pollInterval);
            setError(job.message || 'Tối ưu hóa thất bại');
            setOptimizationResult(null);
            setLoading(false);
          }
        } catch (pollErr) {
          console.error('Poll error:', pollErr);
          clearInterval(pollInterval);
          setError('Mất kết nối khi theo dõi tiến trình.');
          setLoading(false);
        }
      }, 1500);

      // Timeout safety: tự dừng poll sau 5 phút
      setTimeout(() => {
        clearInterval(pollInterval);
        if (loading) {
          setError('Quá thời gian chờ (5 phút). Hãy kiểm tra lại kết quả.');
          setLoading(false);
        }
      }, 300000);

    } catch (err) {
      setError(err.response?.data?.message || 'Không thể gửi yêu cầu tối ưu hóa.');
      setLoading(false);
    }
  };

  const handleStopDragEnd = (e, stop, currentRouteId) => {
    setReassigningStop({ stop, fromRouteId: currentRouteId });
    e.target.setLatLng([stop.lat, stop.lng]);
  };

  const confirmReassign = async (targetRouteId) => {
    if (!reassigningStop || !targetRouteId) return;
    
    setLoading(true);
    try {
      toast.success(`Đã chuyển đơn hàng sang xe ${routes.find(r => r.id == targetRouteId).vehicleLicensePlate}`);
      setReassigningStop(null);
      fetchExistingRoutes();
    } catch (err) {
      // Handled globally
    } finally {
      setLoading(false);
    }
  };

  const handleResetData = async () => {
    if (!window.confirm("Bạn có chắc chắn muốn XÓA tất cả dữ liệu tuyến hiện tại và ĐẶT LẠI tất cả đơn hàng về PENDING để test lại?")) return;
    
    try {
      setLoading(true);
      await api.post('/api/optimization/reset');
      toast.success('Đã reset dữ liệu thử nghiệm!');
      setOptimizationResult(null);
      setError(null);
      fetchExistingRoutes(); // Sẽ trả về mảng rỗng vì đã xóa hết routes
    } catch (err) {
      toast.error('Lỗi khi reset dữ liệu');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const depotCoords = useMemo(() => {
    const depot = depots.find(d => d.id == selectedDepot);
    return depot ? [parseFloat(depot.latitude), parseFloat(depot.longitude)] : null;
  }, [depots, selectedDepot]);

  return (
    <div className="optimization-page">
      {/* ══ Top Header Bar ═══════════════════════════════ */}
      <header className="opt-header">
        <h2 className="opt-header-title">VRP Route Operations Dashboard</h2>
        <div className="opt-header-actions">
          <button className="opt-btn opt-btn-danger" onClick={handleResetData} disabled={loading}>
            <Trash2 size={15} />
            Reset dữ liệu test
          </button>
          <button className="opt-btn opt-btn-outline" onClick={fetchExistingRoutes} disabled={loading || isFetchingPaths}>
            <RefreshCw size={15} className={isFetchingPaths ? 'animate-spin' : ''} />
            {isFetchingPaths ? 'Đang lấy tọa độ...' : 'Làm mới'}
          </button>
          <button className="opt-btn opt-btn-primary" onClick={handleRunOptimization} disabled={loading || !selectedDepot}>
            <Play size={15} />
            {loading ? 'Đang xử lý...' : 'Chạy tối ưu hóa'}
          </button>
        </div>
      </header>

      {/* ══ Body: Left Panel + Map ═══════════════════════ */}
      <div className="opt-body">
        {/* ── Left Column ────────────────────────────── */}
        <div className="opt-left">
          {/* Config Card */}
          <section className="opt-config-card">
            <h3 className="opt-section-title">Cấu hình vận hành</h3>
            <div className="opt-field">
              <label>Depot</label>
              <select value={selectedDepot} onChange={e => setSelectedDepot(e.target.value)}>
                {depots.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </div>
            <div className="opt-field">
              <label>Ngày</label>
              <input type="date" value={routeDate} onChange={e => setRouteDate(e.target.value)} />
            </div>
            <div className="opt-field">
              <label>Algorithm</label>
              <select value={selectedConfigId} onChange={e => setSelectedConfigId(e.target.value)}>
                {algorithmConfigs.length === 0 && <option value="">Đang tải...</option>}
                {algorithmConfigs.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.name === 'GENETIC_ALGORITHM' ? 'Genetic Algorithm' : c.name === 'NEAREST_NEIGHBOR' ? 'Nearest Neighbor' : c.name}
                    {c.isActive ? ' ✓' : ''}
                  </option>
                ))}
              </select>
            </div>
            {(() => {
              const selectedConfig = algorithmConfigs.find(c => String(c.id) === String(selectedConfigId));
              if (!selectedConfig) return null;
              const isGA = selectedConfig.name?.toUpperCase() === 'GENETIC_ALGORITHM';
              return (
                <div className="opt-algo-info">
                  <div className="opt-algo-name">
                    <Cpu size={14} />
                    {isGA ? 'Genetic Algorithm + 2-opt' : 'Nearest Neighbor + 2-opt'}
                  </div>
                  {isGA && (
                    <div className="opt-algo-params">
                      <div className="param-row"><span>Population</span><strong>{selectedConfig.populationSize}</strong></div>
                      <div className="param-row"><span>Generations</span><strong>{selectedConfig.generations}</strong></div>
                      <div className="param-row"><span>Mutation</span><strong>{(selectedConfig.mutationRate * 100).toFixed(1)}%</strong></div>
                      <div className="param-row"><span>Crossover</span><strong>{(selectedConfig.crossoverRate * 100).toFixed(0)}%</strong></div>
                      <div className="param-row"><span>Elitism</span><strong>{selectedConfig.elitismCount}</strong></div>
                    </div>
                  )}
                  {selectedConfig.description && (
                    <p className="opt-algo-desc">{selectedConfig.description}</p>
                  )}
                </div>
              );
            })()}
          </section>

          {/* ── Status Messages ── */}
          {reassigningStop && (
            <div className="opt-reassign">
              <div className="opt-reassign-title">Di chuyển điểm dừng</div>
              <p>Chọn xe đích cho đơn hàng của <strong>{reassigningStop.stop.customerName}</strong></p>
              <select onChange={(e) => confirmReassign(e.target.value)}>
                <option value="">-- Chọn xe đích --</option>
                {routes.filter(r => r.id !== reassigningStop.fromRouteId).map(r => (
                  <option key={r.id} value={r.id}>{r.vehicleLicensePlate}</option>
                ))}
              </select>
              <button className="opt-reassign-cancel" onClick={() => setReassigningStop(null)}>Hủy bỏ</button>
            </div>
          )}

          {error && (
            <div className="opt-status error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {optimizationResult && optimizationResult.status === 'RUNNING' && (
            <div className="opt-status" style={{ background: '#e8f0fe', color: '#1a73e8', border: '1px solid #d2e3fc' }}>
              <RefreshCw size={16} className="animate-spin" />
              <div style={{ flex: 1 }}>
                <p style={{ fontWeight: 700, fontSize: '0.8125rem', margin: 0 }}>{optimizationResult.message}</p>
                <div style={{ marginTop: '6px', background: '#d2e3fc', borderRadius: '4px', height: '6px', overflow: 'hidden' }}>
                  <div style={{
                    width: `${optimizationResult.progressPercent || 0}%`,
                    height: '100%',
                    background: '#1a73e8',
                    borderRadius: '4px',
                    transition: 'width 0.5s ease'
                  }} />
                </div>
                <p style={{ fontSize: '0.7rem', opacity: 0.7, margin: '4px 0 0' }}>
                  {optimizationResult.progressPercent}% · {((optimizationResult.elapsedMs || 0) / 1000).toFixed(1)}s
                </p>
              </div>
            </div>
          )}

          {optimizationResult && optimizationResult.status === 'SUCCESS' && (
            <div className="opt-status success">
              <CheckCircle2 size={16} />
              <div>
                <p style={{ fontWeight: 700, fontSize: '0.8125rem' }}>Giải bài toán hoàn tất!</p>
                <p style={{ fontSize: '0.75rem', opacity: 0.8 }}>Thời gian: {optimizationResult.executionTimeMs}ms</p>
              </div>
            </div>
          )}

          {/* ── Route List ── */}
          <section className="opt-route-section">
            <h3 className="opt-section-title">Danh sách tuyến</h3>
            {routes.length === 0 ? (
              <div className="opt-empty">
                <Info size={40} />
                <p>Chưa có dữ liệu lộ trình. Hãy nhấn "Chạy tối ưu hóa" để bắt đầu.</p>
              </div>
            ) : (
              routes.map((route, idx) => {
                const geo = routeGeometries[route.id];
                const isFocused = focusedRouteId === route.id;
                const distanceKm = geo ? geo.distance.toFixed(1) : (route.totalDistanceKm?.toFixed(1) || '0');
                const stopsCount = route.routeStops?.length || 0;
                const durationStr = geo 
                  ? (geo.duration >= 60 
                    ? `${Math.floor(geo.duration / 60)}h ${Math.round(geo.duration % 60)}m` 
                    : `${Math.round(geo.duration)}m`)
                  : (route.totalDurationMinutes 
                    ? `${Math.floor(route.totalDurationMinutes / 60)}h ${route.totalDurationMinutes % 60}m`
                    : '—');
                const weightKg = route.totalWeightKg?.toFixed(1) || '0';

                return (
                  <div 
                    key={route.id} 
                    className={`opt-route-card ${isFocused ? 'active' : ''}`}
                    style={isFocused ? {} : { borderLeftWidth: '4px', borderLeftColor: ROUTE_COLORS[idx % ROUTE_COLORS.length] }}
                    onClick={() => setFocusedRouteId(isFocused ? null : route.id)}
                  >
                    <div className="opt-route-card-header">
                      <h4 className="opt-route-plate">{route.vehicleLicensePlate}</h4>
                      <span className={`opt-route-badge ${route.status === 'PLANNED' ? 'planned' : 'optimized'}`}>
                        {route.status === 'PLANNED' ? 'Planned' : 'Optimized'}
                      </span>
                    </div>
                    <div className="opt-route-stats">
                      <div className="opt-stat">
                        <Route size={14} className="opt-stat-icon" />
                        {distanceKm} KM
                      </div>
                      <div className="opt-stat">
                        <MapPin size={14} className="opt-stat-icon" />
                        {stopsCount} điểm
                      </div>
                      <div className="opt-stat">
                        <Clock size={14} className="opt-stat-icon" />
                        {durationStr}
                      </div>
                      <div className="opt-stat">
                        <Package size={14} className="opt-stat-icon" />
                        {weightKg} KG
                      </div>
                    </div>
                    {/* Danh sách điểm dừng mở rộng khi bấm vào tuyến */}
                    {isFocused && route.routeStops && route.routeStops.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-slate-200" style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: '8px' }} onClick={e => e.stopPropagation()}>
                        <h5 style={{ fontSize: '13px', fontWeight: 600, margin: '0 0 4px 0', color: '#475569' }}>Lộ trình giao hàng ({stopsCount} điểm)</h5>
                        {[...(route.routeStops)].sort((a,b) => (a.stopOrder||0)-(b.stopOrder||0)).map((stop) => (
                           <div key={stop.id} style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', fontSize: '12px', padding: '6px', backgroundColor: '#f8fafc', borderRadius: '6px' }}>
                             <div style={{ background: ROUTE_COLORS[idx % ROUTE_COLORS.length], color: '#fff', width: '22px', height: '22px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', flexShrink: 0, fontSize: '11px' }}>
                               {stop.stopOrder}
                             </div>
                             <div style={{ flex: 1, minWidth: 0 }}>
                               <div style={{ fontWeight: 600, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{stop.customerName}</div>
                               <div style={{ color: '#64748b', fontSize: '11px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{stop.address || 'Không có địa chỉ'}</div>
                             </div>
                           </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </section>
        </div>

        {/* ── Right Column (Map) ─────────────────────── */}
        <div className="opt-map">
          {/* ── Google Maps-style Map Type Control ── */}
          <MapTypeControl mapType={mapType} setMapType={setMapType} />
          {/* ── Floating Legend ── */}
          {routes.length > 0 && (
            <div className="opt-map-legend">
              <div className="opt-legend-title">Chú thích tuyến</div>
              {routes.map((r, idx) => (
                <div key={r.id} className={`opt-legend-item ${focusedRouteId === r.id ? 'active' : ''}`} onClick={() => setFocusedRouteId(focusedRouteId === r.id ? null : r.id)}>
                  <span className="opt-legend-color" style={{ background: ROUTE_COLORS[idx % ROUTE_COLORS.length] }}></span>
                  <span className="opt-legend-label">{r.vehicleLicensePlate}</span>
                  <span className="opt-legend-stops">{r.routeStops?.length || 0} điểm</span>
                </div>
              ))}
              {focusedRouteId && (
                <button className="opt-legend-reset" onClick={() => setFocusedRouteId(null)}>Hiện tất cả tuyến</button>
              )}
            </div>
          )}
          {/* ── Summary Stats Overlay ── */}
          {routes.length > 0 && (
            <div className="opt-map-stats">
              <div className="opt-mstat"><strong>{routes.length}</strong><span>Tuyến</span></div>
              <div className="opt-mstat"><strong>{routes.reduce((a, r) => a + (r.routeStops?.length || 0), 0)}</strong><span>Điểm giao</span></div>
              <div className="opt-mstat"><strong>{Object.values(routeGeometries).reduce((a, g) => a + g.distance, 0).toFixed(1)}</strong><span>km</span></div>
            </div>
          )}
          <MapContainer center={[21.0285, 105.8542]} zoom={12} style={{ height: '100%', width: '100%' }} zoomControl={true}>
            <TileLayer 
              key={mapType}
              url={TILE_LAYERS[mapType].url}
              attribution={TILE_LAYERS[mapType].attribution}
            />
            
            <AutoFitBounds routes={routes} selectedRouteId={focusedRouteId} depotCoords={depotCoords} />

            {/* ── Depot Marker with Pulse ── */}
            {depotCoords && (
              <Marker position={depotCoords} icon={L.divIcon({
                className: 'custom-div-icon',
                html: `<div class="depot-marker-wrap">
                  <div class="depot-pulse"></div>
                  <div style="
                    background: #EA4335;
                    color: white; width: 36px; height: 36px;
                    border-radius: 50% 50% 50% 0; transform: rotate(-45deg);
                    display: flex; align-items: center; justify-content: center;
                    border: 3px solid white;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.4);
                    position: relative; z-index: 2;
                  "><svg style="transform:rotate(45deg)" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M3 7l9-4 9 4M4 7v14M20 7v14M9 21v-6a3 3 0 0 1 6 0v6"/></svg></div>
                </div>`,
                iconSize: [36, 36],
                iconAnchor: [18, 36]
              })}>
                <Popup>
                  <div style={{ minWidth: 160 }}>
                    <h4 style={{ margin: '0 0 4px', fontSize: '0.9rem', fontWeight: 800 }}>🏭 Kho xuất phát</h4>
                    <p style={{ margin: 0, color: '#64748b', fontSize: '0.78rem' }}>{depots.find(d => d.id == selectedDepot)?.name}</p>
                    <p style={{ margin: '4px 0 0', fontSize: '0.72rem', color: '#94a3b8' }}>{depotCoords[0].toFixed(5)}, {depotCoords[1].toFixed(5)}</p>
                  </div>
                </Popup>
              </Marker>
            )}
            
            {/* ── Route Polylines & Markers ─────────────────────── */}
            {routes.map((route, rIdx) => {
              const color = ROUTE_COLORS[rIdx % ROUTE_COLORS.length];
              const isThisRouteFocused = focusedRouteId === route.id;
              const noRouteFocused = !focusedRouteId;
              
              const geo = routeGeometries[route.id];
              const sortedStops = [...(route.routeStops || [])].sort((a, b) => (a.stopOrder || 0) - (b.stopOrder || 0));
              const stopPoints = sortedStops.map(stop => [stop.lat, stop.lng]);
              const straightPath = depotCoords ? [depotCoords, ...stopPoints, depotCoords] : stopPoints;
              const roadPath = geo ? geo.coordinates : straightPath;

              // Nếu người dùng đã chọn 1 tuyến cụ thể, thì chỉ hiển thị tuyến đó (ẩn các tuyến khác đi cho khỏi rối)
              if (focusedRouteId && !isThisRouteFocused) return null;

              // Google Maps-style: semi-transparent when not focused
              const lineOpacity = isThisRouteFocused ? 1 : 0.7;
              const lineWeight = isThisRouteFocused ? 6 : 5;
              
              return (
                <React.Fragment key={route.id}>
                  {/* ─── Dark border outline (Google Maps style) ─── */}
                  {roadPath.length >= 2 && (
                    <Polyline positions={roadPath} color="#1a1a2e" weight={lineWeight + 3} opacity={lineOpacity * 0.35} lineCap="round" lineJoin="round" />
                  )}
                  {/* ─── White border under route ─── */}
                  {roadPath.length >= 2 && (
                    <Polyline positions={roadPath} color="white" weight={lineWeight + 2} opacity={lineOpacity * 0.95} lineCap="round" lineJoin="round" />
                  )}

                  {/* ─── Main route polyline (Google Maps solid feel) ── */}
                  {roadPath.length >= 2 && (
                    <Polyline 
                      positions={roadPath} 
                      color={color}
                      weight={lineWeight}
                      opacity={lineOpacity}
                      dashArray={geo ? "" : "10, 14"}
                      lineCap="round"
                      lineJoin="round"
                      eventHandlers={{
                        click: () => setFocusedRouteId(isThisRouteFocused ? null : route.id)
                      }}
                    >
                      <Tooltip sticky>
                        <div style={{ fontSize: '0.8125rem', fontFamily: "'Google Sans', 'Inter', sans-serif" }}>
                          <strong style={{ color: '#202124' }}>{route.vehicleLicensePlate}</strong>
                          <br/>
                          <span style={{ color: '#5f6368' }}>
                            {geo ? `${geo.distance.toFixed(1)} km` : `${route.totalDistanceKm?.toFixed(1) || '?'} km`}
                            {' · '}
                            {route.routeStops?.length || 0} điểm
                            {geo ? ` · ${geo.duration >= 60 
                              ? `${Math.floor(geo.duration / 60)}h ${Math.round(geo.duration % 60)}p` 
                              : `${Math.round(geo.duration)}p`}` : ''}
                          </span>
                        </div>
                      </Tooltip>
                    </Polyline>
                  )}

                  {/* ─── Dashed fallback (no OSRM data) ─── */}
                  {!geo && depotCoords && stopPoints.length > 0 && isThisRouteFocused && (
                    <>
                      <Polyline 
                        positions={[depotCoords, stopPoints[0]]}
                        color={color}
                        weight={3}
                        opacity={0.5}
                        dashArray="6, 10"
                        lineCap="round"
                      />
                      <Polyline 
                        positions={[stopPoints[stopPoints.length - 1], depotCoords]}
                        color={color}
                        weight={3}
                        opacity={0.5}
                        dashArray="6, 10"
                        lineCap="round"
                      />
                    </>
                  )}

                  {/* ─── Google Maps-style stop markers ── */}
                  {sortedStops.map((stop) => {
                    const isFocusedMarker = isThisRouteFocused;
                    const markerSize = isFocusedMarker ? 30 : (noRouteFocused ? 24 : 18);
                    const fontSize = isFocusedMarker ? '11px' : (noRouteFocused ? '10px' : '8px');
                    const markerOpacity = isThisRouteFocused ? 1 : (noRouteFocused ? 0.9 : 0.4);
                    
                    return (
                      <Marker 
                        key={stop.id} 
                        position={[stop.lat, stop.lng]}
                        draggable={isThisRouteFocused}
                        opacity={markerOpacity}
                        eventHandlers={{
                          dragend: (e) => handleStopDragEnd(e, stop, route.id),
                          click: () => { if (!isThisRouteFocused) setFocusedRouteId(route.id); }
                        }}
                        icon={L.divIcon({
                          className: 'custom-div-icon',
                          html: `<div style="
                            width: ${markerSize}px; height: ${markerSize}px;
                            position: relative;
                          ">
                            <div style="
                              background: ${color};
                              width: ${markerSize}px; height: ${markerSize}px;
                              border-radius: 50% 50% 50% 0;
                              transform: rotate(-45deg);
                              display: flex; align-items: center; justify-content: center;
                              border: 2px solid white;
                              box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                            "><span style="
                              transform: rotate(45deg);
                              color: white;
                              font-size: ${fontSize};
                              font-weight: 700;
                              font-family: 'Google Sans', Arial, sans-serif;
                            ">${stop.stopOrder || '?'}</span></div>
                          </div>`,
                          iconSize: [markerSize, markerSize],
                          iconAnchor: [markerSize / 2, markerSize]
                        })}
                      >
                        <Popup>
                          <div style={{ minWidth: '200px', fontFamily: "'Google Sans', 'Inter', sans-serif" }}>
                            <div style={{ 
                              display: 'flex', alignItems: 'center', gap: '10px', 
                              marginBottom: '10px', paddingBottom: '10px', 
                              borderBottom: '1px solid #e8eaed' 
                            }}>
                              <div style={{ 
                                background: color, color: 'white', 
                                width: 30, height: 30, borderRadius: '50%',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontWeight: 700, fontSize: '0.8rem', flexShrink: 0
                              }}>{stop.stopOrder || '?'}</div>
                              <div>
                                <h4 style={{ margin: 0, fontSize: '0.875rem', color: '#202124', fontWeight: 600 }}>{stop.customerName}</h4>
                                <p style={{ margin: 0, fontSize: '0.72rem', color: '#9aa0a6' }}>
                                  Điểm dừng #{stop.stopOrder} · Xe {route.vehicleLicensePlate}
                                </p>
                              </div>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.8rem' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#5f6368' }}>Dự kiến đến:</span>
                                <strong style={{ color: '#202124' }}>{stop.estimatedArrival ? new Date(stop.estimatedArrival).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</strong>
                              </div>
                              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#5f6368' }}>Địa chỉ:</span>
                                <strong style={{ textAlign: 'right', maxWidth: 140, fontSize: '0.72rem', color: '#202124' }}>{stop.address || '—'}</strong>
                              </div>
                            </div>
                          </div>
                        </Popup>
                      </Marker>
                    );
                  })}
                </React.Fragment>
              );
            })}

            {/* ── Real-time driver location markers ──────────── */}
            {Object.values(driverLocations).map(d => (
              <Marker
                key={`driver-${d.driverId}`}
                position={[d.lat, d.lng]}
                icon={L.divIcon({
                  className: 'custom-div-icon',
                  html: `<div style="background:#f97316;display:flex;align-items:center;justify-content:center;width:32px;height:32px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3);"><svg viewBox="0 0 24 24" width="16" height="16" fill="white"><path d="M1 3h15v13H1zm15 5h4l3 3v5h-7V8zM4 18a2 2 0 1 0 4 0 2 2 0 0 0-4 0zm11 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0z"/></svg></div>`,
                  iconSize: [32, 32],
                  iconAnchor: [16, 16],
                })}
              >
                <Popup>
                  <strong>{d.driverName}</strong>
                  <br />
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                    {d.lat.toFixed(5)}, {d.lng.toFixed(5)}
                  </span>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
          {/* ── Google Maps Directions-style focused route info ── */}
          {focusedRouteId && (() => {
            const fr = routes.find(r => r.id === focusedRouteId);
            const frIdx = routes.findIndex(r => r.id === focusedRouteId);
            const frGeo = routeGeometries[focusedRouteId];
            if (!fr) return null;
            const frColor = ROUTE_COLORS[frIdx % ROUTE_COLORS.length];
            const frDist = frGeo ? frGeo.distance.toFixed(1) : (fr.totalDistanceKm?.toFixed(1) || '0');
            const frDur = frGeo 
              ? (frGeo.duration >= 60 
                ? `${Math.floor(frGeo.duration / 60)}h ${Math.round(frGeo.duration % 60)}p`
                : `${Math.round(frGeo.duration)}p`)
              : '—';
            return (
              <div className="gmap-route-info">
                <div className="gmap-route-info-color" style={{ background: frColor }}></div>
                <div className="gmap-route-info-main">
                  <p className="gmap-route-info-vehicle">{fr.vehicleLicensePlate}</p>
                  <p className="gmap-route-info-detail">
                    <strong>{frDist} km</strong> · {frDur} · {fr.routeStops?.length || 0} điểm giao
                  </p>
                </div>
                <button className="gmap-route-info-close" onClick={() => setFocusedRouteId(null)}>✕</button>
              </div>
            );
          })()}
        </div>
      </div>
    </div>
  );
};

export default Optimization;
