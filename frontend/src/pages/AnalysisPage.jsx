import { useState } from 'react';
import { post, get } from '../api/client';
import { getScoreColor } from '../components/ScoreCard';
import LoadingSpinner from '../components/LoadingSpinner';

export default function AnalysisPage() {
  const [activeTab, setActiveTab] = useState('content');
  const [contentForm, setContentForm] = useState({ content: '', keyword: '' });
  const [metaForm, setMetaForm] = useState({ title: '', description: '', keyword: '' });
  const [analysisResult, setAnalysisResult] = useState(null);
  const [metaResult, setMetaResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const analyzeContent = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setAnalysisResult(null);
    try {
      const res = await post('/analysis/content', contentForm);
      if (res.data?.id) {
        const detail = await get(`/analysis/content/${res.data.id}`);
        setAnalysisResult(detail.data);
      } else {
        setAnalysisResult(res.data);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const generateMeta = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMetaResult(null);
    try {
      const res = await post('/analysis/meta-generate', metaForm);
      setMetaResult(res.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1 className="page-title">AI Content Analysis</h1>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="tabs" style={{ marginBottom: 24 }}>
        <button
          className={`tab ${activeTab === 'content' ? 'active' : ''}`}
          onClick={() => setActiveTab('content')}
        >
          Content Analysis
        </button>
        <button
          className={`tab ${activeTab === 'meta' ? 'active' : ''}`}
          onClick={() => setActiveTab('meta')}
        >
          Meta Tag Generator
        </button>
      </div>

      {activeTab === 'content' && (
        <div>
          <div className="card" style={{ marginBottom: 24 }}>
            <h3>Analyze Content</h3>
            <form onSubmit={analyzeContent}>
              <div className="form-group">
                <label htmlFor="kw">Target Keyword</label>
                <input
                  id="kw"
                  type="text"
                  value={contentForm.keyword}
                  onChange={(e) => setContentForm({ ...contentForm, keyword: e.target.value })}
                  placeholder="e.g., SEO optimization tips"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="content">Content</label>
                <textarea
                  id="content"
                  value={contentForm.content}
                  onChange={(e) => setContentForm({ ...contentForm, content: e.target.value })}
                  placeholder="Paste the content you want to analyze for SEO..."
                  rows={8}
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Analyzing...' : 'Analyze'}
              </button>
            </form>
          </div>

          {loading && <LoadingSpinner text="Analyzing content..." />}

          {analysisResult && (
            <div className="card">
              <h3>Analysis Result</h3>
              <div className="analysis-results-grid">
                <ResultMetric
                  label="SEO Score"
                  value={analysisResult.seo_score ?? analysisResult.overall_score}
                />
                <ResultMetric label="Readability" value={analysisResult.readability_score} />
                <ResultMetric
                  label="Keyword Density"
                  value={analysisResult.keyword_density != null
                    ? `${(analysisResult.keyword_density * 100).toFixed(1)}%`
                    : null}
                  raw
                />
                <ResultMetric label="Word Count" value={analysisResult.word_count} raw />
              </div>

              {analysisResult.suggestions && analysisResult.suggestions.length > 0 && (
                <div style={{ marginTop: 20 }}>
                  <h4>Improvement Suggestions</h4>
                  <ul className="suggestion-list">
                    {analysisResult.suggestions.map((s, i) => (
                      <li key={i}>{typeof s === 'string' ? s : s.message || s.text}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {activeTab === 'meta' && (
        <div>
          <div className="card" style={{ marginBottom: 24 }}>
            <h3>Generate Meta Tags</h3>
            <form onSubmit={generateMeta}>
              <div className="form-group">
                <label htmlFor="meta-title">Page Title / Topic</label>
                <input
                  id="meta-title"
                  type="text"
                  value={metaForm.title}
                  onChange={(e) => setMetaForm({ ...metaForm, title: e.target.value })}
                  placeholder="e.g., Complete Guide to SEO"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="meta-desc">Description / Summary</label>
                <textarea
                  id="meta-desc"
                  value={metaForm.description}
                  onChange={(e) => setMetaForm({ ...metaForm, description: e.target.value })}
                  placeholder="Brief description of your page..."
                  rows={3}
                />
              </div>
              <div className="form-group">
                <label htmlFor="meta-kw">Target Keyword</label>
                <input
                  id="meta-kw"
                  type="text"
                  value={metaForm.keyword}
                  onChange={(e) => setMetaForm({ ...metaForm, keyword: e.target.value })}
                  placeholder="Primary keyword"
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Generating...' : 'Generate'}
              </button>
            </form>
          </div>

          {loading && <LoadingSpinner text="Generating meta tags..." />}

          {metaResult && (
            <div className="card">
              <h3>Generated Meta Tags</h3>
              <div className="meta-result">
                {metaResult.title && (
                  <div className="meta-field">
                    <label>Title Tag</label>
                    <code className="meta-code">{metaResult.title}</code>
                    <span className="meta-length">{metaResult.title.length} characters</span>
                  </div>
                )}
                {metaResult.description && (
                  <div className="meta-field">
                    <label>Meta Description</label>
                    <code className="meta-code">{metaResult.description}</code>
                    <span className="meta-length">{metaResult.description.length} characters</span>
                  </div>
                )}
                {metaResult.keywords && (
                  <div className="meta-field">
                    <label>Keywords</label>
                    <code className="meta-code">{metaResult.keywords}</code>
                  </div>
                )}
                {metaResult.og_title && (
                  <div className="meta-field">
                    <label>OG Title</label>
                    <code className="meta-code">{metaResult.og_title}</code>
                  </div>
                )}
                {metaResult.og_description && (
                  <div className="meta-field">
                    <label>OG Description</label>
                    <code className="meta-code">{metaResult.og_description}</code>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ResultMetric({ label, value, raw }) {
  if (value == null) return null;
  const numVal = raw ? null : typeof value === 'number' ? value : parseFloat(value);
  return (
    <div className="result-metric">
      <span className="result-metric-label">{label}</span>
      <span
        className="result-metric-value"
        style={!raw && numVal != null ? { color: getScoreColor(numVal) } : {}}
      >
        {value}
      </span>
    </div>
  );
}
