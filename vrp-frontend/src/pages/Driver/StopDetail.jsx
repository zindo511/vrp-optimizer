import { useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, MapPin, Phone, Clock, CheckCircle, XCircle, Navigation } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/axios';
import './Driver.css';

const StopDetail = () => {
  const { stopId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const stop = state?.stop;

  const [currentStatus, setCurrentStatus] = useState(stop?.status ?? 'WAITING');
  const [note, setNote] = useState('');
  const [failureReason, setFailureReason] = useState('');
  const [proofImageUrl, setProofImageUrl] = useState(stop?.proofImageUrl ?? '');
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (!stop) {
    return (
      <div className="driver-center driver-error">
        <p>Không tìm thấy thông tin điểm giao.</p>
        <button className="btn-back-text" onClick={() => navigate('/driver/routes')}>Quay lại</button>
      </div>
    );
  }

  const isDone = currentStatus === 'COMPLETED' || currentStatus === 'FAILED';

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/api/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setProofImageUrl(res.data.data.fileUrl);
      toast.success('Tải ảnh lên thành công');
    } catch {
      toast.error('Không thể tải ảnh lên');
    } finally {
      setUploading(false);
    }
  };

  const handleUpdateStatus = async (newStatus) => {
    if (newStatus === 'FAILED' && !failureReason.trim()) {
      toast.error('Vui lòng nhập lý do giao thất bại');
      return;
    }
    setSubmitting(true);
    try {
      await api.put(`/api/drivers/stops/${stopId}/status`, {
        stopStatus: newStatus,
        proofImageUrl: proofImageUrl || null,
        note: note || null,
        failureReason: newStatus === 'FAILED' ? failureReason : null,
      });
      setCurrentStatus(newStatus);
      toast.success('Cập nhật trạng thái thành công');
      setTimeout(() => navigate('/driver/routes'), 1200);
    } catch {
      // handled by axios interceptor
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="driver-page">
      <div className="stop-detail-nav">
        <button className="back-btn" onClick={() => navigate('/driver/routes')}>
          <ArrowLeft size={18} />
          <span>Lộ trình</span>
        </button>
        <h2>Điểm #{stop.stopOrder}</h2>
      </div>

      <div className="detail-card">
        <div className="detail-row">
          <span className="detail-label">Khách hàng</span>
          <span className="detail-value">{stop.customerName || '—'}</span>
        </div>
        {stop.customerPhone && (
          <div className="detail-row">
            <span className="detail-label"><Phone size={13} /> SĐT</span>
            <a href={`tel:${stop.customerPhone}`} className="detail-value link">{stop.customerPhone}</a>
          </div>
        )}
        <div className="detail-row">
          <span className="detail-label"><MapPin size={13} /> Địa chỉ</span>
          <span className="detail-value">{stop.address}</span>
        </div>
        {stop.estimatedArrival && (
          <div className="detail-row">
            <span className="detail-label"><Clock size={13} /> Dự kiến</span>
            <span className="detail-value">
              {new Date(stop.estimatedArrival).toLocaleString('vi-VN', {
                hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit',
              })}
            </span>
          </div>
        )}
        {stop.weightKg && (
          <div className="detail-row">
            <span className="detail-label">Trọng lượng</span>
            <span className="detail-value">{stop.weightKg} kg</span>
          </div>
        )}
      </div>

      {!isDone && (
        <div className="action-card">
          <h3>Cập nhật giao hàng</h3>

          <div className="form-group">
            <label>Ảnh xác nhận</label>
            <label className="upload-label">
              <input type="file" accept="image/*" onChange={handleImageUpload} hidden disabled={uploading} />
              <span className="upload-btn">
                {uploading ? 'Đang tải...' : proofImageUrl ? '✓ Đã có ảnh — đổi ảnh' : 'Chọn ảnh'}
              </span>
            </label>
            {proofImageUrl && (
              <img src={proofImageUrl} alt="proof" className="proof-preview" />
            )}
          </div>

          <div className="form-group">
            <label>Ghi chú</label>
            <textarea
              placeholder="Ghi chú thêm (không bắt buộc)"
              value={note}
              onChange={e => setNote(e.target.value)}
              rows={2}
            />
          </div>

          <div className="form-group">
            <label>Lý do thất bại</label>
            <input
              type="text"
              placeholder="Điền nếu không giao được..."
              value={failureReason}
              onChange={e => setFailureReason(e.target.value)}
            />
          </div>

          <div className="action-buttons">
            {currentStatus === 'WAITING' && (
              <button
                className="btn-arrived"
                onClick={() => handleUpdateStatus('ARRIVED')}
                disabled={submitting}
              >
                <Navigation size={16} /> Đã đến nơi
              </button>
            )}
            <button
              className="btn-complete"
              onClick={() => handleUpdateStatus('COMPLETED')}
              disabled={submitting}
            >
              <CheckCircle size={16} /> Giao thành công
            </button>
            <button
              className="btn-fail"
              onClick={() => handleUpdateStatus('FAILED')}
              disabled={submitting}
            >
              <XCircle size={16} /> Giao thất bại
            </button>
          </div>
        </div>
      )}

      {isDone && (
        <div className={`done-banner ${currentStatus === 'COMPLETED' ? 'done-success' : 'done-fail'}`}>
          {currentStatus === 'COMPLETED'
            ? <><CheckCircle size={28} /> <span>Giao hàng thành công!</span></>
            : <><XCircle size={28} /> <span>Đã đánh dấu thất bại</span></>
          }
        </div>
      )}
    </div>
  );
};

export default StopDetail;
