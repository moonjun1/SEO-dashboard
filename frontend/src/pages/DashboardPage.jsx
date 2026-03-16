import { useState, useEffect } from 'react';
import { get, post } from '../api/client';
import ScoreCard from '../components/ScoreCard';
import SiteCard from '../components/SiteCard';
import LoadingSpinner from '../components/LoadingSpinner';

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [sites, setSites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddSite, setShowAddSite] = useState(false);
  const [newSite, setNewSite] = useState({ name: '', url: '' });
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [dashRes, sitesRes] = await Promise.all([
        get('/dashboard'),
        get('/sites'),
      ]);
      setDashboard(dashRes.data);
      const sitesList = sitesRes.data?.content || sitesRes.data?.sites || (Array.isArray(sitesRes.data) ? sitesRes.data : []);
      setSites(sitesList);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSite = async (e) => {
    e.preventDefault();
    setAdding(true);
    try {
      await post('/sites', newSite);
      setNewSite({ name: '', url: '' });
      setShowAddSite(false);
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading dashboard..." />;

  const stats = dashboard || {};

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <button className="btn btn-primary" onClick={() => setShowAddSite(!showAddSite)}>
          + Add Site
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {showAddSite && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h3 style={{ marginBottom: 16 }}>Add New Site</h3>
          <form onSubmit={handleAddSite} className="inline-form">
            <div className="form-group">
              <label htmlFor="site-name">Site Name</label>
              <input
                id="site-name"
                type="text"
                value={newSite.name}
                onChange={(e) => setNewSite({ ...newSite, name: e.target.value })}
                placeholder="My Website"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="site-url">URL</label>
              <input
                id="site-url"
                type="url"
                value={newSite.url}
                onChange={(e) => setNewSite({ ...newSite, url: e.target.value })}
                placeholder="https://example.com"
                required
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={adding}>
                {adding ? 'Adding...' : 'Add Site'}
              </button>
              <button type="button" className="btn btn-ghost" onClick={() => setShowAddSite(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="stats-grid">
        <ScoreCard
          label="Total Sites"
          value={stats.totalSites ?? stats.total_sites ?? sites.length}
          icon={
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
            </svg>
          }
        />
        <ScoreCard
          label="Avg SEO Score"
          value={(stats.avgSeoScore ?? stats.avg_seo_score) != null ? Math.round(stats.avgSeoScore ?? stats.avg_seo_score) : '-'}
          icon={
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2">
              <path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/>
            </svg>
          }
        />
        <ScoreCard
          label="Total Keywords"
          value={stats.totalKeywords ?? stats.total_keywords ?? 0}
          icon={
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#eab308" strokeWidth="2">
              <path d="m21 21-6-6m2-5a7 7 0 1 1-14 0 7 7 0 0 1 14 0z"/>
            </svg>
          }
        />
        <ScoreCard
          label="Top 10 Keywords"
          value={stats.keywordsInTop10 ?? stats.top10_keywords ?? 0}
          icon={
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#a855f7" strokeWidth="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          }
        />
      </div>

      <section>
        <h2 className="section-title">My Sites</h2>
        {sites.length === 0 ? (
          <div className="empty-state">
            <p>No sites added yet. Click "Add Site" to get started.</p>
          </div>
        ) : (
          <div className="sites-grid">
            {sites.map((site) => (
              <SiteCard key={site.id} site={site} />
            ))}
          </div>
        )}
      </section>

      {(stats.recentActivity || stats.recent_activities || []).length > 0 && (
        <section>
          <h2 className="section-title">Recent Activity</h2>
          <div className="card">
            <ul className="activity-list">
              {(stats.recentActivity || stats.recent_activities || []).map((item, i) => (
                <li key={i} className="activity-item">
                  <span className="activity-text">{item.message || item.description}</span>
                  <span className="activity-time">
                    {(item.createdAt || item.created_at) ? new Date(item.createdAt || item.created_at).toLocaleString('ko-KR') : ''}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}
    </div>
  );
}
