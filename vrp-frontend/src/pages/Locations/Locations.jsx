import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from 'react-leaflet';
import { Search, Plus, X, MapPin, Crosshair, Edit3, Trash2, ChevronLeft, ChevronRight, Save } from 'lucide-react';
import api from '../../api/axios';
import 'leaflet/dist/leaflet.css';
import './Locations.css';
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

const Locations = () => {
  const [locations, setLocations] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showStatusDropdown, setShowStatusDropdown] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [editingLocation, setEditingLocation] = useState(null);
  const [formData, setFormData] = useState({ address: '', latitude: 10.762622, longitude: 106.660172 });
  const [mapCenter, setMapCenter] = useState([10.762622, 106.660172]);
  const [mapZoom, setMapZoom] = useState(13);
  const [selectedLocationId, setSelectedLocationId] = useState(null);

  const fetchLocations = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/locations');
      setLocations(response.data.data.content || []);
    } catch (err) {
      console.error('Error fetching locations:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLocations();
  }, []);

  const handleOpenAdd = () => {
    setEditingLocation(null);
    setFormData({ address: '', latitude: 10.762622, longitude: 106.660172 });
    setIsAdding(true);
  };

  const handleOpenEdit = (loc) => {
    setEditingLocation(loc);
    setFormData({ address: loc.address, latitude: loc.latitude, longitude: loc.longitude });
    setIsAdding(true);
  };

  const handleSubmit = async () => {
    if (!formData.address) {
      toast.warning('Vui lòng nhập đầy đủ địa chỉ');
      return;
    }

    try {
      if (editingLocation) {
        await api.patch(`/api/locations/${editingLocation.id}`, formData);
        toast.success('Cập nhật địa điểm thành công');
      } else {
        await api.post('/api/locations', formData);
        toast.success('Thêm địa điểm thành công');
      }
      setIsAdding(false);
      fetchLocations();
    } catch (err) {
      // Handled globally
    }
  };

  const handleDelete = async (loc) => {
    if (!window.confirm(`Bạn có chắc muốn xóa địa điểm ${loc.address}?`)) return;
    try {
      await api.delete(`/api/locations/${loc.id}`);
      toast.success('Xóa địa điểm thành công');
      fetchLocations();
    } catch (err) {
      // Handled globally
    }
  };

  const handleLocationClick = (loc) => {
    setSelectedLocationId(loc.id);
    setMapCenter([loc.latitude, loc.longitude]);
    setMapZoom(16);
  };

  function MapEvents() {
    useMapEvents({
      click(e) {
        if (isAdding) {
          setFormData({ ...formData, latitude: e.latlng.lat, longitude: e.latlng.lng });
        }
      },
    });
    return null;
  }

  function MapController({ center, zoom }) {
    const map = useMap();
    useEffect(() => {
      map.flyTo(center, zoom, { duration: 1.5 });
    }, [center, zoom, map]);
    return null;
  }

  const filteredLocations = locations.filter(l => {
    const matchesSearch = (l.address || '').toLowerCase().includes(searchTerm.toLowerCase());
    return matchesSearch;
  });

  return (
    <div className="loc-page">
      {/* ══ Page Header ═══════════════════════════════════ */}
      <div className="loc-header">
        <div className="loc-header-title">
          <h2>Quản lý Địa điểm</h2>
          <p>Quản lý danh sách các điểm giao nhận, kho bãi và tọa độ GPS.</p>
        </div>
        <button className="loc-btn-add" onClick={() => isAdding ? setIsAdding(false) : handleOpenAdd()}>
          {isAdding ? (
            <><X size={16} /> Hủy bỏ</>
          ) : (
            <><Plus size={16} /> Thêm địa điểm mới</>
          )}
        </button>
      </div>

      {/* ══ Bento Grid ════════════════════════════════════ */}
      <div className="loc-bento">
        {/* ── Left Panel: Location List ─────────────────── */}
        <div className="loc-list-card">
          {/* Search & Filter */}
          <div className="loc-search-area">
            <div className="loc-search-input">
              <Search size={16} className="search-icon" />
              <input 
                type="text" 
                placeholder="Tìm kiếm theo địa chỉ, tọa độ..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>

          {/* Location List */}
          <div className="loc-list-scroll">
            {loading ? (
              <div className="loc-empty"><p>Đang tải dữ liệu...</p></div>
            ) : filteredLocations.length === 0 ? (
              <div className="loc-empty">
                <MapPin size={40} strokeWidth={1.5} />
                <p>Không có địa điểm nào.</p>
              </div>
            ) : (
              filteredLocations.map(loc => (
                <div 
                  key={loc.id} 
                  className={`loc-item ${selectedLocationId === loc.id ? 'active' : ''}`}
                  onClick={() => handleLocationClick(loc)}
                >
                  <div className="loc-item-header">
                    <span className="loc-item-name">Địa điểm #{loc.id}</span>
                    <span className="loc-item-badge active-badge">Hoạt động</span>
                  </div>
                  <div className="loc-item-address">
                    <MapPin size={14} className="addr-icon" />
                    <span>{loc.address || '—'}</span>
                  </div>
                  <div className="loc-item-coords">
                    <Crosshair size={13} className="coord-icon" />
                    {parseFloat(loc.latitude).toFixed(4)}° N, {parseFloat(loc.longitude).toFixed(4)}° E
                  </div>
                  <div className="loc-item-actions">
                    <button className="loc-action-btn edit" onClick={(e) => { e.stopPropagation(); handleOpenEdit(loc); }}>
                      <Edit3 size={13} /> Sửa
                    </button>
                    <button className="loc-action-btn delete" onClick={(e) => { e.stopPropagation(); handleDelete(loc); }}>
                      <Trash2 size={13} /> Xóa
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Pagination */}
          <div className="loc-pagination">
            <span className="loc-pagination-info">Hiển thị 1-{Math.min(filteredLocations.length, 10)} của {filteredLocations.length}</span>
            <div className="loc-pagination-btns">
              <button className="loc-page-btn" disabled><ChevronLeft size={16} /></button>
              <button className="loc-page-btn"><ChevronRight size={16} /></button>
            </div>
          </div>
        </div>

        {/* ── Right Panel: Map ──────────────────────────── */}
        <div className="loc-map-card">
          {/* Add/Edit Form Overlay */}
          {isAdding && (
            <div className="loc-form-overlay">
              <h3>{editingLocation ? <><Edit3 size={15} /> Cập nhật</> : <><Plus size={15} /> Thêm</>} địa điểm</h3>
              <p className="form-hint">Click chọn vị trí trên bản đồ hoặc nhập tọa độ</p>
              <input 
                type="text" 
                placeholder="Nhập địa chỉ..." 
                value={formData.address}
                onChange={e => setFormData({...formData, address: e.target.value})}
              />
              <div className="loc-form-coords">
                <span>Lat: {formData.latitude.toFixed(6)}</span>
                <span>Lng: {formData.longitude.toFixed(6)}</span>
              </div>
              <button className="loc-form-submit" onClick={handleSubmit}>
                <Save size={15} /> {editingLocation ? 'Cập nhật' : 'Lưu'} địa điểm
              </button>
            </div>
          )}

          <MapContainer center={mapCenter} zoom={mapZoom} style={{ height: '100%', width: '100%' }}>
            <MapController center={mapCenter} zoom={mapZoom} />
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {locations.map(loc => (
              <Marker key={loc.id} position={[loc.latitude, loc.longitude]}>
                <Popup>
                  <div style={{ minWidth: '160px' }}>
                    <h4 style={{ margin: '0 0 4px', fontWeight: 700, fontSize: '0.85rem' }}>Địa điểm #{loc.id}</h4>
                    <p style={{ margin: 0, fontSize: '0.8rem', color: '#45464d' }}>{loc.address}</p>
                    <p style={{ margin: '4px 0 0', fontSize: '0.72rem', color: '#76777d' }}>
                      {parseFloat(loc.latitude).toFixed(4)}° N, {parseFloat(loc.longitude).toFixed(4)}° E
                    </p>
                  </div>
                </Popup>
              </Marker>
            ))}
            {isAdding && (
              <Marker position={[formData.latitude, formData.longitude]} opacity={0.7} />
            )}
            <MapEvents />
          </MapContainer>
        </div>
      </div>
    </div>
  );
};

export default Locations;
