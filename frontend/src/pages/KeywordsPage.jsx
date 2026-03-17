import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { get, post } from '../api/client';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

export default function KeywordsPage() {
  const { siteId } = useParams();
  const [keywords, setKeywords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newKeyword, setNewKeyword] = useState('');
  const [adding, setAdding] = useState(false);
  const [collecting, setCollecting] = useState(false);
  const [selectedKeyword, setSelectedKeyword] = useState(null);
  const [rankings, setRankings] = useState([]);

  useEffect(() => {
    loadKeywords();
  }, [siteId]);

  const loadKeywords = async () => {
    setLoading(true);
    try {
      const res = await get(`/sites/${siteId}/keywords`);
      setKeywords(res.data?.keywords || res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const addKeyword = async (e) => {
    e.preventDefault();
    setAdding(true);
    try {
      await post(`/sites/${siteId}/keywords`, { keyword: newKeyword });
      setNewKeyword('');
      await loadKeywords();
    } catch (err) {
      setError(err.message);
    } finally {
      setAdding(false);
    }
  };

  const collectRankings = async () => {
    setCollecting(true);
    try {
      await post(`/sites/${siteId}/keywords/collect-rankings`);
      await loadKeywords();
    } catch (err) {
      setError(err.message);
    } finally {
      setCollecting(false);
    }
  };

  const viewRankings = async (kw) => {
    setSelectedKeyword(kw);
    try {
      const res = await get(`/sites/${siteId}/keywords/${kw.id}/rankings`);
      setRankings(res.data?.rankings || res.data || []);
    } catch {
      setRankings([]);
    }
  };

  if (loading) return <LoadingSpinner text="Loading keywords..." />;

  return (
    <div className="page">
      <h1 className="page-title">Keyword Rankings</h1>

      {error && <div className="alert alert-error" role="alert">{error}</div>}

      <div className="keyword-actions" style={{ marginBottom: 20 }}>
        <form onSubmit={addKeyword} className="inline-form" style={{ flex: 1 }}>
          <input
            type="text"
            value={newKeyword}
            onChange={(e) => setNewKeyword(e.target.value)}
            placeholder="Add keyword..."
            required
          />
          <button type="submit" className="btn btn-primary" disabled={adding}>
            {adding ? 'Adding...' : 'Add'}
          </button>
        </form>
        <button className="btn btn-secondary" onClick={collectRankings} disabled={collecting}>
          {collecting ? 'Collecting...' : 'Collect Rankings'}
        </button>
      </div>

      {keywords.length === 0 ? (
        <div className="empty-state"><p>No keywords tracked yet.</p></div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Keyword</th>
              <th>Current Rank</th>
              <th>Change</th>
              <th>Best Rank</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {keywords.map((kw) => (
              <tr key={kw.id} className={selectedKeyword?.id === kw.id ? 'row-selected' : ''}>
                <td>{kw.keyword}</td>
                <td>{kw.current_rank ?? '-'}</td>
                <td><RankChange change={kw.rank_change} /></td>
                <td>{kw.best_rank ?? '-'}</td>
                <td>
                  <button className="btn btn-sm btn-ghost" onClick={() => viewRankings(kw)}>
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
              <LineChart data={rankings.map(r => ({
                date: new Date(r.checked_at || r.date).toLocaleDateString('ko-KR'),
                rank: r.rank || r.position,
              }))}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} />
                <YAxis reversed stroke="#94a3b8" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }}
                  labelStyle={{ color: '#94a3b8' }}
                />
                <Line type="monotone" dataKey="rank" stroke="#3b82f6" strokeWidth={2} dot={{ r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}

function RankChange({ change }) {
  if (change == null || change === 0) return <span className="rank-neutral">-</span>;
  if (change > 0) return <span className="rank-up">{'\u25B2'} {change}</span>;
  return <span className="rank-down">{'\u25BC'} {Math.abs(change)}</span>;
}
