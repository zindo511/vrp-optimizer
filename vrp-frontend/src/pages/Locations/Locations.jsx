import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import { Search, Plus, MapPin, Trash2, Edit2, Save, X } from 'lucide-react';
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
  const [loading, setLoading] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [editingLocation, setEditingLocation] = useState(null);
  const [formData, setFormData] = useState({ name: '', address: '', lat: 10.762622, lng: 106.660172 });

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
    setFormData({ name: '', address: '', lat: 10.762622, lng: 106.660172 });
    setIsAdding(true);
  };

  const handleOpenEdit = (loc) => {
    setEditingLocation(loc);
    setFormData({ name: loc.name, address: loc.address, lat: loc.lat, lng: loc.lng });
    setIsAdding(true);
  };

  const handleSubmit = async () => {
    if (!formData.name || !formData.address) {
      toast.warning('Vui lòng nhập đầy đủ tên và địa chỉ');
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
    if (!window.confirm(`Bạn có chắc muốn xóa địa điểm ${loc.name}?`)) return;
    try {
      await api.delete(`/api/locations/${loc.id}`);
      toast.success('Xóa địa điểm thành công');
      fetchLocations();
    } catch (err) {
      // Handled globally
    }
  };

  function MapEvents() {
    useMapEvents({
      click(e) {
        if (isAdding) {
          setFormData({ ...formData, lat: e.latlng.lat, lng: e.latlng.lng });
        }
      },
    });
    return null;
  }

  return (
    <div className="locations-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Quản lý Địa điểm</h2>
          <p>Quản lý các điểm giao hàng và tọa độ trên bản đồ</p>
        </div>
        <button className="btn-primary" onClick={() => isAdding ? setIsAdding(false) : handleOpenAdd()}>
          {isAdding ? <X size={20} /> : <Plus size={20} />}
          {isAdding ? 'Hủy bỏ' : 'Thêm địa điểm'}
        </button>
      </div>

      <div className="locations-grid">
        <div className="locations-list-card card">
          <div className="search-box">
            <Search size={18} />
            <input 
              type="text" 
              placeholder="Tìm kiếm địa điểm..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="locations-list">
            {loading ? <p style={{ textAlign: 'center', padding: '20px' }}>Đang tải...</p> : 
             locations.filter(l => l.name.toLowerCase().includes(searchTerm.toLowerCase())).map(loc => (
              <div key={loc.id} className="location-item">
                <div className="location-icon">
                  <MapPin size={18} />
                </div>
                <div className="location-info">
                  <h4>{loc.name}</h4>
                  <p title={loc.address}>{loc.address}</p>
                </div>
                <div className="location-actions">
                  <button className="action-btn edit" onClick={() => handleOpenEdit(loc)}><Edit2 size={16} /></button>
                  <button className="action-btn delete" onClick={() => handleDelete(loc)}><Trash2 size={16} /></button>
                </div>
              </div>
            ))}
            {!loading && locations.length === 0 && <p style={{ textAlign: 'center', padding: '20px', color: 'var(--text-muted)' }}>Không có địa điểm nào.</p>}
          </div>
        </div>

        <div className="map-card card">
          {isAdding && (
            <div className="add-location-overlay">
              <div className="overlay-content">
                <h3>{editingLocation ? 'Cập nhật' : 'Thêm'} địa điểm mới</h3>
                <p>Click chọn vị trí trên bản đồ hoặc nhập tọa độ</p>
                <div className="form-group">
                  <input 
                    type="text" 
                    placeholder="Tên địa điểm" 
                    value={formData.name}
                    onChange={e => setFormData({...formData, name: e.target.value})}
                  />
                  <input 
                    type="text" 
                    placeholder="Địa chỉ" 
                    value={formData.address}
                    onChange={e => setFormData({...formData, address: e.target.value})}
                  />
                </div>
                <div className="coord-row">
                  <div>Lat: {formData.lat.toFixed(6)}</div>
                  <div>Lng: {formData.lng.toFixed(6)}</div>
                </div>
                <button className="btn-save" onClick={handleSubmit}>
                  <Save size={18} /> {editingLocation ? 'Cập nhật' : 'Lưu'} địa điểm
                </button>
              </div>
            </div>
          )}
          <MapContainer center={[10.762622, 106.660172]} zoom={13} style={{ height: '100%', width: '100%', borderRadius: '12px' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {locations.map(loc => (
              <Marker key={loc.id} position={[loc.lat, loc.lng]}>
                <Popup>
                  <strong>{loc.name}</strong><br/>{loc.address}
                </Popup>
              </Marker>
            ))}
            {isAdding && (
              <Marker position={[formData.lat, formData.lng]} opacity={0.7} />
            )}
            <MapEvents />
          </MapContainer>
        </div>
      </div>
    </div>
  );
};

export default Locations;
