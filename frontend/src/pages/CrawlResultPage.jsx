import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { get } from '../api/client';
import { getScoreColor } from '../components/ScoreCard';
import LoadingSpinner from '../components/LoadingSpinner';

export default function CrawlResultPage() {
  const { siteId, jobId } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedRow, setExpandedRow] = useState(null);

  useEffect(() => { loadResults(); }, [siteId, jobId]);

  const loadResults = async () => {
    setLoading(true);
    try {
      const [jobRes, resultsRes] = await Promise.all([
        get(`/sites/${siteId}/crawl/jobs/${jobId}`),
        get(`/sites/${siteId}/crawl/jobs/${jobId}/results?page=0&size=50`),
      ]);
      setJob(jobRes.data);
      setResults(resultsRes.data?.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading crawl results..." />;

  return (
    <div className="page">
      <button className="btn btn-ghost" onClick={() => navigate(`/sites/${siteId}`)} style={{ marginBottom: 16 }}>
        &larr; Back to Site
      </button>

      {error && <div className="alert alert-error">{error}</div>}

      {job && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h2>Crawl Job #{jobId}</h2>
          <div className="crawl-info-grid">
            <div>
              <span className="text-muted">Status</span>
              <span className={`status-badge status-${(job.status || '').toLowerCase()}`}>{job.status}</span>
            </div>
            <div>
              <span className="text-muted">Pages Crawled</span>
              <span>{job.totalPages ?? results.length}</span>
            </div>
            <div>
              <span className="text-muted">Errors</span>
              <span>{job.errorCount ?? 0}</span>
            </div>
            <div>
              <span className="text-muted">Started</span>
              <span>{job.startedAt ? new Date(job.startedAt).toLocaleString('ko-KR') : '-'}</span>
            </div>
          </div>
        </div>
      )}

      <h2 className="section-title">Page Results ({results.length} pages)</h2>
      {results.length === 0 ? (
        <div className="empty-state"><p>No results available.</p></div>
      ) : (
        <table className="table">
          <thead>
            <tr><th>URL</th><th>Status</th><th>SEO Score</th><th>Response</th></tr>
          </thead>
          <tbody>
            {results.map((page, i) => {
              const analysis = page.analysis || {};
              return (
                <tr key={i} className="clickable-row"
                  onClick={() => setExpandedRow(expandedRow === i ? null : i)}>
                  <td className="url-cell">{page.url}</td>
                  <td>
                    <span className={`status-code status-code-${Math.floor((page.statusCode || 200) / 100)}xx`}>
                      {page.statusCode || 200}
                    </span>
                  </td>
                  <td>
                    <span style={{ color: getScoreColor(analysis.seoScore), fontWeight: 600 }}>
                      {analysis.seoScore ?? '-'}
                    </span>
                  </td>
                  <td>{page.responseTimeMs ? `${page.responseTimeMs}ms` : '-'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
