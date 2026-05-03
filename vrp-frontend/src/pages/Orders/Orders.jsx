import React, { useState, useEffect, useRef } from 'react';
import { Plus, ShoppingCart, Upload, FileDown, Search, Filter, Calendar, MapPin, Loader2, X, Save } from 'lucide-react';
import api from '../../api/axios';
import DataTable from '../../components/Common/DataTable';
import './Orders.css';
import { toast } from 'react-toastify';

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [uploadLoading, setUploadLoading] = useState(false);
  const fileInputRef = useRef(null);

  const [formData, setFormData] = useState({
    customerName: '',
    customerPhone: '',
    locationId: '',
    totalWeightKg: 10,
    totalVolumeM3: 0.1,
    note: '',
    serviceTimeMinutes: 15,
    timeWindowFrom: '08:00',
    timeWindowTo: '17:00'
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [oRes, lRes] = await Promise.all([
        api.get('/api/orders'),
        api.get('/api/locations')
      ]);
      setOrders(oRes.data.data.content || []);
      setLocations(lRes.data.data.content || []);
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleOpenModal = (item = null) => {
    setEditingItem(item);
    if (item) {
      setFormData({
        customerName: item.customerName,
        customerPhone: item.customerPhone || '',
        locationId: item.locationId,
        totalWeightKg: item.totalWeightKg,
        totalVolumeM3: item.totalVolumeM3 || 0.1,
        note: item.note || '',
        serviceTimeMinutes: item.serviceTimeMinutes || 15,
        timeWindowFrom: item.timeWindowFrom || '08:00',
        timeWindowTo: item.timeWindowTo || '17:00'
      });
    } else {
      setFormData({
        customerName: '',
        customerPhone: '',
        locationId: locations.length > 0 ? locations[0].id : '',
        totalWeightKg: 10,
        totalVolumeM3: 0.1,
        note: '',
        serviceTimeMinutes: 15,
        timeWindowFrom: '08:00',
        timeWindowTo: '17:00'
      });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await api.patch(`/api/orders/${editingItem.id}`, formData);
        toast.success('Cập nhật đơn hàng thành công');
      } else {
        await api.post('/api/orders', formData);
        toast.success('Tạo đơn hàng thành công');
      }
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      // Handled globally
    }
  };

  const handleDelete = async (item) => {
    if (!window.confirm('Bạn có chắc muốn xóa đơn hàng này?')) return;
    try {
      await api.delete(`/api/orders/${item.id}`);
      toast.success('Xóa đơn hàng thành công');
      fetchData();
    } catch (err) {
      // Handled globally
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const fileFormData = new FormData();
    fileFormData.append('file', file);

    setUploadLoading(true);
    try {
      await api.post('/api/files/upload/orders', fileFormData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      fetchData();
      toast.success('Import dữ liệu thành công');
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      setUploadLoading(false);
      event.target.value = '';
    }
  };

  const columns = [
    { header: 'ID', accessor: 'id' },
    { header: 'Khách hàng', accessor: 'customerName' },
    { header: 'Địa điểm', accessor: 'locationName' },
    { 
      header: 'Khối lượng', 
      accessor: 'totalWeightKg',
      render: (val) => `${val} kg`
    },
    { 
      header: 'Thời gian', 
      accessor: 'timeWindowFrom',
      render: (val, row) => `${val} - ${row.timeWindowTo}`
    },
    { 
      header: 'Trạng thái', 
      accessor: 'status',
      render: (val) => (
        <span className={`status-badge ${val === 'PENDING' ? 'warning' : 'active'}`}>
          {val === 'PENDING' ? 'Đang chờ' : val}
        </span>
      )
    }
  ];

  return (
    <div className="orders-page">
      <div className="page-header">
        <div className="page-title">
          <h2>Quản lý Đơn hàng</h2>
          <p>Điều hành và tiếp nhận đơn hàng vận chuyển</p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <input 
            type="file" 
            style={{ display: 'none' }} 
            ref={fileInputRef} 
            onChange={handleFileUpload}
            accept=".csv,.xlsx,.xls"
          />
          <button className="btn-secondary" onClick={() => fileInputRef.current.click()} disabled={uploadLoading}>
            {uploadLoading ? <Loader2 className="animate-spin" size={20} /> : <Upload size={20} />}
            Import Excel/CSV
          </button>
          <button className="btn-primary" onClick={() => handleOpenModal()}>
            <Plus size={20} /> Tạo đơn mới
          </button>
        </div>
      </div>

      <div className="filters-bar" style={{ marginBottom: '24px', display: 'flex', gap: '16px' }}>
        <div className="search-box" style={{ flex: 1 }}>
          <Search size={18} />
          <input type="text" placeholder="Tìm kiếm đơn hàng, khách hàng..." />
        </div>
        <button className="filter-btn"><Filter size={18} /> Lọc</button>
        <button className="filter-btn"><Calendar size={18} /> Theo ngày</button>
      </div>

      <DataTable columns={columns} data={orders} loading={loading} onEdit={handleOpenModal} onDelete={handleDelete} />

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="card" style={{ width: '100%', maxWidth: '600px' }}>
            <div className="modal-header">
              <h3>{editingItem ? 'Cập nhật' : 'Tạo mới'} đơn hàng</h3>
              <button onClick={() => setIsModalOpen(false)}><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="input-group">
                  <label>Tên khách hàng</label>
                  <input 
                    type="text" 
                    required 
                    value={formData.customerName} 
                    onChange={e => setFormData({...formData, customerName: e.target.value})}
                    placeholder="VD: Nguyễn Văn A"
                  />
                </div>
                <div className="input-group">
                  <label>Số điện thoại</label>
                  <input 
                    type="text" 
                    required 
                    pattern="^[0-9]{10,11}$"
                    value={formData.customerPhone} 
                    onChange={e => setFormData({...formData, customerPhone: e.target.value})}
                    placeholder="VD: 0912345678"
                  />
                </div>
              </div>

              <div className="input-group">
                <label>Địa điểm giao hàng</label>
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
                  value={formData.locationId}
                  onChange={e => setFormData({...formData, locationId: e.target.value})}
                >
                  <option value="">-- Chọn địa điểm --</option>
                  {locations.map(loc => (
                    <option key={loc.id} value={loc.id}>{loc.name} ({loc.address})</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="input-group">
                  <label>Khối lượng (kg)</label>
                  <input 
                    type="number" 
                    step="0.01"
                    required 
                    value={formData.totalWeightKg} 
                    onChange={e => setFormData({...formData, totalWeightKg: e.target.value})}
                  />
                </div>
                <div className="input-group">
                  <label>Thể tích (m³)</label>
                  <input 
                    type="number" 
                    step="0.01"
                    required 
                    value={formData.totalVolumeM3} 
                    onChange={e => setFormData({...formData, totalVolumeM3: e.target.value})}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="input-group">
                  <label>Khung giờ từ</label>
                  <input 
                    type="time" 
                    value={formData.timeWindowFrom} 
                    onChange={e => setFormData({...formData, timeWindowFrom: e.target.value})}
                  />
                </div>
                <div className="input-group">
                  <label>Khung giờ đến</label>
                  <input 
                    type="time" 
                    value={formData.timeWindowTo} 
                    onChange={e => setFormData({...formData, timeWindowTo: e.target.value})}
                  />
                </div>
              </div>

              <div className="input-group">
                <label>Thời gian phục vụ (phút)</label>
                <input 
                  type="number" 
                  value={formData.serviceTimeMinutes} 
                  onChange={e => setFormData({...formData, serviceTimeMinutes: e.target.value})}
                />
              </div>

              <div className="input-group">
                <label>Ghi chú</label>
                <input 
                  type="text" 
                  value={formData.note} 
                  onChange={e => setFormData({...formData, note: e.target.value})}
                  placeholder="Yêu cầu đặc biệt..."
                />
              </div>

              <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>Hủy</button>
                <button type="submit" className="btn-primary">
                  <Save size={18} /> Lưu đơn hàng
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Orders;
