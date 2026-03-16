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
import LandingPage from './pages/LandingPage';
import PublicResultPage from './pages/PublicResultPage';
import RankingPage from './pages/RankingPage';

export default function App() {
  const { user } = useAuth();

  return (
    <Routes>
      {/* Public pages (no auth) */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/result/:id" element={<PublicResultPage />} />
      <Route path="/ranking" element={<RankingPage />} />

      {/* Auth */}
      <Route
        path="/login"
        element={user ? <Navigate to="/dashboard" replace /> : <LoginPage />}
      />

      {/* Protected pages */}
      <Route element={<Layout />}>
        <Route path="/dashboard" element={<DashboardPage />} />
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
