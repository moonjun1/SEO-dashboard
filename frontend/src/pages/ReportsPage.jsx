import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { get, post } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner';

export default function ReportsPage() {
  const { siteId } = useParams();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ type: 'seo', period: 'monthly', title: '' });
  const [creating, setCreating] = useState(false);
  const [selectedReport, setSelectedReport] = useState(null);

  useEffect(() => {
    loadReports();
  }, [siteId]);

  const loadReports = async () => {
    setLoading(true);
    try {
      const res = await get(`/sites/${siteId}/reports`);
      setReports(res.data?.reports || res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const createReport = async (e) => {
    e.preventDefault();
    setCreating(true);
    try {
      await post(`/sites/${siteId}/reports`, form);
      setShowForm(false);
      setForm({ type: 'seo', period: 'monthly', title: '' });
      await loadReports();
    } catch (err) {
      setError(err.message);
    } finally {
      setCreating(false);
    }
  };

  const viewReport = async (report) => {
    if (selectedReport?.id === report.id) {
      setSelectedReport(null);
      return;
    }
    try {
      const res = await get(`/sites/${siteId}/reports/${report.id}`);
      setSelectedReport(res.data);
    } catch {
      setSelectedReport(report);
    }
  };

  if (loading) return <LoadingSpinner text="Loading reports..." />;

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Reports</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          + New Report
        </button>
      </div>

      {error && <div className="alert alert-error" role="alert">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h3>Create Report</h3>
          <form onSubmit={createReport}>
            <div className="form-group">
              <label htmlFor="report-title">Title</label>
              <input
                id="report-title"
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Report title"
                required
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="report-type">Type</label>
                <select
                  id="report-type"
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}
                >
                  <option value="seo">SEO Report</option>
                  <option value="keyword">Keyword Report</option>
                  <option value="crawl">Crawl Report</option>
                  <option value="performance">Performance Report</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="report-period">Period</label>
                <select
                  id="report-period"
                  value={form.period}
                  onChange={(e) => setForm({ ...form, period: e.target.value })}
                >
                  <option value="weekly">Weekly</option>
                  <option value="monthly">Monthly</option>
                  <option value="quarterly">Quarterly</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={creating}>
                {creating ? 'Creating...' : 'Create Report'}
              </button>
              <button type="button" className="btn btn-ghost" onClick={() => setShowForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {reports.length === 0 ? (
        <div className="empty-state"><p>No reports yet. Create your first report.</p></div>
      ) : (
        <div className="reports-list">
          {reports.map((report) => (
            <div key={report.id} className="card report-card" onClick={() => viewReport(report)}>
              <div className="report-header">
                <div>
                  <h3 className="report-title">{report.title}</h3>
                  <div className="report-meta">
                    <span className="report-type">{report.type}</span>
                    <span className="report-period">{report.period}</span>
                    <span className="report-date">
                      {report.created_at ? new Date(report.created_at).toLocaleDateString('ko-KR') : ''}
                    </span>
                  </div>
                </div>
                <span className={`status-badge status-${report.status || 'completed'}`}>
                  {report.status || 'completed'}
                </span>
              </div>

              {selectedReport?.id === report.id && selectedReport.summary && (
                <div className="report-detail" onClick={(e) => e.stopPropagation()}>
                  <h4>Summary</h4>
                  <ReportSummary summary={selectedReport.summary} />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ReportSummary({ summary }) {
  if (typeof summary === 'string') {
    try {
      summary = JSON.parse(summary);
    } catch {
      return <p>{summary}</p>;
    }
  }

  if (!summary || typeof summary !== 'object') {
    return <p>No summary data available.</p>;
  }

  return (
    <div className="summary-grid">
      {Object.entries(summary).map(([key, value]) => (
        <div key={key} className="summary-item">
          <span className="summary-key">{key.replace(/_/g, ' ')}</span>
          <span className="summary-value">
            {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
          </span>
        </div>
      ))}
    </div>
  );
}
