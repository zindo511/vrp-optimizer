import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Login from './components/Login/Login';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import DashboardLayout from './components/Layout/DashboardLayout';
import DriverLayout from './components/Layout/DriverLayout';
import Locations from './pages/Locations/Locations';
import Depots from './pages/Depots/Depots';
import Vehicles from './pages/Vehicles/Vehicles';
import Drivers from './pages/Drivers/Drivers';
import Orders from './pages/Orders/Orders';
import Optimization from './pages/Optimization/Optimization';
import Reports from './pages/Reports/Reports';
import DriverRoutes from './pages/Driver/DriverRoutes';
import StopDetail from './pages/Driver/StopDetail';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />

        {/* Admin / Dispatcher Routes */}
        <Route path="/admin" element={<ProtectedRoute allowedRoles={['ADMIN', 'DISPATCHER']}><DashboardLayout /></ProtectedRoute>}>
          <Route path="dashboard" element={<Optimization />} />
          <Route path="locations" element={<Locations />} />
          <Route path="depots" element={<Depots />} />
          <Route path="vehicles" element={<Vehicles />} />
          <Route path="drivers" element={<Drivers />} />
          <Route path="orders" element={<Orders />} />
          <Route path="reports" element={<Reports />} />
          <Route index element={<Navigate to="dashboard" replace />} />
        </Route>

        {/* Driver Routes */}
        <Route path="/driver" element={<ProtectedRoute allowedRoles={['DRIVER']}><DriverLayout /></ProtectedRoute>}>
          <Route path="routes" element={<DriverRoutes />} />
          <Route path="stops/:stopId" element={<StopDetail />} />
          <Route index element={<Navigate to="routes" replace />} />
        </Route>

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
      <ToastContainer 
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
      />
    </Router>
  );
}

export default App;
