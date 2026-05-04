import React, { useState, useEffect, useRef } from 'react';
import api from '../../api/axios';
import { toast } from 'react-toastify';

const STATUS_MAP = {
  'PENDING': { label: 'Chờ xử lý', cls: 'bg-surface-variant text-on-surface-variant', dotCls: 'bg-on-surface-variant' },
  'ASSIGNED': { label: 'Đã gán', cls: 'bg-secondary-container text-on-secondary-container', dotCls: 'bg-secondary' },
  'DELIVERING': { label: 'Đang giao', cls: 'bg-tertiary-container text-on-tertiary-container', dotCls: 'bg-tertiary' },
  'COMPLETED': { label: 'Hoàn thành', cls: 'bg-primary-container text-on-primary-container', dotCls: 'bg-primary' }
};

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeStatus, setActiveStatus] = useState('ALL');
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

  const filteredOrders = orders.filter(order => {
    const matchesSearch = 
      order.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (order.customerPhone && order.customerPhone.includes(searchTerm)) ||
      `ORD-${order.id}`.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStatus = activeStatus === 'ALL' || order.status === activeStatus;
    
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="flex-1 p-4 md:p-6 lg:p-8 flex flex-col gap-lg max-w-container-max mx-auto w-full" style={{ fontFamily: 'var(--font-body-md)' }}>
      {/* Page Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="font-headline-lg text-[32px] font-bold text-on-surface mb-2">Quản lý Đơn hàng</h2>
          <p className="font-body-md text-[16px] text-on-surface-variant m-0">Xem, lọc và quản lý danh sách đơn hàng cần giao.</p>
        </div>
        <div className="flex flex-wrap gap-3 w-full md:w-auto">
          <input 
            type="file" 
            style={{ display: 'none' }} 
            ref={fileInputRef} 
            onChange={handleFileUpload}
            accept=".csv,.xlsx,.xls"
          />
          <button 
            className="flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 border border-secondary text-secondary rounded-lg font-label-md hover:bg-secondary/5 transition-colors bg-surface-container-lowest shadow-sm"
            onClick={() => fileInputRef.current.click()}
            disabled={uploadLoading}
          >
            {uploadLoading ? (
              <span className="material-symbols-outlined text-[20px] animate-spin">refresh</span>
            ) : (
              <span className="material-symbols-outlined text-[20px]" data-icon="upload_file">upload_file</span>
            )}
            Nhập Excel/CSV
          </button>
          <button 
            className="flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 bg-secondary text-on-secondary rounded-lg font-label-md hover:opacity-90 transition-opacity shadow-sm"
            onClick={() => handleOpenModal()}
          >
            <span className="material-symbols-outlined text-[20px]" data-icon="add">add</span>
            Tạo đơn mới
          </button>
        </div>
      </div>

      {/* Filters Area */}
      <div className="bg-surface-container-lowest p-4 rounded-xl border border-outline-variant/50 shadow-sm flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
          {/* Status Tabs */}
          <div className="flex overflow-x-auto w-full pb-2 sm:pb-0 hide-scrollbar gap-2">
            <button 
              className={`px-4 py-1.5 rounded-full font-label-md text-[13px] whitespace-nowrap transition-colors ${activeStatus === 'ALL' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant border border-outline-variant/50 hover:bg-surface-variant'}`}
              onClick={() => setActiveStatus('ALL')}
            >
              Tất cả ({orders.length})
            </button>
            <button 
              className={`px-4 py-1.5 rounded-full font-label-md text-[13px] whitespace-nowrap transition-colors ${activeStatus === 'PENDING' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant border border-outline-variant/50 hover:bg-surface-variant'}`}
              onClick={() => setActiveStatus('PENDING')}
            >
              Chờ xử lý ({orders.filter(o => o.status === 'PENDING').length})
            </button>
            <button 
              className={`px-4 py-1.5 rounded-full font-label-md text-[13px] whitespace-nowrap transition-colors ${activeStatus === 'ASSIGNED' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant border border-outline-variant/50 hover:bg-surface-variant'}`}
              onClick={() => setActiveStatus('ASSIGNED')}
            >
              Đã gán ({orders.filter(o => o.status === 'ASSIGNED').length})
            </button>
            <button 
              className={`px-4 py-1.5 rounded-full font-label-md text-[13px] whitespace-nowrap transition-colors ${activeStatus === 'DELIVERING' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant border border-outline-variant/50 hover:bg-surface-variant'}`}
              onClick={() => setActiveStatus('DELIVERING')}
            >
              Đang giao ({orders.filter(o => o.status === 'DELIVERING').length})
            </button>
            <button 
              className={`px-4 py-1.5 rounded-full font-label-md text-[13px] whitespace-nowrap transition-colors ${activeStatus === 'COMPLETED' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant border border-outline-variant/50 hover:bg-surface-variant'}`}
              onClick={() => setActiveStatus('COMPLETED')}
            >
              Hoàn thành ({orders.filter(o => o.status === 'COMPLETED').length})
            </button>
          </div>
          
          {/* Additional Filter/Sort */}
          <div className="flex gap-2 w-full sm:w-auto">
            <button className="flex items-center gap-2 px-3 py-1.5 border border-outline-variant/50 rounded-lg text-on-surface-variant hover:bg-surface-variant transition-colors font-label-md text-[13px] w-full sm:w-auto justify-center">
              <span className="material-symbols-outlined text-[18px]" data-icon="filter_list">filter_list</span>
              Bộ lọc
            </button>
            <button className="flex items-center gap-2 px-3 py-1.5 border border-outline-variant/50 rounded-lg text-on-surface-variant hover:bg-surface-variant transition-colors font-label-md text-[13px] w-full sm:w-auto justify-center">
              <span className="material-symbols-outlined text-[18px]" data-icon="calendar_today">calendar_today</span>
              Hôm nay
            </button>
          </div>
        </div>

        {/* Search Bar (Added inside filter card matching the style logic) */}
        <div className="relative w-full">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[20px]" data-icon="search">search</span>
          <input 
            className="w-full pl-10 pr-4 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] font-body-md focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none transition-all" 
            placeholder="Tìm kiếm mã đơn, khách hàng, số điện thoại..." 
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Data Table Card */}
      <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/50 shadow-sm overflow-hidden flex-1 flex flex-col">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr className="bg-surface border-b border-outline-variant/50">
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold w-12 text-center">
                  <input className="rounded border-outline-variant text-secondary focus:ring-secondary h-4 w-4" type="checkbox"/>
                </th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold">Mã Đơn</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold">Khách hàng</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold w-64">Địa điểm</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold text-right">Khối lượng</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold">Thời gian</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold">Trạng thái</th>
                <th className="py-3 px-4 font-label-md text-[13px] text-on-surface-variant font-semibold text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/30">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-on-surface-variant">
                    <span className="material-symbols-outlined animate-spin text-[32px]">refresh</span>
                    <p className="mt-2 text-[14px]">Đang tải dữ liệu...</p>
                  </td>
                </tr>
              ) : filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-on-surface-variant">
                    <span className="material-symbols-outlined text-[48px] opacity-50">inbox</span>
                    <p className="mt-2 text-[14px]">Không tìm thấy đơn hàng nào.</p>
                  </td>
                </tr>
              ) : (
                filteredOrders.map(order => {
                  const statusInfo = STATUS_MAP[order.status] || { label: order.status, cls: 'bg-surface-variant text-on-surface-variant', dotCls: 'bg-on-surface-variant' };
                  
                  return (
                    <tr key={order.id} className="hover:bg-surface transition-colors group">
                      <td className="py-3 px-4 text-center">
                        <input className="rounded border-outline-variant text-secondary focus:ring-secondary h-4 w-4" type="checkbox"/>
                      </td>
                      <td className="py-3 px-4 font-code-data text-[12px] font-medium text-on-surface">ORD-{order.id}</td>
                      <td className="py-3 px-4">
                        <div className="font-body-md text-[14px] font-medium text-on-surface">{order.customerName}</div>
                        <div className="font-code-data text-[12px] text-on-surface-variant">{order.customerPhone || '—'}</div>
                      </td>
                      <td className="py-3 px-4 font-body-md text-[14px] text-on-surface-variant truncate max-w-[200px]" title={order.locationAddress || order.locationName}>
                        {order.locationAddress || order.locationName}
                      </td>
                      <td className="py-3 px-4 font-code-data text-[12px] font-medium text-on-surface text-right">{order.totalWeightKg} kg</td>
                      <td className="py-3 px-4 font-code-data text-[12px] text-on-surface-variant">{order.timeWindowFrom} - {order.timeWindowTo}</td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2 py-1 rounded-full ${statusInfo.cls} font-code-data text-[12px] font-semibold`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${statusInfo.dotCls} mr-1.5`}></span>
                          {statusInfo.label}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button 
                            className="p-1.5 text-on-surface-variant hover:text-secondary hover:bg-secondary-container/20 rounded-md transition-colors" 
                            title="Chỉnh sửa"
                            onClick={() => handleOpenModal(order)}
                          >
                            <span className="material-symbols-outlined text-[20px]" data-icon="edit">edit</span>
                          </button>
                          <button 
                            className="p-1.5 text-on-surface-variant hover:text-error hover:bg-error-container rounded-md transition-colors" 
                            title="Xóa"
                            onClick={() => handleDelete(order)}
                          >
                            <span className="material-symbols-outlined text-[20px]" data-icon="delete">delete</span>
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
        <div className="mt-auto border-t border-outline-variant/50 px-4 py-3 flex items-center justify-between bg-surface-container-lowest">
          <span className="font-code-data text-[12px] text-on-surface-variant">
            Tổng số: {filteredOrders.length} đơn hàng
          </span>
          <div className="flex items-center gap-1">
            <button className="p-1 rounded text-on-surface-variant hover:bg-surface-variant disabled:opacity-50" disabled>
              <span className="material-symbols-outlined text-[20px]" data-icon="chevron_left">chevron_left</span>
            </button>
            <button className="w-8 h-8 rounded bg-secondary text-on-secondary font-code-data text-[12px] font-medium flex items-center justify-center">1</button>
            <button className="p-1 rounded text-on-surface-variant hover:bg-surface-variant">
              <span className="material-symbols-outlined text-[20px]" data-icon="chevron_right">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      {/* Modal Overlay using Tailwind classes */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={() => setIsModalOpen(false)}>
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl w-full max-w-2xl shadow-xl overflow-hidden" onClick={e => e.stopPropagation()}>
            <div className="flex justify-between items-center p-6 border-b border-outline-variant/30">
              <h3 className="font-headline-lg text-[20px] font-bold text-on-surface">{editingItem ? 'Cập nhật' : 'Tạo mới'} đơn hàng</h3>
              <button className="w-8 h-8 rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-surface-variant transition-colors" onClick={() => setIsModalOpen(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-6">
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Tên khách hàng</label>
                    <input 
                      type="text" 
                      required 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.customerName} 
                      onChange={e => setFormData({...formData, customerName: e.target.value})}
                      placeholder="VD: Nguyễn Văn A"
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Số điện thoại</label>
                    <input 
                      type="text" 
                      required 
                      pattern="^[0-9]{10,11}$"
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.customerPhone} 
                      onChange={e => setFormData({...formData, customerPhone: e.target.value})}
                      placeholder="VD: 0912345678"
                    />
                  </div>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="font-label-md text-[13px] font-semibold text-on-surface">Địa điểm giao hàng</label>
                  <select 
                    required
                    className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                    value={formData.locationId}
                    onChange={e => setFormData({...formData, locationId: e.target.value})}
                  >
                    <option value="">-- Chọn địa điểm --</option>
                    {locations.map(loc => (
                      <option key={loc.id} value={loc.id}>{loc.name} ({loc.address})</option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Khối lượng (kg)</label>
                    <input 
                      type="number" 
                      step="0.01"
                      required 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.totalWeightKg} 
                      onChange={e => setFormData({...formData, totalWeightKg: e.target.value})}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Thể tích (m³)</label>
                    <input 
                      type="number" 
                      step="0.01"
                      required 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.totalVolumeM3} 
                      onChange={e => setFormData({...formData, totalVolumeM3: e.target.value})}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Khung giờ từ</label>
                    <input 
                      type="time" 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.timeWindowFrom} 
                      onChange={e => setFormData({...formData, timeWindowFrom: e.target.value})}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Khung giờ đến</label>
                    <input 
                      type="time" 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.timeWindowTo} 
                      onChange={e => setFormData({...formData, timeWindowTo: e.target.value})}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Thời gian phục vụ (phút)</label>
                    <input 
                      type="number" 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.serviceTimeMinutes} 
                      onChange={e => setFormData({...formData, serviceTimeMinutes: e.target.value})}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-[13px] font-semibold text-on-surface">Ghi chú</label>
                    <input 
                      type="text" 
                      className="w-full px-3 py-2 bg-surface border border-outline-variant/50 rounded-lg text-[14px] focus:border-secondary focus:ring-1 focus:ring-secondary/20 outline-none"
                      value={formData.note} 
                      onChange={e => setFormData({...formData, note: e.target.value})}
                      placeholder="Yêu cầu đặc biệt..."
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-3 mt-4 pt-4 border-t border-outline-variant/30">
                  <button 
                    type="button" 
                    className="px-4 py-2 border border-outline-variant/50 rounded-lg text-on-surface-variant font-label-md text-[13px] hover:bg-surface-variant transition-colors" 
                    onClick={() => setIsModalOpen(false)}
                  >
                    Hủy
                  </button>
                  <button 
                    type="submit" 
                    className="flex items-center gap-2 px-4 py-2 bg-secondary text-on-secondary rounded-lg font-label-md text-[13px] hover:opacity-90 transition-opacity"
                  >
                    <span className="material-symbols-outlined text-[18px]">save</span>
                    Lưu đơn hàng
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Orders;
