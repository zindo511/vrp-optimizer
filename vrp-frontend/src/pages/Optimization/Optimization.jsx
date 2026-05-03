import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, Tooltip, useMap } from 'react-leaflet';
import { Play, RotateCcw, Truck, ChevronRight, Info, AlertCircle, CheckCircle2, Settings2, Cpu } from 'lucide-react';
import api from '../../api/axios';
import { useDriverLocations } from '../../hooks/useDriverLocations';
import 'leaflet/dist/leaflet.css';
import './Optimization.css';
import L from 'leaflet';
import { toast } from 'react-toastify';

// Predefined colors for different routes
const ROUTE_COLORS = [
  '#2563eb', '#dc2626', '#16a34a', '#d97706', '#7c3aed', 
  '#0891b2', '#4f46e5', '#be185d', '#15803d', '#9333ea'
];

// Helper component to auto-fit map to routes
const AutoFitBounds = ({ routes, selectedRouteId }) => {
  const map = useMap();
  
  useEffect(() => {
    if (routes.length === 0) return;
    
    let points = [];
    if (selectedRouteId) {
      const selectedRoute = routes.find(r => r.id === selectedRouteId);
      if (selectedRoute && selectedRoute.routeStops) {
        points = selectedRoute.routeStops.map(s => [s.lat, s.lng]);
      }
    } else {
      routes.forEach(r => {
        r.routeStops?.forEach(s => points.push([s.lat, s.lng]));
      });
    }

    if (points.length > 0) {
      map.fitBounds(L.latLngBounds(points), { padding: [50, 50], maxZoom: 15 });
    }
  }, [routes, selectedRouteId, map]);

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

    const coords = [
      `${depot.longitude},${depot.latitude}`,
      ...route.routeStops.map(s => `${s.lng},${s.lat}`),
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
      const response = await api.post('/api/optimization/run', {
        depotId: selectedDepot,
        routeDate: routeDate,
        algorithmConfigId: selectedConfigId
      });
      
      setOptimizationResult(response.data.data);
      setTimeout(fetchExistingRoutes, 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể thực hiện tối ưu hóa.');
    } finally {
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

  const depotCoords = useMemo(() => {
    const depot = depots.find(d => d.id == selectedDepot);
    return depot ? [parseFloat(depot.latitude), parseFloat(depot.longitude)] : null;
  }, [depots, selectedDepot]);

  return (
    <div className="optimization-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Điều hành lộ trình</h2>
          <p>Tối ưu hóa và phân bổ đơn hàng cho đội xe</p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn-secondary" onClick={fetchExistingRoutes} disabled={loading || isFetchingPaths}>
            <RotateCcw size={18} className={isFetchingPaths ? 'animate-spin' : ''} /> 
            {isFetchingPaths ? 'Đang lấy tọa độ...' : 'Làm mới'}
          </button>
          <button 
            className="btn-primary" 
            onClick={handleRunOptimization}
            disabled={loading || !selectedDepot}
          >
            {loading ? <span className="animate-spin" style={{ marginRight: '8px' }}>⏳</span> : <Play size={18} />}
            {loading ? 'Đang xử lý...' : 'Chạy tối ưu hóa'}
          </button>
        </div>
      </div>

      <div className="optimization-layout">
        <aside className="optimization-panel card">
          <div className="panel-section">
            <h3 className="section-title">Cấu hình vận hành</h3>
            <div className="input-group">
              <label>Kho xuất phát</label>
              <select 
                style={{ width: '100%', padding: '10px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', background: 'white', fontSize: '0.8125rem' }}
                value={selectedDepot} 
                onChange={e => setSelectedDepot(e.target.value)}
              >
                {depots.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </div>
            <div className="input-group">
              <label>Ngày vận hành</label>
              <input type="date" value={routeDate} onChange={e => setRouteDate(e.target.value)} />
            </div>
            <div className="input-group">
              <label><Settings2 size={13} style={{ marginRight: '4px', verticalAlign: 'middle' }} />Thuật toán</label>
              <select 
                style={{ width: '100%', padding: '10px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', background: 'white', fontSize: '0.8125rem' }}
                value={selectedConfigId} 
                onChange={e => setSelectedConfigId(e.target.value)}
              >
                {algorithmConfigs.length === 0 && <option value="">Đang tải...</option>}
                {algorithmConfigs.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.name === 'GENETIC_ALGORITHM' ? '🧬 Genetic Algorithm' : c.name === 'NEAREST_NEIGHBOR' ? '📍 Nearest Neighbor' : c.name}
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
                <div className="config-info-box">
                  <div className="config-info-row">
                    <Cpu size={13} />
                    <span className="config-solver-name">
                      {isGA ? 'Genetic Algorithm + 2-opt' : 'Nearest Neighbor + 2-opt'}
                    </span>
                  </div>
                  {isGA && (
                    <div className="config-ga-params">
                      <div className="param-item"><span>Population</span><strong>{selectedConfig.populationSize}</strong></div>
                      <div className="param-item"><span>Generations</span><strong>{selectedConfig.generations}</strong></div>
                      <div className="param-item"><span>Mutation</span><strong>{(selectedConfig.mutationRate * 100).toFixed(1)}%</strong></div>
                      <div className="param-item"><span>Crossover</span><strong>{(selectedConfig.crossoverRate * 100).toFixed(0)}%</strong></div>
                      <div className="param-item"><span>Elitism</span><strong>{selectedConfig.elitismCount}</strong></div>
                    </div>
                  )}
                  {selectedConfig.description && (
                    <p className="config-description">{selectedConfig.description}</p>
                  )}
                </div>
              );
            })()}
          </div>

          {reassigningStop && (
            <div className="status-message info" style={{ backgroundColor: '#eff6ff', border: '1px solid #dbeafe', color: '#1e40af', padding: '16px', margin: '16px 20px', borderRadius: 'var(--radius-sm)' }}>
              <div style={{ fontWeight: '700', marginBottom: '8px', fontSize: '0.875rem' }}>Di chuyển điểm dừng</div>
              <p style={{ fontSize: '0.75rem', marginBottom: '12px' }}>Chọn xe đích cho đơn hàng của <strong>{reassigningStop.stop.customerName}</strong></p>
              <select 
                style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #bfdbfe', marginBottom: '12px', fontSize: '0.8125rem' }}
                onChange={(e) => confirmReassign(e.target.value)}
              >
                <option value="">-- Chọn xe đích --</option>
                {routes.filter(r => r.id !== reassigningStop.fromRouteId).map(r => (
                  <option key={r.id} value={r.id}>{r.vehicleLicensePlate}</option>
                ))}
              </select>
              <button style={{ background: 'transparent', color: '#ef4444', fontSize: '0.75rem', fontWeight: '700' }} onClick={() => setReassigningStop(null)}>Hủy bỏ</button>
            </div>
          )}

          {error && (
            <div className="status-message error" style={{ margin: '0 20px 16px' }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {optimizationResult && (
            <div className="status-message success" style={{ margin: '0 20px 16px' }}>
              <CheckCircle2 size={18} />
              <div>
                <p><strong>Giải bài toán hoàn tất!</strong></p>
                <p style={{ fontSize: '0.7rem' }}>Thời gian: {optimizationResult.executionTimeMs}ms</p>
              </div>
            </div>
          )}

          <div className="panel-section" style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <h3 className="section-title">Danh sách tuyến ({routes.length})</h3>
            <div className="route-list" style={{ flex: 1, overflowY: 'auto' }}>
              {routes.length === 0 ? (
                <div className="empty-routes">
                  <Info size={40} />
                  <p>Chưa có dữ liệu lộ trình.</p>
                </div>
              ) : (
                routes.map((route, idx) => {
                  const geo = routeGeometries[route.id];
                  return (
                    <div 
                      key={route.id} 
                      className={`route-item ${focusedRouteId === route.id ? 'active' : ''}`} 
                      style={{ 
                        borderLeftColor: ROUTE_COLORS[idx % ROUTE_COLORS.length],
                        backgroundColor: focusedRouteId === route.id ? '#eff6ff' : 'white',
                        borderColor: focusedRouteId === route.id ? 'var(--primary)' : 'var(--border-color)'
                      }}
                      onClick={() => setFocusedRouteId(focusedRouteId === route.id ? null : route.id)}
                    >
                      <div className="route-main">
                        <div className="route-vehicle">
                          <Truck size={14} />
                          <span>{route.vehicleLicensePlate}</span>
                        </div>
                        <div className="route-stats">
                          <strong style={{ color: 'var(--primary)' }}>
                            {geo ? geo.distance.toFixed(1) : (route.totalDistanceKm?.toFixed(1) || 0)} km
                          </strong>
                          <span className="dot">•</span>
                          <span>{route.routeStops?.length || 0} điểm</span>
                          {geo && (
                            <>
                              <span className="dot">•</span>
                              <span>{Math.round(geo.duration)} phút</span>
                            </>
                          )}
                        </div>
                      </div>
                      <ChevronRight size={16} style={{ color: '#cbd5e1' }} />
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </aside>

        <div className="optimization-map card">
          <MapContainer center={[10.7626, 106.6602]} zoom={12} style={{ height: '100%', width: '100%' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            
            <AutoFitBounds routes={routes} selectedRouteId={focusedRouteId} />

            {depotCoords && (
              <Marker position={depotCoords} icon={L.divIcon({
                className: 'custom-div-icon',
                html: `<div style="background-color: #1e293b; color: white;" class="marker-pin"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M3 7l9-4 9 4M4 7v14M20 7v14M9 21v-6a3 3 0 0 1 6 0v6"/></svg></div>`,
                iconSize: [28, 28],
                iconAnchor: [14, 28]
              })}>
                <Popup><strong>Kho: {depots.find(d => d.id == selectedDepot)?.name}</strong></Popup>
              </Marker>
            )}
            
            {routes.map((route, rIdx) => {
              const color = ROUTE_COLORS[rIdx % ROUTE_COLORS.length];
              const isFocused = !focusedRouteId || focusedRouteId === route.id;
              
              const geo = routeGeometries[route.id];
              const stopPoints = route.routeStops?.map(stop => [stop.lat, stop.lng]) || [];
              const straightPath = depotCoords ? [depotCoords, ...stopPoints, depotCoords] : stopPoints;
              const roadPath = geo ? geo.coordinates : straightPath;
              
              return (
                <React.Fragment key={route.id}>
                  <Polyline 
                    positions={roadPath} 
                    color={color} 
                    weight={focusedRouteId === route.id ? 6 : 3} 
                    opacity={isFocused ? 0.8 : 0.1}
                    dashArray={geo ? "" : "5, 10"}
                  >
                    <Tooltip sticky>Tuyến {route.vehicleLicensePlate}</Tooltip>
                  </Polyline>
                  
                  {isFocused && route.routeStops?.map((stop, sIdx) => (
                    <Marker 
                      key={stop.id} 
                      position={[stop.lat, stop.lng]}
                      draggable={true}
                      eventHandlers={{
                        dragend: (e) => handleStopDragEnd(e, stop, route.id)
                      }}
                      icon={L.divIcon({
                        className: 'custom-div-icon',
                        html: `<div style="background-color: ${color};" class="marker-pin"><span>${sIdx + 1}</span></div>`,
                        iconSize: [24, 24],
                        iconAnchor: [12, 24]
                      })}
                    >
                      <Popup>
                        <div style={{ minWidth: '160px' }}>
                          <h4 style={{ margin: '0 0 4px', fontSize: '0.875rem', color: color }}>Điểm dừng {sIdx + 1}</h4>
                          <p style={{ margin: '0 0 4px', fontWeight: '700' }}>{stop.customerName}</p>
                          <p style={{ margin: '0 0 8px', fontSize: '0.7rem', color: '#64748b' }}>Kéo Marker để đổi xe</p>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.75rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: '4px' }}>
                              <span>Dự kiến:</span>
                              <strong>{new Date(stop.estimatedArrival).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</strong>
                            </div>
                          </div>
                        </div>
                      </Popup>
                    </Marker>
                  ))}
                </React.Fragment>
              );
            })}
            {/* Real-time driver location markers */}
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
        </div>
      </div>
    </div>
  );
};

export default Optimization;
