import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  MapPin, 
  Warehouse, 
  Truck, 
  Users, 
  ShoppingCart, 
  BarChart3,
  LogOut
} from 'lucide-react';
import './Sidebar.css';

const Sidebar = () => {
  const menuItems = [
    { icon: <LayoutDashboard size={20} />, label: 'Bảng điều khiển', path: '/admin/dashboard' },
    { icon: <MapPin size={20} />, label: 'Quản lý Địa điểm', path: '/admin/locations' },
    { icon: <Warehouse size={20} />, label: 'Kho bãi', path: '/admin/depots' },
    { icon: <Truck size={20} />, label: 'Đội xe', path: '/admin/vehicles' },
    { icon: <Users size={20} />, label: 'Tài xế', path: '/admin/drivers' },
    { icon: <ShoppingCart size={20} />, label: 'Đơn hàng', path: '/admin/orders' },
    { icon: <BarChart3 size={20} />, label: 'Báo cáo', path: '/admin/reports' },
  ];

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = '/login';
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span>VRP System</span>
        <span className="sidebar-logo-sub">Quản trị Logistics</span>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => (
          <NavLink 
            key={item.path} 
            to={item.path} 
            className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}
          >
            {item.icon}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button onClick={handleLogout} className="logout-btn">
          <LogOut size={20} />
          <span>Đăng xuất</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
