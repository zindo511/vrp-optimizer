import React, { useState, useEffect } from 'react';
import { Plus, Search, X, Save, Edit3, Trash2, ChevronLeft, ChevronRight, Truck } from 'lucide-react';
import api from '../../api/axios';
import './Vehicles.css';
import { toast } from 'react-toastify';

const STATUS_MAP = {
  'AVAILABLE': { label: 'Sẵn sàng', cls: 'available' },
  'IN_USE': { label: 'Đang sử dụng', cls: 'in-use' },
  'MAINTENANCE': { label: 'Bảo trì', cls: 'maintenance' },
};

const getInitials = (name) => {
  if (!name) return '?';
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
};

const Vehicles = () => {
  const [vehicles, setVehicles] = useState([]);
  const [vehicleTypes, setVehicleTypes] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [activeTab, setActiveTab] = useState('vehicles');
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  
  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState({});

  const fetchData = async () => {
    setLoading(true);
    try {
      const [vRes, tRes, dRes] = await Promise.all([
        api.get('/api/vehicles'),
        api.get('/api/vehicle-types'),
        api.get('/api/drivers')
      ]);
      setVehicles(vRes.data.data.content || []);
      setVehicleTypes(tRes.data.data.content || []);
      setDrivers(dRes.data.data.content || []);
    } catch (err) {
      console.error('Error fetching vehicle data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Xây dựng map vehicleId -> driverName từ danh sách tài xế
  // Map: vehicleId -> driver object
  const vehicleDriverMap = {};
  drivers.forEach(d => {
    if (d.vehicleId) {
      vehicleDriverMap[d.vehicleId] = { id: d.id, name: d.userFullName || d.userEmail || 'Tài xế' };
    }
  });

  const handleOpenModal = (item = null) => {
    setEditingItem(item);
    if (activeTab === 'vehicles') {
      if (item) {
        const vtId = item.vehicleType?.id || '';
        const currentDriver = vehicleDriverMap[item.id];
        setFormData({ 
          licensePlate: item.licensePlate, 
          vehicleTypeId: vtId,
          vehicleStatus: item.status || 'AVAILABLE',
          driverId: currentDriver ? String(currentDriver.id) : ''
        });
      } else {
        setFormData({ 
          licensePlate: '', 
          vehicleTypeId: vehicleTypes.length > 0 ? vehicleTypes[0].id : '',
          driverId: ''
        });
      }
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
          await api.patch(`/api/vehicles/${editingItem.id}`, {
            licensePlate: formData.licensePlate,
            vehicleTypeId: formData.vehicleTypeId ? Number(formData.vehicleTypeId) : null,
            vehicleStatus: formData.vehicleStatus || null
          });

          // Xử lý gán/bỏ gán tài xế
          const currentDriver = vehicleDriverMap[editingItem.id];
          const newDriverId = formData.driverId ? Number(formData.driverId) : null;
          const oldDriverId = currentDriver ? currentDriver.id : null;

          // Bỏ gán tài xế cũ (nếu đổi sang tài xế khác hoặc bỏ gán)
          if (oldDriverId && oldDriverId !== newDriverId) {
            await api.patch(`/api/drivers/${oldDriverId}`, { vehicleId: 0 });
          }
          // Gán tài xế mới
          if (newDriverId && newDriverId !== oldDriverId) {
            await api.patch(`/api/drivers/${newDriverId}`, { vehicleId: editingItem.id });
          }

          toast.success('Cập nhật xe thành công');
        } else {
          const res = await api.post('/api/vehicles', {
            licensePlate: formData.licensePlate,
            vehicleTypeId: formData.vehicleTypeId ? Number(formData.vehicleTypeId) : null
          });
          // Nếu tạo xe mới và chọn tài xế luôn
          if (formData.driverId && res.data?.data?.id) {
            await api.patch(`/api/drivers/${Number(formData.driverId)}`, { vehicleId: res.data.data.id });
          }
          toast.success('Thêm xe mới thành công');
        }
      } else {
        if (editingItem) {
          await api.patch(`/api/vehicle-types/${editingItem.id}`, formData);
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

  // Filter vehicles
  const filteredVehicles = vehicles.filter(v => {
    const matchSearch = (v.licensePlate || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                        (v.vehicleType?.name || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || v.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="veh-page">
      {/* ══ Page Header ═══════════════════════════════════ */}
      <div className="veh-header">
        <div className="veh-header-title">
          <h2>Quản lý Đội xe</h2>
          <p>Quản lý thông tin phương tiện, trọng tải và trạng thái vận hành.</p>
        </div>
        <button className="veh-btn-add" onClick={() => handleOpenModal()}>
          <Plus size={16} /> Thêm {activeTab === 'vehicles' ? 'xe mới' : 'loại xe'}
        </button>
      </div>

      {/* ══ Tabs ══════════════════════════════════════════ */}
      <div className="veh-tabs">
        <button className={`veh-tab ${activeTab === 'vehicles' ? 'active' : ''}`} onClick={() => setActiveTab('vehicles')}>
          Danh sách xe
        </button>
        <button className={`veh-tab ${activeTab === 'types' ? 'active' : ''}`} onClick={() => setActiveTab('types')}>
          Phân loại xe
        </button>
      </div>

      {activeTab === 'vehicles' ? (
        <>
          {/* ══ Toolbar ═══════════════════════════════════ */}
          <div className="veh-toolbar">
            <div className="veh-search">
              <Search size={16} />
              <input 
                type="text" 
                placeholder="Tìm kiếm biển số hoặc loại xe..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="veh-filter-pills">
              {[
                { key: 'ALL', label: 'Tất cả', cls: 'all' },
                { key: 'IN_USE', label: 'Đang sử dụng', cls: 'in-use' },
                { key: 'AVAILABLE', label: 'Sẵn sàng', cls: 'available' },
                { key: 'MAINTENANCE', label: 'Bảo trì', cls: 'maintenance' },
              ].map(f => (
                <button 
                  key={f.key}
                  className={`veh-pill ${f.cls} ${statusFilter === f.key ? 'active' : ''}`}
                  onClick={() => setStatusFilter(f.key)}
                >
                  {f.label}
                </button>
              ))}
            </div>
          </div>

          {/* ══ Data Table ════════════════════════════════ */}
          <div className="veh-table-card">
            <div style={{ overflowX: 'auto' }}>
              <table className="veh-table">
                <thead>
                  <tr>
                    <th>Biển số</th>
                    <th>Loại xe</th>
                    <th>Trọng tải (kg)</th>
                    <th>Trạng thái</th>
                    <th>Tài xế hiện tại</th>
                    <th style={{ textAlign: 'right' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: '#76777d' }}>Đang tải...</td></tr>
                  ) : filteredVehicles.length === 0 ? (
                    <tr><td colSpan={6}>
                      <div className="veh-empty">
                        <Truck size={40} strokeWidth={1.5} />
                        <p>Không tìm thấy xe nào.</p>
                      </div>
                    </td></tr>
                  ) : (
                    filteredVehicles.map(v => {
                      const st = STATUS_MAP[v.status] || { label: v.status || 'N/A', cls: 'available' };
                      const driverName = vehicleDriverMap[v.id] || null;
                      return (
                        <tr key={v.id}>
                          <td><span className="veh-plate">{v.licensePlate}</span></td>
                          <td>{v.vehicleType?.name || '—'}</td>
                          <td style={{ fontFamily: 'Inter, monospace', fontSize: '0.75rem', color: '#45464d' }}>
                            {v.vehicleType?.maxWeightKg ? v.vehicleType.maxWeightKg.toLocaleString() : '—'}
                          </td>
                          <td>
                            <span className={`veh-status ${st.cls}`}>
                              <span className="dot"></span>
                              {st.label}
                            </span>
                          </td>
                          <td>
                            {driverName ? (
                              <div className="veh-driver">
                                <div className="veh-driver-avatar">{getInitials(driverName.name)}</div>
                                <span>{driverName.name}</span>
                              </div>
                            ) : (
                              <span className="veh-driver-none">Không có</span>
                            )}
                          </td>
                          <td>
                            <div className="veh-actions">
                              <button className="veh-action-btn edit" onClick={() => handleOpenModal(v)}>
                                <Edit3 size={16} />
                              </button>
                              <button className="veh-action-btn delete" onClick={() => handleDelete(v)}>
                                <Trash2 size={16} />
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
            <div className="veh-pagination">
              <span className="veh-pag-info">Tổng số: {filteredVehicles.length} xe</span>
              <div className="veh-pag-btns">
                <button className="veh-pag-btn" disabled><ChevronLeft size={16} /></button>
                <button className="veh-pag-btn active">1</button>
                <button className="veh-pag-btn"><ChevronRight size={16} /></button>
              </div>
            </div>
          </div>
        </>
      ) : (
        /* ══ Vehicle Types Table ═════════════════════════ */
        <div className="veh-table-card">
          <div style={{ overflowX: 'auto' }}>
            <table className="veh-table">
              <thead>
                <tr>
                  <th>Tên loại xe</th>
                  <th>Tải trọng (kg)</th>
                  <th>Thể tích (m³)</th>
                  <th>Tốc độ TB (km/h)</th>
                  <th>Thời gian lái tối đa</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: '#76777d' }}>Đang tải...</td></tr>
                ) : vehicleTypes.length === 0 ? (
                  <tr><td colSpan={6}>
                    <div className="veh-empty"><p>Chưa có loại xe nào.</p></div>
                  </td></tr>
                ) : (
                  vehicleTypes.map(t => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 600 }}>{t.name}</td>
                      <td style={{ fontFamily: 'Inter, monospace', fontSize: '0.75rem', color: '#45464d' }}>{t.maxWeightKg?.toLocaleString()}</td>
                      <td style={{ fontFamily: 'Inter, monospace', fontSize: '0.75rem', color: '#45464d' }}>{t.maxVolumeM3}</td>
                      <td style={{ fontFamily: 'Inter, monospace', fontSize: '0.75rem', color: '#45464d' }}>{t.averageSpeedKmh}</td>
                      <td style={{ fontFamily: 'Inter, monospace', fontSize: '0.75rem', color: '#45464d' }}>{t.maxDrivingTimeMinutes} phút</td>
                      <td>
                        <div className="veh-actions" style={{ opacity: 1 }}>
                          <button className="veh-action-btn edit" onClick={() => handleOpenModal(t)}>
                            <Edit3 size={16} />
                          </button>
                          <button className="veh-action-btn delete" onClick={() => handleDelete(t)}>
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ══ Modal ═════════════════════════════════════════ */}
      {isModalOpen && (
        <div className="veh-modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="veh-modal" onClick={e => e.stopPropagation()}>
            <div className="veh-modal-header">
              <h3>{editingItem ? 'Cập nhật' : 'Thêm mới'} {activeTab === 'vehicles' ? 'xe' : 'loại xe'}</h3>
              <button className="veh-modal-close" onClick={() => setIsModalOpen(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              {activeTab === 'vehicles' ? (
                <>
                  <div className="veh-form-field">
                    <label>Biển số xe</label>
                    <input 
                      type="text" 
                      required 
                      value={formData.licensePlate || ''} 
                      onChange={e => setFormData({...formData, licensePlate: e.target.value})}
                      placeholder="VD: 29C-123.45"
                    />
                  </div>
                  <div className="veh-form-field">
                    <label>Loại xe</label>
                    <select 
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
                  {editingItem && (
                    <div className="veh-form-field">
                      <label>Trạng thái</label>
                      <select 
                        value={formData.vehicleStatus || 'AVAILABLE'}
                        onChange={e => setFormData({...formData, vehicleStatus: e.target.value})}
                      >
                        <option value="AVAILABLE">Sẵn sàng</option>
                        <option value="IN_USE">Đang sử dụng</option>
                        <option value="MAINTENANCE">Bảo trì</option>
                      </select>
                    </div>
                  )}
                  <div className="veh-form-field">
                    <label>Gán tài xế</label>
                    <select 
                      value={formData.driverId || ''}
                      onChange={e => setFormData({...formData, driverId: e.target.value})}
                    >
                      <option value="">-- Không gán tài xế --</option>
                      {drivers.map(d => {
                        // Tài xế đã gán cho xe khác (disable)
                        const isAssigned = d.vehicleId && d.vehicleId !== (editingItem?.id);
                        return (
                          <option key={d.id} value={d.id} disabled={isAssigned}>
                            {d.userFullName || d.userEmail}{isAssigned ? ` — đã gán cho ${d.vehicleLicensePlate}` : ''}
                          </option>
                        );
                      })}
                    </select>
                  </div>
                </>
              ) : (
                <>
                  <div className="veh-form-field">
                    <label>Tên loại xe</label>
                    <input 
                      type="text" 
                      required 
                      value={formData.name || ''} 
                      onChange={e => setFormData({...formData, name: e.target.value})}
                      placeholder="VD: Xe tải 2.5 tấn"
                    />
                  </div>
                  <div className="veh-form-grid">
                    <div className="veh-form-field">
                      <label>Tải trọng (kg)</label>
                      <input type="number" required value={formData.maxWeightKg || ''} onChange={e => setFormData({...formData, maxWeightKg: e.target.value})} />
                    </div>
                    <div className="veh-form-field">
                      <label>Thể tích (m³)</label>
                      <input type="number" required value={formData.maxVolumeM3 || ''} onChange={e => setFormData({...formData, maxVolumeM3: e.target.value})} />
                    </div>
                  </div>
                  <div className="veh-form-grid">
                    <div className="veh-form-field">
                      <label>Tốc độ TB (km/h)</label>
                      <input type="number" required value={formData.averageSpeedKmh || ''} onChange={e => setFormData({...formData, averageSpeedKmh: e.target.value})} />
                    </div>
                    <div className="veh-form-field">
                      <label>TG lái tối đa (phút)</label>
                      <input type="number" required value={formData.maxDrivingTimeMinutes || ''} onChange={e => setFormData({...formData, maxDrivingTimeMinutes: e.target.value})} />
                    </div>
                  </div>
                </>
              )}
              <div className="veh-form-actions">
                <button type="button" className="veh-form-cancel" onClick={() => setIsModalOpen(false)}>Hủy</button>
                <button type="submit" className="veh-form-save">
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

export default Vehicles;
