import React, { useState, useEffect } from 'react';
import { Plus, Users, Phone, ShieldCheck, X, Save, Edit2, Trash2, Truck } from 'lucide-react';
import api from '../../api/axios';
import DataTable from '../../components/Common/DataTable';
import { toast } from 'react-toastify';

const Drivers = () => {
  const [drivers, setDrivers] = useState([]);
  const [users, setUsers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState({ userId: '', licenseNumber: '', phone: '', vehicleId: '' });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [dRes, uRes, vRes] = await Promise.all([
        api.get('/api/drivers'),
        api.get('/api/users'),
        api.get('/api/vehicles')
      ]);
      setDrivers(dRes.data.data.content || []);
      setUsers(uRes.data.data || []);
      setVehicles((vRes.data.data.content || vRes.data.data || []));
    } catch (err) {
      console.error('Error fetching drivers data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Xe đã gán cho driver khác (để disable trong dropdown)
  const assignedVehicleIds = drivers
    .filter(d => d.vehicleId && (!editingItem || d.id !== editingItem.id))
    .map(d => d.vehicleId);

  const handleOpenModal = (item = null) => {
    setEditingItem(item);
    if (item) {
      setFormData({
        licenseNumber: item.licenseNumber,
        phone: item.phone,
        vehicleId: item.vehicleId || ''
      });
    } else {
      setFormData({ userId: '', licenseNumber: '', phone: '', vehicleId: '' });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await api.patch(`/api/drivers/${editingItem.id}`, {
          licenseNumber: formData.licenseNumber,
          phone: formData.phone,
          vehicleId: formData.vehicleId ? Number(formData.vehicleId) : 0
        });
        toast.success('Cập nhật tài xế thành công');
      } else {
        const payload = { ...formData };
        if (payload.vehicleId) payload.vehicleId = Number(payload.vehicleId);
        else delete payload.vehicleId;
        await api.post('/api/drivers', payload);
        toast.success('Thêm tài xế mới thành công');
      }
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      // Handled globally
    }
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Bạn có chắc muốn xóa tài xế ${item.userEmail}?`)) return;
    try {
      await api.delete(`/api/drivers/${item.id}`);
      toast.success('Xóa tài xế thành công');
      fetchData();
    } catch (err) {
      // Handled globally
    }
  };

  const columns = [
    { 
      header: 'Tên tài xế', 
      accessor: 'userEmail',
      render: (val, row) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ 
            width: '32px', 
            height: '32px', 
            borderRadius: '50%', 
            background: '#f1f5f9', 
            border: '1px solid var(--border-color)',
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center', 
            fontSize: '0.75rem', 
            fontWeight: '700',
            color: 'var(--primary)'
          }}>
            {val ? val.charAt(0).toUpperCase() : '?'}
          </div>
          <div>
            <div style={{ fontWeight: 600 }}>{row.userFullName || val}</div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{val}</div>
          </div>
        </div>
      )
    },
    { 
      header: 'Bằng lái', 
      accessor: 'licenseNumber'
    },
    { 
      header: 'Số điện thoại', 
      accessor: 'phone',
      render: (val) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)' }}>
          <Phone size={14} />
          {val}
        </div>
      )
    },
    { 
      header: 'Trạng thái', 
      accessor: 'status',
      render: (val) => (
        <span className={`status-badge ${val === 'ACTIVE' ? 'active' : val === 'INACTIVE' ? 'info' : 'warning'}`}>
          {val === 'ACTIVE' ? 'Hoạt động' : val === 'INACTIVE' ? 'Ngừng' : val}
        </span>
      )
    },
    { 
      header: 'Xe được gán', 
      accessor: 'vehicleLicensePlate',
      render: (val) => val ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Truck size={14} style={{ color: 'var(--primary)' }} />
          <span style={{ fontWeight: 600 }}>{val}</span>
        </div>
      ) : (
        <span style={{ color: '#cbd5e1', fontSize: '0.8rem' }}>Chưa gán xe</span>
      )
    }
  ];

  return (
    <div className="drivers-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Quản lý Tài xế</h2>
          <p>Danh sách đội ngũ vận hành và trạng thái làm việc</p>
        </div>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={20} /> Thêm tài xế
        </button>
      </div>

      <DataTable columns={columns} data={drivers} loading={loading} onEdit={handleOpenModal} onDelete={handleDelete} />

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="card" style={{ width: '100%', maxWidth: '500px' }}>
            <div className="modal-header">
              <h3>{editingItem ? 'Cập nhật' : 'Thêm mới'} tài xế</h3>
              <button onClick={() => setIsModalOpen(false)}><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              {!editingItem && (
                <div className="input-group">
                  <label>Chọn người dùng</label>
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
                    value={formData.userId}
                    onChange={e => setFormData({...formData, userId: e.target.value})}
                  >
                    <option value="">-- Chọn người dùng --</option>
                    {users.map(u => (
                      <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="input-group">
                <label>Số bằng lái</label>
                <input 
                  type="text" 
                  required 
                  value={formData.licenseNumber} 
                  onChange={e => setFormData({...formData, licenseNumber: e.target.value})}
                  placeholder="VD: 123456789"
                />
              </div>
              <div className="input-group">
                <label>Số điện thoại</label>
                <input 
                  type="text" 
                  required 
                  pattern="^[0-9]{10,11}$"
                  title="Số điện thoại phải có 10-11 chữ số"
                  value={formData.phone} 
                  onChange={e => setFormData({...formData, phone: e.target.value})}
                  placeholder="VD: 0912345678"
                />
              </div>
              <div className="input-group">
                <label><Truck size={13} style={{ marginRight: '4px', verticalAlign: 'middle' }} />Gán xe</label>
                <select 
                  style={{ 
                    width: '100%', 
                    padding: '10px', 
                    borderRadius: 'var(--radius-sm)', 
                    border: '1px solid var(--border-color)',
                    background: 'white',
                    fontSize: '0.8125rem'
                  }}
                  value={formData.vehicleId}
                  onChange={e => setFormData({...formData, vehicleId: e.target.value})}
                >
                  <option value="">-- Không gán xe --</option>
                  {vehicles.map(v => {
                    const isAssigned = assignedVehicleIds.includes(v.id);
                    return (
                      <option key={v.id} value={v.id} disabled={isAssigned}>
                        {v.licensePlate} ({v.vehicleType?.name || 'N/A'}){isAssigned ? ' — đã gán' : ''}
                      </option>
                    );
                  })}
                </select>
              </div>
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

export default Drivers;
