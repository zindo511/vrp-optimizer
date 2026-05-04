import React, { useState, useEffect } from 'react';
import { Plus, Search, Phone, UserPlus, X, Save, Edit3, Trash2, Truck, ChevronLeft, ChevronRight } from 'lucide-react';
import api from '../../api/axios';
import './Drivers.css';
import { toast } from 'react-toastify';

const STATUS_MAP = {
  'ACTIVE': { label: 'Hoạt động', cls: 'active' },
  'INACTIVE': { label: 'Ngừng việc', cls: 'inactive' }
};

const getInitials = (name) => {
  if (!name) return '?';
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
};

const Drivers = () => {
  const [drivers, setDrivers] = useState([]);
  const [users, setUsers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  
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
        licenseNumber: item.licenseNumber || '',
        phone: item.phone || '',
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
    if (!window.confirm(`Bạn có chắc muốn xóa tài xế ${item.userFullName || item.userEmail}?`)) return;
    try {
      await api.delete(`/api/drivers/${item.id}`);
      toast.success('Xóa tài xế thành công');
      fetchData();
    } catch (err) {
      // Handled globally
    }
  };

  // Filter logic
  const filteredDrivers = drivers.filter(d => {
    const searchString = `${d.userFullName || ''} ${d.userEmail || ''} ${d.licenseNumber || ''} ${d.phone || ''} ${d.vehicleLicensePlate || ''}`.toLowerCase();
    const matchSearch = searchString.includes(searchTerm.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || d.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="drv-page">
      {/* ══ Page Header ═══════════════════════════════════ */}
      <div className="drv-header">
        <div className="drv-header-title">
          <h2>Quản lý Tài xế</h2>
          <p>Danh sách đội ngũ vận hành và trạng thái làm việc</p>
        </div>
        <button className="drv-btn-add" onClick={() => handleOpenModal()}>
          <UserPlus size={16} /> Thêm tài xế mới
        </button>
      </div>

      {/* ══ Toolbar ═══════════════════════════════════════ */}
      <div className="drv-toolbar">
        <div className="drv-filter-pills">
          <button 
            className={`drv-pill ${statusFilter === 'ALL' ? 'all active' : 'inactive-pill'}`}
            onClick={() => setStatusFilter('ALL')}
          >
            Tất cả
          </button>
          <button 
            className={`drv-pill ${statusFilter === 'ACTIVE' ? 'active-pill active' : 'inactive-pill'}`}
            onClick={() => setStatusFilter('ACTIVE')}
          >
            Đang hoạt động
          </button>
          <button 
            className={`drv-pill inactive-pill`}
            onClick={() => toast.info('Chức năng đang phát triển')}
          >
            Nghỉ phép
          </button>
          <button 
            className={`drv-pill ${statusFilter === 'INACTIVE' ? 'active-pill active' : 'inactive-pill'}`}
            onClick={() => setStatusFilter('INACTIVE')}
          >
            Đã khóa
          </button>
        </div>
        <div className="drv-search">
          <Search size={16} />
          <input 
            type="text" 
            placeholder="Tìm tên, SĐT, bằng lái..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* ══ Data Table ════════════════════════════════════ */}
      <div className="drv-table-card">
        <div style={{ overflowX: 'auto' }}>
          <table className="drv-table">
            <thead>
              <tr>
                <th>Họ tên & Ảnh</th>
                <th>Số bằng lái</th>
                <th>Số điện thoại</th>
                <th>Trạng thái</th>
                <th>Xe được gán</th>
                <th style={{ textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: '#76777d' }}>Đang tải...</td></tr>
              ) : filteredDrivers.length === 0 ? (
                <tr><td colSpan={6}>
                  <div className="drv-empty">
                    <UserPlus size={40} strokeWidth={1.5} />
                    <p>Không tìm thấy tài xế nào.</p>
                  </div>
                </td></tr>
              ) : (
                filteredDrivers.map(d => {
                  const st = STATUS_MAP[d.status] || { label: d.status || 'N/A', cls: 'inactive' };
                  return (
                    <tr key={d.id}>
                      <td>
                        <div className="drv-identity">
                          {d.avatarUrl ? (
                            <img src={d.avatarUrl} alt={d.userFullName} className="drv-avatar" />
                          ) : (
                            <div className="drv-avatar-placeholder">{getInitials(d.userFullName || d.userEmail)}</div>
                          )}
                          <div>
                            <div className="drv-name">{d.userFullName || d.userEmail || 'Chưa cập nhật'}</div>
                            <div className="drv-id">ID: TX-{10000 + d.id}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ fontFamily: 'Inter, monospace', color: '#45464d' }}>{d.licenseNumber || '—'}</td>
                      <td style={{ fontFamily: 'Inter, monospace', color: '#45464d' }}>{d.phone || '—'}</td>
                      <td>
                        <span className={`drv-status ${st.cls}`}>
                          <span className="dot"></span>
                          {st.label}
                        </span>
                      </td>
                      <td>
                        {d.vehicleLicensePlate ? (
                          <div className="drv-vehicle">
                            <Truck size={16} />
                            <span className="drv-vehicle-plate">{d.vehicleLicensePlate}</span>
                          </div>
                        ) : (
                          <span className="drv-vehicle-none">Chưa gán xe</span>
                        )}
                      </td>
                      <td>
                        <div className="drv-actions">
                          <button className="drv-action-btn edit" onClick={() => handleOpenModal(d)} title="Sửa">
                            <Edit3 size={18} />
                          </button>
                          <button className="drv-action-btn delete" onClick={() => handleDelete(d)} title="Xóa">
                            <Trash2 size={18} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        <div className="drv-pagination">
          <span className="drv-pag-info">
            {filteredDrivers.length === 0 ? 'Không có tài xế' : `Tổng số: ${filteredDrivers.length} tài xế`}
          </span>
          <div className="drv-pag-btns">
            <button className="drv-pag-btn nav" disabled><ChevronLeft size={16} /></button>
            <button className="drv-pag-btn active">1</button>
            <button className="drv-pag-btn nav"><ChevronRight size={16} /></button>
          </div>
        </div>
      </div>

      {/* ══ Modal ═════════════════════════════════════════ */}
      {isModalOpen && (
        <div className="drv-modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="drv-modal" onClick={e => e.stopPropagation()}>
            <div className="drv-modal-header">
              <h3>{editingItem ? 'Cập nhật' : 'Thêm mới'} tài xế</h3>
              <button className="drv-modal-close" onClick={() => setIsModalOpen(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              {!editingItem && (
                <div className="drv-form-field">
                  <label>Chọn người dùng</label>
                  <select 
                    required
                    value={formData.userId}
                    onChange={e => setFormData({...formData, userId: e.target.value})}
                  >
                    <option value="">-- Chọn người dùng --</option>
                    {users.map(u => (
                      <option key={u.id} value={u.id}>{u.fullName || u.email}</option>
                    ))}
                  </select>
                </div>
              )}
              
              <div className="drv-form-field">
                <label>Số bằng lái</label>
                <input 
                  type="text" 
                  required 
                  value={formData.licenseNumber} 
                  onChange={e => setFormData({...formData, licenseNumber: e.target.value})}
                  placeholder="VD: 123456789"
                />
              </div>
              
              <div className="drv-form-field">
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
              
              <div className="drv-form-field">
                <label><Truck size={14} /> Gán xe</label>
                <select 
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
              
              <div className="drv-form-actions">
                <button type="button" className="drv-form-cancel" onClick={() => setIsModalOpen(false)}>Hủy</button>
                <button type="submit" className="drv-form-save">
                  <Save size={15} /> Lưu thông tin
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
