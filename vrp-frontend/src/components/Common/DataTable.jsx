import React from 'react';
import { Edit2, Trash2, ChevronLeft, ChevronRight } from 'lucide-react';
import './DataTable.css';

const DataTable = ({ columns, data, onEdit, onDelete, loading }) => {
  return (
    <div className="table-container card">
      <div className="table-wrapper">
        <table className="custom-table">
          <thead>
            <tr>
              {columns.map((col, index) => (
                <th key={index} style={{ width: col.width }}>{col.header}</th>
              ))}
              {(onEdit || onDelete) && <th style={{ width: '100px' }}>Thao tác</th>}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length + 1} className="text-center py-10">
                  <div className="loading-spinner-small"></div>
                  Đang tải dữ liệu...
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length + 1} className="text-center py-10">
                  Không có dữ liệu hiển thị.
                </td>
              </tr>
            ) : (
              data.map((row, rowIndex) => (
                <tr key={rowIndex}>
                  {columns.map((col, colIndex) => (
                    <td key={colIndex}>
                      {col.render ? col.render(row[col.accessor], row) : row[col.accessor]}
                    </td>
                  ))}
                  {(onEdit || onDelete) && (
                    <td>
                      <div className="table-actions">
                        {onEdit && (
                          <button className="action-btn edit" onClick={() => onEdit(row)}>
                            <Edit2 size={16} />
                          </button>
                        )}
                        {onDelete && (
                          <button className="action-btn delete" onClick={() => onDelete(row)}>
                            <Trash2 size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      
      <div className="table-pagination">
        <div className="pagination-info">
          Hiển thị <span>{data.length}</span> bản ghi
        </div>
        <div className="pagination-controls">
          <button className="page-btn" disabled><ChevronLeft size={18} /></button>
          <button className="page-btn active">1</button>
          <button className="page-btn"><ChevronRight size={18} /></button>
        </div>
      </div>
    </div>
  );
};

export default DataTable;
