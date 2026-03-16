import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { get, post } from '../api/client';
import { ScoreGauge, getScoreColor } from '../components/ScoreCard';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

export default function SiteDetailPage() {
  const { siteId } = useParams();
  const navigate = useNavigate();
  const [site, setSite] = useState(null);
  const [siteInfo, setSiteInfo] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [crawlJobs, setCrawlJobs] = useState([]);
  const [crawling, setCrawling] = useState(false);

  const [keywords, setKeywords] = useState([]);
  const [newKeyword, setNewKeyword] = useState('');
  const [addingKeyword, setAddingKeyword] = useState(false);
  const [selectedKeyword, setSelectedKeyword] = useState(null);
  const [rankings, setRankings] = useState([]);
  const [collectingRanks, setCollectingRanks] = useState(false);

  const [analyses, setAnalyses] = useState([]);
  const [analysisForm, setAnalysisForm] = useState({ content: '', targetKeywords: '' });
  const [analyzing, setAnalyzing] = useState(false);

  useEffect(() => { loadSite(); }, [siteId]);

  useEffect(() => {
    if (activeTab === 'crawl') loadCrawlJobs();
    if (activeTab === 'keywords') loadKeywords();
    if (activeTab === 'analysis') loadAnalyses();
  }, [activeTab, siteId]);

  const loadSite = async () => {
    setLoading(true);
    try {
      const [dashRes, siteRes] = await Promise.all([
        get(`/dashboard/sites/${siteId}`).catch(() => ({ data: {} })),
        get(`/sites/${siteId}`),
      ]);
      setSite(dashRes.data);
      setSiteInfo(siteRes.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadCrawlJobs = async () => {
    try {
      const res = await get(`/sites/${siteId}/crawl/jobs`);
      const jobs = res.data?.content || res.data?.jobs || (Array.isArray(res.data) ? res.data : []);
      setCrawlJobs(jobs);
    } catch (err) { setError(err.message); }
  };

  const startCrawl = async () => {
    setCrawling(true);
    setError('');
    try {
      await post(`/sites/${siteId}/crawl`, { maxPages: 10, maxDepth: 2 });
      setTimeout(() => loadCrawlJobs(), 2000);
    } catch (err) { setError(err.message); }
    finally { setCrawling(false); }
  };

  const loadKeywords = async () => {
    try {
      const res = await get(`/sites/${siteId}/keywords`);
      const kws = res.data?.content || res.data?.keywords || (Array.isArray(res.data) ? res.data : []);
      setKeywords(kws);
    } catch (err) { setError(err.message); }
  };

  const addKeyword = async (e) => {
    e.preventDefault();
    setAddingKeyword(true);
    try {
      await post(`/sites/${siteId}/keywords`, { keyword: newKeyword });
      setNewKeyword('');
      await loadKeywords();
    } catch (err) { setError(err.message); }
    finally { setAddingKeyword(false); }
  };

  const collectRankings = async () => {
    setCollectingRanks(true);
    try {
      await post(`/sites/${siteId}/keywords/collect`);
      await loadKeywords();
    } catch (err) { setError(err.message); }
    finally { setCollectingRanks(false); }
  };

  const loadRankings = async (kw) => {
    setSelectedKeyword(kw);
    try {
      const res = await get(`/sites/${siteId}/keywords/${kw.id}/rankings?period=30d`);
      setRankings(res.data?.rankings || []);
    } catch { setRankings([]); }
  };

  const loadAnalyses = async () => {
    try {
      const res = await get(`/analysis/content`);
      const list = res.data?.content || res.data?.analyses || (Array.isArray(res.data) ? res.data : []);
      setAnalyses(list);
    } catch { setAnalyses([]); }
  };

  const submitAnalysis = async (e) => {
    e.preventDefault();
    setAnalyzing(true);
    try {
      await post('/analysis/content', {
        siteId: parseInt(siteId),
        content: analysisForm.content,
        targetKeywords: analysisForm.targetKeywords,
      });
      setAnalysisForm({ content: '', targetKeywords: '' });
      setTimeout(() => loadAnalyses(), 4000);
    } catch (err) { setError(err.message); }
    finally { setAnalyzing(false); }
  };

  if (loading) return <LoadingSpinner text="Loading site..." />;

  const info = siteInfo || {};
  const overview = site?.overview || site || {};

  return (
    <div className="page">
      <button className="btn btn-ghost" onClick={() => navigate('/')} style={{ marginBottom: 16 }}>
        &larr; Back to Dashboard
      </button>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="site-header">
        <div>
          <h1 className="page-title">{info.name || 'Site'}</h1>
          <p className="text-muted">{info.url}</p>
        </div>
        <ScoreGauge score={overview.seoScore ?? info.seoScore} size={100} />
      </div>

      <div className="tabs">
        {['overview', 'crawl', 'keywords', 'analysis'].map((t) => (
          <button key={t} className={`tab ${activeTab === t ? 'active' : ''}`}
            onClick={() => setActiveTab(t)}>
            {t.charAt(0).toUpperCase() + t.slice(1)}
          </button>
        ))}
      </div>

      <div className="tab-content">
        {activeTab === 'overview' && <OverviewTab site={site} />}
        {activeTab === 'crawl' && (
          <CrawlTab jobs={crawlJobs} crawling={crawling} onStartCrawl={startCrawl}
            siteId={siteId} navigate={navigate} onRefresh={loadCrawlJobs} />
        )}
        {activeTab === 'keywords' && (
          <KeywordsTab keywords={keywords} newKeyword={newKeyword} setNewKeyword={setNewKeyword}
            addingKeyword={addingKeyword} onAddKeyword={addKeyword}
            onCollectRankings={collectRankings} collectingRanks={collectingRanks}
            selectedKeyword={selectedKeyword} rankings={rankings} onSelectKeyword={loadRankings} />
        )}
        {activeTab === 'analysis' && (
          <AnalysisTab analyses={analyses} form={analysisForm} setForm={setAnalysisForm}
            analyzing={analyzing} onSubmit={submitAnalysis} />
        )}
      </div>
    </div>
  );
}

function OverviewTab({ site }) {
  const overview = site?.overview || {};
  const issues = site?.issuesSummary || {};
  const topKws = site?.topKeywords || [];
  const priorities = site?.improvementPriority || [];

  return (
    <div>
      {overview.seoScore != null && (
        <div className="stats-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card"><div className="stat-label">SEO Score</div><div className="stat-value">{overview.seoScore}</div></div>
          <div className="stat-card"><div className="stat-label">Total Pages</div><div className="stat-value">{overview.totalPages ?? 0}</div></div>
          <div className="stat-card"><div className="stat-label">Healthy Pages</div><div className="stat-value">{overview.healthyPages ?? 0}</div></div>
          <div className="stat-card"><div className="stat-label">Avg Response</div><div className="stat-value">{overview.avgResponseTimeMs ? `${Math.round(overview.avgResponseTimeMs)}ms` : '-'}</div></div>
        </div>
      )}

      {topKws.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3>Top Keywords</h3>
          <table className="table">
            <thead><tr><th>Keyword</th><th>Rank</th><th>Change</th></tr></thead>
            <tbody>
              {topKws.map((kw, i) => (
                <tr key={i}>
                  <td>{kw.keyword}</td>
                  <td>{kw.currentRank ?? '-'}</td>
                  <td><RankChange change={kw.rankChange} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {priorities.length > 0 && (
        <div className="card">
          <h3>Improvement Priorities</h3>
          <ul className="priority-list">
            {priorities.map((p, i) => (
              <li key={i} className="issue-item">
                <span className={`issue-severity severity-${(p.severity || 'info').toLowerCase()}`}>{p.severity}</span>
                <span>{p.description || p.category}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!overview.seoScore && topKws.length === 0 && (
        <div className="empty-state"><p>Start a crawl to see site overview data.</p></div>
      )}
    </div>
  );
}

function CrawlTab({ jobs, crawling, onStartCrawl, siteId, navigate, onRefresh }) {
  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
        <button className="btn btn-primary" onClick={onStartCrawl} disabled={crawling}>
          {crawling ? 'Starting...' : 'Start Crawl'}
        </button>
        <button className="btn btn-ghost" onClick={onRefresh}>Refresh</button>
      </div>

      {jobs.length === 0 ? (
        <div className="empty-state"><p>No crawl jobs yet. Click "Start Crawl" to begin.</p></div>
      ) : (
        <table className="table">
          <thead><tr><th>ID</th><th>Status</th><th>Pages</th><th>Errors</th><th>Started</th><th></th></tr></thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>#{job.id}</td>
                <td><span className={`status-badge status-${(job.status || '').toLowerCase()}`}>{job.status}</span></td>
                <td>{job.totalPages ?? '-'}</td>
                <td>{job.errorCount ?? 0}</td>
                <td>{job.startedAt ? new Date(job.startedAt).toLocaleString('ko-KR') : '-'}</td>
                <td>
                  {(job.status === 'COMPLETED') && (
                    <button className="btn btn-sm btn-ghost"
                      onClick={() => navigate(`/sites/${siteId}/crawl/${job.id}`)}>
                      View Results
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function KeywordsTab({ keywords, newKeyword, setNewKeyword, addingKeyword, onAddKeyword,
  onCollectRankings, collectingRanks, selectedKeyword, rankings, onSelectKeyword }) {
  return (
    <div>
      <div className="keyword-actions">
        <form onSubmit={onAddKeyword} className="inline-form" style={{ flex: 1 }}>
          <input type="text" value={newKeyword} onChange={(e) => setNewKeyword(e.target.value)}
            placeholder="Add keyword..." required />
          <button type="submit" className="btn btn-primary" disabled={addingKeyword}>
            {addingKeyword ? 'Adding...' : 'Add'}
          </button>
        </form>
        <button className="btn btn-secondary" onClick={onCollectRankings} disabled={collectingRanks}>
          {collectingRanks ? 'Collecting...' : 'Collect Rankings'}
        </button>
      </div>

      {keywords.length === 0 ? (
        <div className="empty-state"><p>No keywords tracked yet.</p></div>
      ) : (
        <table className="table">
          <thead><tr><th>Keyword</th><th>Current Rank</th><th>Change</th><th></th></tr></thead>
          <tbody>
            {keywords.map((kw) => (
              <tr key={kw.id} className={selectedKeyword?.id === kw.id ? 'row-selected' : ''}>
                <td>{kw.keyword}</td>
                <td>{kw.currentRank ?? kw.current_rank ?? '-'}</td>
                <td><RankChange change={kw.rankChange ?? kw.rank_change} /></td>
                <td>
                  <button className="btn btn-sm btn-ghost" onClick={() => onSelectKeyword(kw)}>
                    History
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedKeyword && rankings.length > 0 && (
        <div className="card" style={{ marginTop: 20 }}>
          <h3>Ranking History: {selectedKeyword.keyword}</h3>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <LineChart data={[...rankings].reverse().map(r => ({
                date: new Date(r.recordedAt).toLocaleDateString('ko-KR'),
                rank: r.rank,
              }))}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} />
                <YAxis reversed stroke="#94a3b8" fontSize={12} />
                <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }}
                  labelStyle={{ color: '#94a3b8' }} />
                <Line type="monotone" dataKey="rank" stroke="#3b82f6" strokeWidth={2} dot={{ r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}

function AnalysisTab({ analyses, form, setForm, analyzing, onSubmit }) {
  return (
    <div>
      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Request AI Analysis</h3>
        <form onSubmit={onSubmit}>
          <div className="form-group">
            <label>Target Keywords (comma separated)</label>
            <input type="text" value={form.targetKeywords}
              onChange={(e) => setForm({ ...form, targetKeywords: e.target.value })}
              placeholder="e.g., SEO, 검색엔진 최적화" required />
          </div>
          <div className="form-group">
            <label>Content</label>
            <textarea value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              placeholder="Paste content to analyze (min 100 characters)..." rows={6} required />
          </div>
          <button type="submit" className="btn btn-primary" disabled={analyzing}>
            {analyzing ? 'Analyzing...' : 'Analyze Content'}
          </button>
        </form>
      </div>

      {analyses.length > 0 && (
        <div>
          <h3 className="section-title">Analysis Results</h3>
          {analyses.map((a) => <AnalysisResultCard key={a.id} analysis={a} />)}
        </div>
      )}
    </div>
  );
}

function AnalysisResultCard({ analysis }) {
  const score = analysis.seoScore ?? analysis.seo_score;
  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span><strong>{analysis.title || analysis.targetKeywords || 'Analysis'}</strong></span>
        <span className={`status-badge status-${(analysis.status || '').toLowerCase()}`}>{analysis.status}</span>
      </div>
      {score != null && (
        <div style={{ display: 'flex', gap: 16, marginBottom: 8 }}>
          <span style={{ color: getScoreColor(score) }}>SEO: {score}</span>
          {analysis.readabilityScore != null && <span>Readability: {analysis.readabilityScore}</span>}
        </div>
      )}
      {analysis.suggestions && (
        <ul className="suggestion-list">
          {(typeof analysis.suggestions === 'string' ? JSON.parse(analysis.suggestions) : analysis.suggestions).map((s, i) => (
            <li key={i}>{typeof s === 'string' ? s : s.message || s.text}</li>
          ))}
        </ul>
      )}
      <div className="text-muted" style={{ fontSize: 12, marginTop: 8 }}>
        {analysis.createdAt ? new Date(analysis.createdAt).toLocaleString('ko-KR') : ''}
      </div>
    </div>
  );
}

function RankChange({ change }) {
  if (change == null || change === 0) return <span className="rank-neutral">-</span>;
  if (change > 0) return <span className="rank-up">{'\u25B2'} {change}</span>;
  return <span className="rank-down">{'\u25BC'} {Math.abs(change)}</span>;
}
