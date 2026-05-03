import { Outlet, useNavigate } from 'react-router-dom';
import { Truck, LogOut } from 'lucide-react';
import './DriverLayout.css';

const DriverLayout = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="driver-layout">
      <header className="driver-header">
        <div className="driver-header-logo">
          <Truck size={20} />
          <span>VRP Driver</span>
        </div>
        <button className="driver-logout-btn" onClick={handleLogout} title="Đăng xuất">
          <LogOut size={18} />
        </button>
      </header>
      <main className="driver-main">
        <Outlet />
      </main>
    </div>
  );
};

export default DriverLayout;
