import { Navigate } from 'react-router-dom';

/**
 * allowedRoles: nếu truyền vào thì kiểm tra thêm role.
 * Không truyền → chỉ check token tồn tại (backward-compatible).
 */
const ProtectedRoute = ({ children, allowedRoles }) => {
  const token = localStorage.getItem('accessToken');
  const role = localStorage.getItem('role');

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    // Đúng role sai route → redirect về đúng home của role đó
    return <Navigate to={role === 'DRIVER' ? '/driver/routes' : '/admin/dashboard'} replace />;
  }

  return children;
};

export default ProtectedRoute;
