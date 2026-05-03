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
  Settings,
  LogOut
} from 'lucide-react';
import './Sidebar.css';

const Sidebar = () => {
  const menuItems = [
    { icon: <LayoutDashboard size={20} />, label: 'Điều hành lộ trình', path: '/admin/dashboard' },
    { icon: <MapPin size={20} />, label: 'Địa điểm', path: '/admin/locations' },
    { icon: <Warehouse size={20} />, label: 'Kho bãi', path: '/admin/depots' },
    { icon: <Truck size={20} />, label: 'Phương tiện', path: '/admin/vehicles' },
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
        <div className="logo-icon">V</div>
        <span>VRP Optimizer</span>
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
