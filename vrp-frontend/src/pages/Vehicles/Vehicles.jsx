import React, { useState, useEffect } from 'react';
import { Plus, Truck, Settings, X, Save, Edit2, Trash2 } from 'lucide-react';
import api from '../../api/axios';
import DataTable from '../../components/Common/DataTable';
import { toast } from 'react-toastify';

const Vehicles = () => {
  const [vehicles, setVehicles] = useState([]);
  const [vehicleTypes, setVehicleTypes] = useState([]);
  const [activeTab, setActiveTab] = useState('vehicles');
  const [loading, setLoading] = useState(false);
  
  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState({});

  const fetchData = async () => {
    setLoading(true);
    try {
      const [vRes, tRes] = await Promise.all([
        api.get('/api/vehicles'),
        api.get('/api/vehicle-types')
      ]);
      setVehicles(vRes.data.data.content || []);
      setVehicleTypes(tRes.data.data.content || []);
    } catch (err) {
      console.error('Error fetching vehicle data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleOpenModal = (item = null) => {
    setEditingItem(item);
    if (activeTab === 'vehicles') {
      setFormData(item ? { 
        licensePlate: item.licensePlate, 
        vehicleTypeId: item.vehicleTypeId 
      } : { 
        licensePlate: '', 
        vehicleTypeId: vehicleTypes.length > 0 ? vehicleTypes[0].id : '' 
      });
    } else {
      setFormData(item ? { ...item } : {
        name: '',
        maxWeightKg: 1000,
        maxVolumeM3: 5,
        maxDrivingTimeMinutes: 480,
        costPerKm: 5000,
        fixedCost: 100000,
        averageSpeedKmh: 40,
        isActive: true
      });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (activeTab === 'vehicles') {
        if (editingItem) {
          await api.put(`/api/vehicles/${editingItem.id}`, formData);
          toast.success('Cập nhật xe thành công');
        } else {
          await api.post('/api/vehicles', formData);
          toast.success('Thêm xe mới thành công');
        }
      } else {
        if (editingItem) {
          await api.put(`/api/vehicle-types/${editingItem.id}`, formData);
          toast.success('Cập nhật loại xe thành công');
        } else {
          await api.post('/api/vehicle-types', formData);
          toast.success('Thêm loại xe mới thành công');
        }
      }
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      // Error handled by global axios interceptor
    }
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Bạn có chắc muốn xóa ${activeTab === 'vehicles' ? 'xe ' + item.licensePlate : 'loại xe ' + item.name}?`)) return;
    
    try {
      if (activeTab === 'vehicles') {
        await api.delete(`/api/vehicles/${item.id}`);
        toast.success('Xóa xe thành công');
      } else {
        await api.delete(`/api/vehicle-types/${item.id}`);
        toast.success('Xóa loại xe thành công');
      }
      fetchData();
    } catch (err) {
      // Error handled by global axios interceptor
    }
  };

  const vehicleColumns = [
    { header: 'Biển số', accessor: 'licensePlate' },
    { header: 'Loại xe', accessor: 'vehicleTypeName' },
    { 
      header: 'Trạng thái', 
      accessor: 'status',
      render: (val) => (
        <span className={`status-badge ${val === 'AVAILABLE' ? 'active' : 'warning'}`}>
          {val === 'AVAILABLE' ? 'Sẵn sàng' : val}
        </span>
      )
    },
    { header: 'Tài xế hiện tại', accessor: 'currentDriverName' }
  ];

  const typeColumns = [
    { header: 'Tên loại xe', accessor: 'name' },
    { header: 'Tải trọng (kg)', accessor: 'maxWeightKg' },
    { header: 'Thể tích (m³)', accessor: 'maxVolumeM3' },
    { header: 'Tốc độ (km/h)', accessor: 'averageSpeedKmh' }
  ];

  return (
    <div className="vehicles-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Quản lý Phương tiện</h2>
          <p>Quản lý đội xe và phân loại tải trọng</p>
        </div>
        <div className="flex gap-4">
          <button className="btn-primary" onClick={() => handleOpenModal()}>
            <Plus size={20} /> Thêm {activeTab === 'vehicles' ? 'xe' : 'loại xe'}
          </button>
        </div>
      </div>

      <div className="tabs-container" style={{ marginBottom: '32px', display: 'flex', gap: '16px', borderBottom: '1px solid var(--border-color)' }}>
        <button 
          className={`tab-btn ${activeTab === 'vehicles' ? 'active' : ''}`}
          style={{ 
            paddingBottom: '16px', 
            paddingLeft: '8px', 
            paddingRight: '8px', 
            fontWeight: '600', 
            background: 'transparent',
            color: activeTab === 'vehicles' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'vehicles' ? '2px solid var(--primary)' : 'none'
          }}
          onClick={() => setActiveTab('vehicles')}
        >
          Danh sách xe
        </button>
        <button 
          className={`tab-btn ${activeTab === 'types' ? 'active' : ''}`}
          style={{ 
            paddingBottom: '16px', 
            paddingLeft: '8px', 
            paddingRight: '8px', 
            fontWeight: '600', 
            background: 'transparent',
            color: activeTab === 'types' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'types' ? '2px solid var(--primary)' : 'none'
          }}
          onClick={() => setActiveTab('types')}
        >
          Phân loại xe
        </button>
      </div>

      {activeTab === 'vehicles' ? (
        <DataTable columns={vehicleColumns} data={vehicles} loading={loading} onEdit={handleOpenModal} onDelete={handleDelete} />
      ) : (
        <DataTable columns={typeColumns} data={vehicleTypes} loading={loading} onEdit={handleOpenModal} onDelete={handleDelete} />
      )}

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="card" style={{ width: '100%', maxWidth: '500px' }}>
            <div className="modal-header">
              <h3>{editingItem ? 'Cập nhật' : 'Thêm mới'} {activeTab === 'vehicles' ? 'xe' : 'loại xe'}</h3>
              <button onClick={() => setIsModalOpen(false)}><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              {activeTab === 'vehicles' ? (
                <>
                  <div className="input-group">
                    <label>Biển số xe</label>
                    <input 
                      type="text" 
                      required 
                      value={formData.licensePlate || ''} 
                      onChange={e => setFormData({...formData, licensePlate: e.target.value})}
                      placeholder="VD: 29C-123.45"
                    />
                  </div>
                  <div className="input-group">
                    <label>Loại xe</label>
                    <select 
                      style={{ 
                        width: '100%', 
                        padding: '10px', 
                        borderRadius: 'var(--radius-sm)', 
                        border: '1px solid var(--border-color)',
                        background: 'white',
                        fontSize: '0.8125rem'
                      }}
                      required
                      value={formData.vehicleTypeId || ''}
                      onChange={e => setFormData({...formData, vehicleTypeId: e.target.value})}
                    >
                      <option value="">-- Chọn loại xe --</option>
                      {vehicleTypes.map(type => (
                        <option key={type.id} value={type.id}>{type.name} ({type.maxWeightKg}kg)</option>
                      ))}
                    </select>
                  </div>
                </>
              ) : (
                <>
                  <div className="input-group">
                    <label>Tên loại xe</label>
                    <input 
                      type="text" 
                      required 
                      value={formData.name || ''} 
                      onChange={e => setFormData({...formData, name: e.target.value})}
                      placeholder="VD: Xe tải 2.5 tấn"
                    />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <div className="input-group">
                      <label>Tải trọng (kg)</label>
                      <input 
                        type="number" 
                        required 
                        value={formData.maxWeightKg || ''} 
                        onChange={e => setFormData({...formData, maxWeightKg: e.target.value})}
                      />
                    </div>
                    <div className="input-group">
                      <label>Thể tích (m³)</label>
                      <input 
                        type="number" 
                        required 
                        value={formData.maxVolumeM3 || ''} 
                        onChange={e => setFormData({...formData, maxVolumeM3: e.target.value})}
                      />
                    </div>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <div className="input-group">
                      <label>Tốc độ trung bình (km/h)</label>
                      <input 
                        type="number" 
                        required 
                        value={formData.averageSpeedKmh || ''} 
                        onChange={e => setFormData({...formData, averageSpeedKmh: e.target.value})}
                      />
                    </div>
                    <div className="input-group">
                      <label>Thời gian lái tối đa (phút)</label>
                      <input 
                        type="number" 
                        required 
                        value={formData.maxDrivingTimeMinutes || ''} 
                        onChange={e => setFormData({...formData, maxDrivingTimeMinutes: e.target.value})}
                      />
                    </div>
                  </div>
                </>
              )}
              <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>Hủy</button>
                <button type="submit" className="btn-primary">
                  <Save size={18} /> Lưu thông tin
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Vehicles;
