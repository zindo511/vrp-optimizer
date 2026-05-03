import React, { useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import { Plus, Warehouse, Save, X, Navigation } from 'lucide-react';
import api from '../../api/axios';
import 'leaflet/dist/leaflet.css';
import '../Locations/Locations.css'; // Reuse container layout
import { toast } from 'react-toastify';

const MapEvents = ({ isAdding, setNewDepot }) => {
  useMapEvents({
    click(e) {
      if (isAdding) {
        setNewDepot(prev => ({ ...prev, lat: e.latlng.lat, lng: e.latlng.lng }));
      }
    },
  });
  return null;
};

const Depots = () => {
  const [depots, setDepots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [newDepot, setNewDepot] = useState({ name: '', address: '', lat: 10.762622, lng: 106.660172 });

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

  const handleAddDepot = async () => {
    if (!newDepot.name || !newDepot.address) {
      toast.warning('Vui lòng nhập đầy đủ thông tin kho');
      return;
    }
    try {
      await api.post('/api/depots', newDepot);
      toast.success('Thêm kho bãi thành công');
      setIsAdding(false);
      fetchDepots();
      setNewDepot({ name: '', address: '', lat: 10.762622, lng: 106.660172 });
    } catch {
      // Handled globally
    }
  };

  const handleDeleteDepot = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa kho này?')) return;
    try {
      await api.delete(`/api/depots/${id}`);
      toast.success('Xóa kho bãi thành công');
      fetchDepots();
    } catch {
      // Handled globally
    }
  };

  return (
    <div className="locations-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Quản lý Kho bãi</h2>
          <p>Thiết lập các điểm xuất phát/kết thúc cho đội xe</p>
        </div>
        <button className="btn-primary" onClick={() => setIsAdding(!isAdding)}>
          {isAdding ? <X size={20} /> : <Plus size={20} />}
          {isAdding ? 'Hủy bỏ' : 'Thêm kho mới'}
        </button>
      </div>

      <div className="locations-grid">
        <div className="locations-list-card card">
          <div className="locations-list">
            {loading ? <p>Đang tải...</p> : depots.map(depot => (
              <div key={depot.id} className="location-item">
                <div className="location-icon" style={{background: 'rgba(236, 72, 153, 0.1)', color: 'var(--secondary)'}}>
                  <Warehouse size={18} />
                </div>
                <div className="location-info">
                  <h4>{depot.name}</h4>
                  <p>{depot.address}</p>
                </div>
                <div className="location-actions">
                  <button className="action-btn delete" onClick={() => handleDeleteDepot(depot.id)}>
                    <X size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="map-card card">
          {isAdding && (
            <div className="add-location-overlay">
              <div className="overlay-content">
                <h3>Vị trí kho mới</h3>
                <div className="form-group">
                  <input 
                    type="text" 
                    placeholder="Tên kho" 
                    value={newDepot.name}
                    onChange={e => setNewDepot({...newDepot, name: e.target.value})}
                  />
                  <input 
                    type="text" 
                    placeholder="Địa chỉ" 
                    value={newDepot.address}
                    onChange={e => setNewDepot({...newDepot, address: e.target.value})}
                  />
                </div>
                <button className="btn-save" onClick={handleAddDepot}>
                  <Save size={18} /> Lưu Kho
                </button>
              </div>
            </div>
          )}
          <MapContainer center={[10.762622, 106.660172]} zoom={13} style={{ height: '100%', width: '100%', borderRadius: '12px' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            {depots.map(depot => (
              <Marker key={depot.id} position={[parseFloat(depot.latitude), parseFloat(depot.longitude)]}>
                <Popup><strong>{depot.name}</strong><br/>{depot.address}</Popup>
              </Marker>
            ))}
            {isAdding && <Marker position={[newDepot.lat, newDepot.lng]} />}
            <MapEvents isAdding={isAdding} setNewDepot={setNewDepot} />
          </MapContainer>
        </div>
      </div>
    </div>
  );
};

export default Depots;
