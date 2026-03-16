import { Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Sidebar from './Sidebar';
import LoadingSpinner from './LoadingSpinner';

export default function Layout() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="page-center">
        <LoadingSpinner text="Loading..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
