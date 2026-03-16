import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import SiteDetailPage from './pages/SiteDetailPage';
import CrawlResultPage from './pages/CrawlResultPage';
import KeywordsPage from './pages/KeywordsPage';
import AnalysisPage from './pages/AnalysisPage';
import ReportsPage from './pages/ReportsPage';

export default function App() {
  const { user } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route element={<Layout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/sites/:siteId" element={<SiteDetailPage />} />
        <Route path="/sites/:siteId/crawl/:jobId" element={<CrawlResultPage />} />
        <Route path="/sites/:siteId/keywords" element={<KeywordsPage />} />
        <Route path="/analysis" element={<AnalysisPage />} />
        <Route path="/reports/:siteId" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
