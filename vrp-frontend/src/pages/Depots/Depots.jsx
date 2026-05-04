import React, { useState, useEffect, useCallback, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from 'react-leaflet';
import { Plus, Warehouse, Save, X, Search, MapPin, Clock, Truck, Crosshair, Trash2 } from 'lucide-react';
import api from '../../api/axios';
import 'leaflet/dist/leaflet.css';
import './Depots.css';
import { toast } from 'react-toastify';

// Fix Leaflet icon issue
import L from 'leaflet';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

const MapEvents = ({ isAdding, setNewDepot, setMapCenter, setMapZoom }) => {
  useMapEvents({
    click(e) {
      if (isAdding) {
        setNewDepot(prev => ({ ...prev, lat: e.latlng.lat, lng: e.latlng.lng }));
        setMapCenter([e.latlng.lat, e.latlng.lng]);
        setMapZoom(16);
      }
    },
  });
  return null;
};

function MapController({ center, zoom }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo(center, zoom, { duration: 1.5 });
  }, [center, zoom, map]);
  return null;
}

// Hà Nội default
const DEFAULT_LAT = 21.0285;
const DEFAULT_LNG = 105.8542;

const Depots = () => {
  const [depots, setDepots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [newDepot, setNewDepot] = useState({ name: '', address: '', lat: DEFAULT_LAT, lng: DEFAULT_LNG });
  
  const [mapCenter, setMapCenter] = useState([DEFAULT_LAT, DEFAULT_LNG]);
  const [mapZoom, setMapZoom] = useState(13);
  const [selectedDepotId, setSelectedDepotId] = useState(null);

  // Geocoding
  const [isGeocoding, setIsGeocoding] = useState(false);
  const [geocodeResults, setGeocodeResults] = useState([]);
  const [showResults, setShowResults] = useState(false);
  const geocodeTimeout = useRef(null);

  const fetchDepots = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/depots');
      setDepots(response.data.data.content || []);
    } catch (err) {
      console.error('Error fetching depots:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDepots();
  }, [fetchDepots]);

  // Geocode address using Nominatim (OpenStreetMap)
  const geocodeAddress = useCallback(async (query) => {
    if (!query || query.length < 3) {
      setGeocodeResults([]);
      setShowResults(false);
      return;
    }

    setIsGeocoding(true);
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&countrycodes=vn&limit=5&addressdetails=1`
      );
      const data = await res.json();
      setGeocodeResults(data);
      setShowResults(data.length > 0);
    } catch (err) {
      console.error('Geocoding error:', err);
    } finally {
      setIsGeocoding(false);
    }
  }, []);

  const handleAddressChange = (e) => {
    const value = e.target.value;
    setNewDepot(prev => ({ ...prev, address: value }));

    if (geocodeTimeout.current) clearTimeout(geocodeTimeout.current);
    geocodeTimeout.current = setTimeout(() => {
      geocodeAddress(value);
    }, 600);
  };

  const handleSelectGeocode = (result) => {
    const lat = parseFloat(result.lat);
    const lng = parseFloat(result.lon);
    setNewDepot(prev => ({
      ...prev,
      address: result.display_name,
      lat,
      lng
    }));
    setMapCenter([lat, lng]);
    setMapZoom(17);
    setShowResults(false);
    setGeocodeResults([]);
    toast.success(`Đã xác định vị trí: ${lat.toFixed(5)}, ${lng.toFixed(5)}`);
  };

  const handleSearchAddress = async () => {
    if (!newDepot.address) {
      toast.warning('Vui lòng nhập địa chỉ');
      return;
    }
    await geocodeAddress(newDepot.address);
  };

  const handleAddDepot = async () => {
    if (!newDepot.name || !newDepot.address) {
      toast.warning('Vui lòng nhập đầy đủ thông tin kho');
      return;
    }
    try {
      const locRes = await api.post('/api/locations', {
        address: newDepot.address,
        latitude: newDepot.lat,
        longitude: newDepot.lng
      });
      const locationId = locRes.data.data.id;

      await api.post('/api/depots', { 
        name: newDepot.name, 
        locationId: locationId,
        startTime: "07:00:00",
        endTime: "18:00:00"
      });
      
      toast.success('Thêm kho bãi thành công');
      setIsAdding(false);
      fetchDepots();
      setNewDepot({ name: '', address: '', lat: DEFAULT_LAT, lng: DEFAULT_LNG });
    } catch (err) {
      console.error(err);
      toast.error('Có lỗi xảy ra khi lưu kho');
    }
  };

  const handleDeleteDepot = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa kho này?')) return;
    try {
      await api.delete(`/api/depots/${id}`);
      toast.success('Xóa kho bãi thành công');
      if (selectedDepotId === id) setSelectedDepotId(null);
      fetchDepots();
    } catch {
      // Handled globally
    }
  };

  const handleDepotClick = (depot) => {
    setSelectedDepotId(depot.id);
    setMapCenter([parseFloat(depot.latitude), parseFloat(depot.longitude)]);
    setMapZoom(16);
  };

  const selectedDepot = depots.find(d => d.id === selectedDepotId);

  return (
    <div className="depot-page">
      {/* ══ Page Header ═══════════════════════════════════ */}
      <div className="depot-header">
        <div className="depot-header-title">
          <h2>Quản lý Kho bãi</h2>
          <p>Thiết lập và quản lý các điểm xuất phát/kết thúc cho đội xe.</p>
        </div>
        <button className="depot-btn-add" onClick={() => { setIsAdding(!isAdding); setShowResults(false); setGeocodeResults([]); }}>
          {isAdding ? <><X size={16} /> Hủy bỏ</> : <><Plus size={16} /> Thêm kho mới</>}
        </button>
      </div>

      {/* ══ Split Layout ══════════════════════════════════ */}
      <div className="depot-split">
        {/* ── Left Panel ─────────────────────────────────── */}
        <div className="depot-left">
          {/* Filters */}
          <div className="depot-filters">
            <select className="depot-filter-select">
              <option>Tất cả trạng thái</option>
              <option>Hoạt động</option>
              <option>Tạm ngưng</option>
            </select>
            <select className="depot-filter-select">
              <option>Khu vực</option>
              <option>Miền Bắc</option>
              <option>Miền Nam</option>
            </select>
          </div>

          {/* Depot List */}
          <div className="depot-list">
            {loading ? (
              <div className="depot-empty"><p>Đang tải dữ liệu...</p></div>
            ) : depots.length === 0 ? (
              <div className="depot-empty">
                <Warehouse size={40} strokeWidth={1.5} />
                <p>Chưa có kho bãi nào. Hãy thêm kho mới.</p>
              </div>
            ) : (
              depots.map(depot => {
                const isSelected = selectedDepotId === depot.id;
                return (
                  <div 
                    key={depot.id}
                    className={`depot-card ${isSelected ? 'active' : ''}`}
                    onClick={() => handleDepotClick(depot)}
                  >
                    <div className="depot-card-header">
                      <h3 className="depot-card-name">{depot.name}</h3>
                      <span className="depot-card-badge active-badge">Hoạt động</span>
                    </div>
                    <div className="depot-card-address">
                      <MapPin size={15} />
                      <span>{depot.address || '—'}</span>
                    </div>
                    <div className="depot-card-stats">
                      <div className="depot-stat-block">
                        <span className="depot-stat-label">Giờ mở cửa</span>
                        <span className="depot-stat-value">
                          <Clock size={12} style={{ display: 'inline', marginRight: 4, verticalAlign: 'middle' }} />
                          {depot.startTime || '07:00'} – {depot.endTime || '18:00'}
                        </span>
                      </div>
                      <div className="depot-stat-block">
                        <span className="depot-stat-label">Tọa độ</span>
                        <span className="depot-stat-value">
                          <Crosshair size={12} style={{ display: 'inline', marginRight: 4, verticalAlign: 'middle' }} />
                          {parseFloat(depot.latitude).toFixed(4)}°, {parseFloat(depot.longitude).toFixed(4)}°
                        </span>
                      </div>
                    </div>

                    {/* Delete button (visible on hover) */}
                    <div className="depot-card-actions">
                      <button className="depot-delete-btn" onClick={(e) => { e.stopPropagation(); handleDeleteDepot(depot.id); }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* ── Right Panel (Map) ──────────────────────────── */}
        <div className="depot-map">
          {/* Add Form Overlay */}
          {isAdding && (
            <div className="depot-form-overlay">
              <h3><Warehouse size={16} /> Thêm kho mới</h3>
              <p className="form-hint">Nhập thông tin hoặc click trên bản đồ để chọn vị trí</p>
              
              <input 
                className="depot-form-input"
                type="text" 
                placeholder="Tên kho (VD: Kho Cầu Giấy)" 
                value={newDepot.name}
                onChange={e => setNewDepot({...newDepot, name: e.target.value})}
              />
              
              <div style={{ position: 'relative' }}>
                <div className="depot-form-row">
                  <input 
                    className="depot-form-input"
                    type="text" 
                    placeholder="Nhập địa chỉ để tìm tọa độ..." 
                    value={newDepot.address}
                    onChange={handleAddressChange}
                  />
                  <button className="depot-search-btn" onClick={handleSearchAddress} disabled={isGeocoding}>
                    <Search size={14} />
                    {isGeocoding ? '...' : 'Tìm'}
                  </button>
                </div>

                {showResults && geocodeResults.length > 0 && (
                  <div className="depot-geocode-dropdown">
                    {geocodeResults.map((result, idx) => (
                      <div 
                        key={idx}
                        className="depot-geocode-item"
                        onClick={() => handleSelectGeocode(result)}
                      >
                        <MapPin size={14} style={{ marginTop: 2, flexShrink: 0, color: '#0058be' }} />
                        <div>
                          <div className="depot-geocode-name">{result.display_name}</div>
                          <div className="depot-geocode-coord">
                            {parseFloat(result.lat).toFixed(5)}, {parseFloat(result.lon).toFixed(5)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="depot-form-coords">
                <Crosshair size={13} />
                Tọa độ: {newDepot.lat.toFixed(5)}, {newDepot.lng.toFixed(5)}
              </div>

              <p className="depot-form-tip">
                Gõ địa chỉ → chọn từ gợi ý, hoặc click trực tiếp trên bản đồ
              </p>

              <button className="depot-form-submit" onClick={handleAddDepot}>
                <Save size={15} /> Lưu Kho
              </button>
            </div>
          )}

          {/* Selected Depot Detail Popup */}
          {!isAdding && selectedDepot && (
            <div className="depot-detail-popup">
              <div className="depot-detail-header">
                <h4 className="depot-detail-name">{selectedDepot.name}</h4>
                <button className="depot-detail-close" onClick={() => setSelectedDepotId(null)}>
                  <X size={16} />
                </button>
              </div>
              <div className="depot-detail-info">
                <p><Warehouse size={15} /> {selectedDepot.address}</p>
                <p><Clock size={15} /> {selectedDepot.startTime || '07:00'} – {selectedDepot.endTime || '18:00'}</p>
              </div>
              <button className="depot-detail-btn">Xem chi tiết</button>
            </div>
          )}

          <MapContainer center={mapCenter} zoom={mapZoom} style={{ height: '100%', width: '100%' }}>
            <MapController center={mapCenter} zoom={mapZoom} />
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            {depots.map(depot => (
              <Marker key={depot.id} position={[parseFloat(depot.latitude), parseFloat(depot.longitude)]}>
                <Popup>
                  <div style={{ minWidth: '160px' }}>
                    <h4 style={{ margin: '0 0 4px', fontWeight: 700, fontSize: '0.85rem' }}>{depot.name}</h4>
                    <p style={{ margin: 0, fontSize: '0.8rem', color: '#45464d' }}>{depot.address}</p>
                  </div>
                </Popup>
              </Marker>
            ))}
            {isAdding && <Marker position={[newDepot.lat, newDepot.lng]} />}
            <MapEvents isAdding={isAdding} setNewDepot={setNewDepot} setMapCenter={setMapCenter} setMapZoom={setMapZoom} />
          </MapContainer>
        </div>
      </div>
    </div>
  );
};

export default Depots;
