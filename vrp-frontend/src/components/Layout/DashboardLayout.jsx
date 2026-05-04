import React from 'react';
import { Outlet } from 'react-router-dom';
import { Bell, HelpCircle, UserCircle } from 'lucide-react';
import Sidebar from './Sidebar';
import './DashboardLayout.css';

const DashboardLayout = () => {
  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="dashboard-main">
        <header className="dashboard-header">
          <div style={{ display: 'flex', alignItems: 'center', height: '100%' }}>
            <span className="header-brand">VRP Logistics</span>
          </div>
          <div className="header-profile">
            <button className="header-icon-btn" title="Thông báo">
              <Bell size={20} />
            </button>
            <button className="header-icon-btn" title="Trợ giúp">
              <HelpCircle size={20} />
            </button>
            <button className="header-icon-btn" title="Tài khoản">
              <UserCircle size={20} />
            </button>
          </div>
        </header>
        <div className="dashboard-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default DashboardLayout;
